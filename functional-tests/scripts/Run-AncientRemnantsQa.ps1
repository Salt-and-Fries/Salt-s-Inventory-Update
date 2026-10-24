param(
    [ValidateSet('1.20.1','1.21.1','26.1.2')][string]$Minecraft,
    [ValidateSet('fabric','forge','neoforge')][string]$Loader,
    [ValidateSet('rei','jei')][string]$Browser='rei',
    [ValidatePattern('^[a-z0-9-]+$')][string]$RunId=('ancient-'+(Get-Date -Format 'yyyyMMdd-HHmmss'))
)
$ErrorActionPreference='Stop'
$repo=(Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$output=Join-Path $repo "build/ancient-remnants-issue16/gameplay/$Minecraft-$Loader-$RunId"
New-Item -ItemType Directory -Path $output -Force | Out-Null
if(Test-Path -LiteralPath (Join-Path $output 'results.json')){throw 'Choose a fresh run ID'}
$manifest=Join-Path $output 'upstream-fixtures.json'
& python (Join-Path $repo 'tools/prepare-compatibility-qa.py') --minecraft $Minecraft --loader $Loader --scenario "browser-$Browser" --extra-project ancient-remnants --output $manifest
if($LASTEXITCODE -ne 0){throw 'Ancient Remnants fixture preparation failed'}
$arguments=@('-I',(Join-Path $repo 'functional-tests/chest-close-qa.init.gradle'),'-PqaBrowserSearchOnly=true',
    "-PqaMinecraft=$Minecraft","-PqaLoader=$Loader","-PqaScenario=browser-$Browser","-PqaRunId=$RunId",
    "-PcompatibilityQaManifest=$manifest",'-PincludeFunctionalTests=false','-PsaltsSmokeRun=true',"-PsaltsSmokeProfile=$RunId",
    ":${Minecraft}:${Loader}:runBrowserSearchQa",'--no-parallel','--max-workers=2','--console=plain')
Push-Location -LiteralPath $repo
try {
    & ./gradlew.bat @arguments *> (Join-Path $output 'launch.log')
    if($LASTEXITCODE -ne 0){throw "Browser-search launch failed: $output"}
    $report=Get-Content -LiteralPath (Join-Path $output 'results.json') -Raw | ConvertFrom-Json
    if($report.failures -ne 0 -or !(Select-String -LiteralPath (Join-Path $output 'launch.log') -Pattern 'SIU_GAMEPLAY_SUMMARY status=PASS' -Quiet)){
        throw "Browser-search assertions failed: $output"
    }
    Write-Output "ANCIENT REMNANTS PASS $Minecraft/$Loader/$Browser ($($report.results.Count) records): $output"
}finally{Pop-Location}
