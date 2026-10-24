param(
    [ValidateSet('1.20.1','1.21.1','1.21.11','26.1.2','26.2','26.3')][string]$Minecraft='26.3',
    [ValidateSet('fabric','forge','neoforge')][string]$Loader='fabric',
    [ValidateSet('present','absent')][string]$Mode='present',
    [switch]$Manual,
    [switch]$Server,
    [switch]$ControlsOnly,
    [switch]$VisualReport,
    [switch]$ScaleOnly,
    [switch]$Findings,
    [switch]$Fixes,
    [switch]$HotbarProbe,
    [switch]$Walls,
    [switch]$WithRegression,
    [switch]$Transitions,
    [switch]$Complete,
    [switch]$UseDistribution,
    [ValidatePattern('^[a-zA-Z0-9_-]+$')][string]$RunId=('spatial-'+(Get-Date -Format 'yyyyMMdd-HHmmss'))
)
$ErrorActionPreference='Stop'
if($Complete -and $Mode -ne 'present'){throw 'Complete Spatial QA requires the native mod present'}
$repo=(Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
if(($Loader -eq 'forge' -and $Minecraft -ne '1.20.1') -or ($Loader -eq 'neoforge' -and $Minecraft -eq '1.20.1')){throw 'Unsupported Minecraft/loader pair.'}
if($Server -and ($Minecraft -ne '26.3' -or $Loader -ne 'fabric')){throw 'The existing remote-server fixture is specific to 26.3 Fabric.'}
$output=Join-Path $repo $(if($Loader -eq 'fabric'){"build/gameplay-qa/packaged-core/$Minecraft-fabric-$RunId"}else{"build/gameplay-qa/packaged-native/$Minecraft-$Loader-$RunId"})
New-Item -ItemType Directory -Path $output -Force | Out-Null
$manifest=Join-Path $output 'upstream-fixtures.json'
$prepare=@('--minecraft',$Minecraft,'--loader',$Loader,'--scenario','core','--output',$manifest)
if($Mode -eq 'present'){$prepare+=@('--extra-project','spatial-gui')}
& python (Join-Path $repo 'tools/prepare-compatibility-qa.py') @prepare
if($LASTEXITCODE -ne 0){throw 'Unable to prepare Spatial GUI dependencies.'}
$arguments=@('-I',(Join-Path $repo 'functional-tests/compatibility-coverage.init.gradle'),
    '-I',(Join-Path $repo 'functional-tests/packaged-qa.init.gradle'),
    "-PcompatibilityQaManifest=$manifest","-PcompatibilityQaProject=:${Minecraft}:${Loader}",
    '-PcompatibilityQaOriginalJars=true',"-PqaMinecraft=$Minecraft",'-PqaScenario=core',"-PqaRunId=$RunId",
    '-PqaExtension=false','-PqaIntegrationOnly=true','-PsaltsSmokeRun=true',"-PsaltsSmokeProfile=$RunId",
    '-PsaltsDesktopRunDiagnostics=false','-PsaltsDesktopRunTrace=false',":${Minecraft}:fabric:runPackagedGameplay",'--console=plain')
if($Loader -ne 'fabric'){
    $arguments=@('-I',(Join-Path $repo 'functional-tests/chest-close-qa.init.gradle'),
        "-PcompatibilityQaManifest=$manifest",'-PqaFullGameplay=true','-PqaSpatial=true','-PqaScenario=core','-PqaIntegrationOnly=true',
        '-PincludeFunctionalTests=false',"-PqaMinecraft=$Minecraft","-PqaLoader=$Loader","-PqaRunId=$RunId",
        '-PsaltsSmokeRun=true',"-PsaltsSmokeProfile=$RunId",'-PsaltsDesktopRunDiagnostics=false','-PsaltsDesktopRunTrace=false',
        ":${Minecraft}:${Loader}:runFullGameplayQa",'--console=plain')
}
$env:SIU_QA_SPATIAL_ONLY='true'
$env:SIU_QA_SPATIAL_ABSENT=($Mode -eq 'absent').ToString().ToLowerInvariant()
$env:SIU_QA_SPATIAL_MANUAL=$Manual.ToString().ToLowerInvariant()
$env:SIU_QA_SPATIAL_CONTROLS_ONLY=$ControlsOnly.ToString().ToLowerInvariant()
$env:SIU_QA_SPATIAL_REPORT=$VisualReport.ToString().ToLowerInvariant()
$env:SIU_QA_SPATIAL_SCALE_ONLY=$ScaleOnly.ToString().ToLowerInvariant()
$env:SIU_QA_SPATIAL_FINDINGS=$Findings.ToString().ToLowerInvariant()
$env:SIU_QA_SPATIAL_FIXES=$Fixes.ToString().ToLowerInvariant()
$env:SIU_QA_SPATIAL_HOTBAR_PROBE=$HotbarProbe.ToString().ToLowerInvariant()
$env:SIU_QA_WINDOW_WALLS_ONLY=$Walls.ToString().ToLowerInvariant()
$env:SIU_QA_SPATIAL_WITH_REGRESSION=$WithRegression.ToString().ToLowerInvariant()
$env:SIU_QA_SPATIAL_TRANSITIONS=$Transitions.ToString().ToLowerInvariant()
$env:SIU_QA_SPATIAL_COMPLETE=$Complete.ToString().ToLowerInvariant()
if($UseDistribution){$arguments=@('-PqaUseDistribution=true')+$arguments}
if($VisualReport){$arguments=@('-PqaCaptureWidth=1280','-PqaCaptureHeight=720')+$arguments}
if($Server){
    $env:SIU_QA_SPATIAL_SERVER='127.0.0.1:25587'
    # Keep the live server's project cache separate; reuse the client cache across
    # sequential cases while retaining a separate game/evidence directory per run.
    $arguments+=@('--project-cache-dir',(Join-Path $repo 'build/spatial-server-qa/client-project-cache'))
}
Push-Location $repo
try {
    & (Join-Path $repo 'gradlew.bat') @arguments *> (Join-Path $output 'launch.log')
    if($LASTEXITCODE -ne 0){throw "Spatial GUI launch failed: $output"}
    if($Server){
        if(Test-Path -LiteralPath (Join-Path $output 'client-failure.json')){throw "Remote client failed: $output"}
        $report=Get-Content -LiteralPath (Join-Path $output 'server-results.json') -Raw | ConvertFrom-Json
        if(!$report.passed){throw "Remote server verification failed: $output"}
        Write-Output "Spatial GUI $Mode dedicated server PASS: $output"
    }else{
        $report=Get-Content -LiteralPath (Join-Path $output 'results.json') -Raw | ConvertFrom-Json
        if($report.scope -ne "spatial-gui-$Minecraft-$Loader" -or $report.failures -ne 0){throw "Spatial GUI focused checks failed: $output"}
        if($report.coreCodeSource -notmatch '\.jar'){throw 'Spatial QA loaded a source implementation'}
        Write-Output "Spatial GUI $Mode PASS ($($report.results.Count) records): $output"
    }
}finally{
    Pop-Location
    foreach($name in @('SIU_QA_SPATIAL_ONLY','SIU_QA_SPATIAL_ABSENT','SIU_QA_SPATIAL_MANUAL','SIU_QA_SPATIAL_SERVER','SIU_QA_SPATIAL_CONTROLS_ONLY','SIU_QA_SPATIAL_REPORT','SIU_QA_SPATIAL_SCALE_ONLY','SIU_QA_SPATIAL_FINDINGS','SIU_QA_SPATIAL_FIXES','SIU_QA_SPATIAL_HOTBAR_PROBE','SIU_QA_WINDOW_WALLS_ONLY','SIU_QA_SPATIAL_WITH_REGRESSION','SIU_QA_SPATIAL_TRANSITIONS','SIU_QA_SPATIAL_COMPLETE')){
        Remove-Item -LiteralPath "Env:$name" -ErrorAction SilentlyContinue
    }
}
