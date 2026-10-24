param(
    [string]$Minecraft,
    [ValidateSet('fabric','forge','neoforge')][string]$Loader,
    [switch]$CpuLifecycle,
    [ValidatePattern('^[a-z0-9-]+$')][string]$RunId
)
$ErrorActionPreference='Stop'
$repo=(Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$output=Join-Path $repo "build/gameplay-qa/packaged-native/$Minecraft-$Loader-$RunId"
New-Item -ItemType Directory -Path $output -Force | Out-Null
if(Test-Path -LiteralPath (Join-Path $output 'results.json')){throw 'Choose a fresh run ID'}
$manifest=Join-Path $output 'upstream-fixtures.json'
& python (Join-Path $repo 'tools/prepare-compatibility-qa.py') --minecraft $Minecraft --loader $Loader --scenario ae2-atlas --output $manifest
if($LASTEXITCODE -ne 0){throw 'AE2 fixture preparation failed'}
$arguments=@('-I','functional-tests/chest-close-qa.init.gradle','-I','functional-tests/ae2-release-qa.init.gradle',
    "-PcompatibilityQaManifest=$manifest",'-PqaFullGameplay=true','-PqaScenario=ae2-atlas','-PqaIntegrationOnly=true',
    '-PincludeFunctionalTests=false',"-PqaMinecraft=$Minecraft","-PqaLoader=$Loader","-PqaRunId=$RunId",
    '-PsaltsSmokeRun=true',"-PsaltsSmokeProfile=$RunId",'-PsaltsDesktopRunDiagnostics=false','-PsaltsDesktopRunTrace=false',
    ":${Minecraft}:${Loader}:runFullGameplayQa",'--no-parallel','--max-workers=2','--console=plain')
if($CpuLifecycle){$env:SIU_AE2_ATLAS_FOCUS='craftingcpu';$env:SIU_AE2_CPU_LIFECYCLE_QA='true'}
Push-Location -LiteralPath $repo
try {
    & ./gradlew.bat @arguments *> (Join-Path $output 'launch.log')
    if($LASTEXITCODE -ne 0){throw "AE2 release launch failed: $output"}
    $report=Get-Content -LiteralPath (Join-Path $output 'results.json') -Raw | ConvertFrom-Json
    if($report.failures -ne 0 -or $report.scenario -ne 'ae2-atlas' -or $report.coreCodeSource -notmatch '\.jar' -or
        $report.compatibilityCodeSource -notmatch '\.jar' -or
        !(Select-String -LiteralPath (Join-Path $output 'launch.log') -Pattern 'SIU_GAMEPLAY_SUMMARY status=PASS' -Quiet)){
        throw "AE2 release assertions failed: $output"
    }
    if($CpuLifecycle){
        $state=Get-Content -LiteralPath (Join-Path $output 'atlas/cpu-lifecycle.json') -Raw | ConvertFrom-Json
        if(!$state.nativeJobSubmitted -or !$state.clientActiveStateReceived -or !$state.cancelClickedThroughDesktop -or
            !$state.clientIdleStateReceived -or !$state.serverCancellationVerified -or $state.clientStatusFieldsAssignedByFixture -or
            $state.nativeJobsSubmitted -ne 2 -or $state.serverCancellationsVerified -ne 2){throw 'Native crafting CPU lifecycle failed'}
    }
    Write-Output "AE2 RELEASE PASS $Minecraft/$Loader ($($report.results.Count) records): $output"
}finally{
    Pop-Location
    foreach($name in @('SIU_AE2_ATLAS_FOCUS','SIU_AE2_CPU_LIFECYCLE_QA')){Remove-Item -LiteralPath "Env:$name" -ErrorAction SilentlyContinue}
}
