param(
    [ValidateSet('1.20.1','1.21.1')][string]$Minecraft = '1.21.1',
    [ValidateSet('core','extension-empty','browser-emi','browser-jei','browser-rei','sophisticated','toms','ae2')][string]$Scenario = 'extension-empty',
    [switch]$UseDistribution,
    [ValidatePattern('^[a-zA-Z0-9_-]+$')][string]$RunId = ('packaged-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
)
$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$output = Join-Path $repo "build/gameplay-qa/packaged-$Scenario/$Minecraft-fabric-$RunId"
New-Item -ItemType Directory -Path $output -Force | Out-Null
$manifest = Join-Path $output 'upstream-fixtures.json'
& python (Join-Path $repo 'tools/prepare-compatibility-qa.py') --minecraft $Minecraft --loader fabric --scenario $Scenario --output $manifest
if ($LASTEXITCODE -ne 0) { throw 'Unable to prepare packaged QA dependencies.' }
$includeExtension = if ($Scenario -eq 'core') { 'false' } else { 'true' }
$arguments = @(
    '-I', (Join-Path $repo 'functional-tests/packaged-qa.init.gradle'),
    '-I', (Join-Path $repo 'functional-tests/compatibility-coverage.init.gradle'),
    '-PcompatibilityQaOriginalJars=true', "-PcompatibilityQaManifest=$manifest", "-PcompatibilityQaProject=:${Minecraft}:fabric",
    "-PqaMinecraft=$Minecraft", "-PqaScenario=$Scenario", "-PqaRunId=$RunId",
    "-PqaExtension=$includeExtension", '-PqaIntegrationOnly=true', '-PincludeFunctionalTests=false',
    '-PsaltsSmokeRun=true', "-PsaltsSmokeProfile=packaged-$Scenario-$RunId",
    '-PsaltsDesktopRunDiagnostics=false', '-PsaltsDesktopRunTrace=false',
    ":${Minecraft}:fabric:runPackagedGameplay", '--console=plain'
)
if ($UseDistribution) { $arguments += '-PqaUseDistribution=true' }
$log = Join-Path $output 'launch.log'
Push-Location $repo
try {
    & (Join-Path $repo 'gradlew.bat') @arguments *> $log
    if ($LASTEXITCODE -ne 0) { throw "Packaged Gradle launch failed: $log" }
    $report = Get-Content -LiteralPath (Join-Path $output 'results.json') -Raw | ConvertFrom-Json
    if (!(Select-String -LiteralPath $log -Pattern 'SIU_GAMEPLAY_SUMMARY status=PASS' -Quiet)) {
        throw "Packaged gameplay assertions failed ($($report.failures)): $log"
    }
    if ($report.coreCodeSource -notmatch '\.jar' -or ($includeExtension -eq 'true' -and $report.compatibilityCodeSource -notmatch '\.jar')) {
        throw "Packaged test loaded implementation classes outside production JARs: $log"
    }
    if ($Scenario -eq 'core' -and $report.loadedMods.PSObject.Properties['salts_inventory_compat']) {
        throw "Standalone packaged Core test unexpectedly loaded Compatibility: $log"
    }
    Write-Output "PACKAGED $Scenario $Minecraft fabric PASS: $($report.results.Count) records; $output"
} finally { Pop-Location }
