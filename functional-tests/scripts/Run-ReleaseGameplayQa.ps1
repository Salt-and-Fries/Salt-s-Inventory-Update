param(
    [ValidateSet('1.20.1','1.21.1','1.21.11','26.1.2','26.2','26.3')][string]$Minecraft,
    [ValidateSet('fabric','forge','neoforge')][string]$Loader,
    [ValidateSet('core','extension-empty','browser-emi','browser-jei','browser-rei','sophisticated','toms','ae2')][string]$Scenario='core',
    [switch]$IntegrationOnly,
    [switch]$CompleteCore,
    [switch]$TomsPickupOnly,
    [switch]$DedicatedServer,
    [switch]$UpstreamOnly,
    [switch]$Trace,
    [ValidateSet('','trinkets','curios')][string]$Accessory='',
    [ValidatePattern('^[a-z0-9-]+$')][string]$RunId=('release-'+(Get-Date -Format 'yyyyMMdd-HHmmss'))
)
$ErrorActionPreference='Stop'
$previousCompleteCore=$env:SIU_QA_FULL_CORE
if($CompleteCore -and ($Scenario -ne 'core' -or $IntegrationOnly)){throw 'CompleteCore requires the unrestricted Core scenario'}
if($TomsPickupOnly){
    if($Scenario -ne 'toms' -or $DedicatedServer){throw 'Two-terminal pickup QA requires the Tom client scenario'}
    $IntegrationOnly=$true
}
$repo=(Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
if(($Loader -eq 'forge' -and $Minecraft -ne '1.20.1') -or ($Loader -eq 'neoforge' -and $Minecraft -eq '1.20.1')){throw 'Unsupported target'}
if($UpstreamOnly -and (!$DedicatedServer -or $Scenario -eq 'core')){throw 'UpstreamOnly requires a dedicated integration scenario'}
$output=Join-Path $repo $(if($Loader -eq 'fabric' -and !$DedicatedServer){"build/gameplay-qa/packaged-$Scenario/$Minecraft-fabric-$RunId"}else{"build/gameplay-qa/packaged-native/$Minecraft-$Loader-$RunId"})
New-Item -ItemType Directory -Path $output -Force | Out-Null
if(Test-Path -LiteralPath (Join-Path $output 'results.json')){throw 'Choose a fresh run ID to preserve evidence'}
$manifest=Join-Path $output 'upstream-fixtures.json'
$prepare=@('--minecraft',$Minecraft,'--loader',$Loader,'--scenario',$Scenario,'--output',$manifest)
if($Accessory){
    if($Scenario -ne 'extension-empty'){throw 'Accessory profiles require the extension baseline scenario'}
    $prepare+=@('--extra-project',$Accessory)
}
& python (Join-Path $repo 'tools/prepare-compatibility-qa.py') @prepare
if($LASTEXITCODE -ne 0){throw 'Release fixture preparation failed'}
$argsList=@("-PcompatibilityQaManifest=$manifest","-PqaMinecraft=$Minecraft","-PqaLoader=$Loader","-PqaScenario=$Scenario","-PqaRunId=$RunId",
    "-PqaIntegrationOnly=$($IntegrationOnly.ToString().ToLowerInvariant())",'-PincludeFunctionalTests=false',
    '-PsaltsSmokeRun=true',"-PsaltsSmokeProfile=$RunId",'-PsaltsDesktopRunDiagnostics=false','-PsaltsDesktopRunTrace=false',
    '--no-parallel','--max-workers=2','--console=plain')
if($Loader -eq 'fabric' -and !$DedicatedServer){
    $argsList+=@('-I',(Join-Path $repo 'functional-tests/packaged-qa.init.gradle'),'-I',(Join-Path $repo 'functional-tests/compatibility-coverage.init.gradle'),
        '-PcompatibilityQaOriginalJars=true',"-PcompatibilityQaProject=:${Minecraft}:fabric","-PqaExtension=$($Scenario -ne 'core')",'-PqaUseDistribution=true',":${Minecraft}:fabric:runPackagedGameplay")
}else{
    $argsList+=@('-I',(Join-Path $repo 'functional-tests/chest-close-qa.init.gradle'),'-PqaFullGameplay=true',"-PqaFullCore=$($CompleteCore.ToString().ToLowerInvariant())",":${Minecraft}:${Loader}:runFullGameplayQa")
}
if($DedicatedServer){$argsList+='-PqaDedicatedServer=true'}
if($UpstreamOnly){$argsList+='-PqaUpstreamOnly=true'}
if($TomsPickupOnly){$argsList+='-PqaTomsPickupOnly=true'}
if($Trace){$argsList+='-PqaTrace=true'}
if($Accessory -and !$DedicatedServer){$argsList+=@('-I',(Join-Path $repo 'functional-tests/accessories-qa.init.gradle'),"-PqaAccessory=$Accessory")}
Push-Location -LiteralPath $repo
try{
    $env:SIU_QA_FULL_CORE=$CompleteCore.ToString().ToLowerInvariant()
    & ./gradlew.bat @argsList *> (Join-Path $output 'launch.log')
    if($LASTEXITCODE -ne 0){throw "Release gameplay launch failed: $output"}
    if(Select-String -LiteralPath (Join-Path $output 'launch.log') -Pattern 'Unable to initialize optional integration|Mixin apply.*salts_inventory|Couldn.t load (mod/)?salts_inventory_(update|compat|controlify) pack metadata' -Quiet){
        throw "Caught Salts integration, mixin or resource-pack initialization failure: $output"
    }
    if($DedicatedServer){
        if(!(Select-String -LiteralPath (Join-Path $output 'launch.log') -Pattern 'DEDICATED_RELEASE_SERVER_STOPPED' -Quiet)){throw 'No clean dedicated-server completion'}
        Write-Output "DEDICATED RELEASE SERVER STOPPED $Minecraft/$Loader/${Scenario}: $output"
        return
    }
    $report=Get-Content -LiteralPath (Join-Path $output 'results.json') -Raw | ConvertFrom-Json
    if($report.failures -ne 0 -or $report.minecraft -ne $Minecraft -or $report.loader -ne $Loader -or $report.scenario -ne $Scenario -or
       ($CompleteCore -and !$report.completeCoreFeatureSuite) -or
       !(Select-String -LiteralPath (Join-Path $output 'launch.log') -Pattern 'SIU_GAMEPLAY_SUMMARY status=PASS' -Quiet)){
        throw "Release gameplay assertions failed: $output"
    }
    if($report.coreCodeSource -notmatch '\.jar' -or ($Scenario -ne 'core' -and $report.compatibilityCodeSource -notmatch '\.jar')){
        throw 'Release gameplay loaded implementation output instead of production archives'
    }
    if($TomsPickupOnly){
        if($report.scope -ne 'toms-pickup-issue-17'){throw 'Two-terminal pickup scope was not executed'}
        foreach($terminal in @('toms-storage_terminal','toms-crafting_terminal')){
            foreach($check in @('withdraw-conservation','client-cursor-has-stack','deposit-conservation')){
                $record=@($report.results | Where-Object {$_.name -eq "$terminal-$check"})
                if($record.Count -ne 1 -or $record[0].status -ne 'PASS'){throw "Missing native terminal pickup check: $terminal-$check"}
            }
        }
    }
    Write-Output "RELEASE GAMEPLAY PASS $Minecraft/$Loader/$Scenario ($($report.results.Count) records): $output"
}finally{
    Pop-Location
    if($null -eq $previousCompleteCore){Remove-Item -LiteralPath Env:SIU_QA_FULL_CORE -ErrorAction SilentlyContinue}
    else{$env:SIU_QA_FULL_CORE=$previousCompleteCore}
}
