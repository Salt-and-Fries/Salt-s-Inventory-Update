param(
    [ValidateSet('1.20.1','1.21.1','1.21.11','26.1.2','26.2','26.3')][string]$Minecraft = '26.3',
    [ValidateSet('fabric','forge','neoforge')][string]$Loader = 'fabric',
    [ValidateSet('core','extension-empty','raised')][string]$Mode = 'raised',
    [switch]$MouseTweaks,
    [switch]$HudVisibilityOnly,
    [switch]$HudMovementOnly,
    [ValidatePattern('^[a-zA-Z0-9_-]+$')][string]$RunId = ('raised-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
)
$ErrorActionPreference = 'Stop'
if ($HudVisibilityOnly -and $HudMovementOnly) { throw 'Choose one focused HUD profile.' }
if (($Loader -eq 'forge' -and $Minecraft -ne '1.20.1') -or ($Loader -eq 'neoforge' -and $Minecraft -eq '1.20.1')) {
    throw 'Unsupported Minecraft/loader pair.'
}
$repo = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$scenario = if ($Mode -eq 'core') { 'core' } else { 'extension-empty' }
$profile = "raised-$Mode-$RunId"
$output = if ($Loader -eq 'fabric') {
    Join-Path $repo "build/gameplay-qa/packaged-$scenario/$Minecraft-fabric-$RunId"
} else { Join-Path $repo "build/raised-qa/runtime/$Minecraft-$Loader-$RunId" }
New-Item -ItemType Directory -Path $output -Force | Out-Null
$manifest = Join-Path $output 'upstream-fixtures.json'
$prepare = @('--minecraft',$Minecraft,'--loader',$Loader,'--scenario',$scenario,'--output',$manifest)
if ($Mode -eq 'raised') { $prepare += @('--extra-project','raised') }
if ($MouseTweaks) { $prepare += @('--extra-project','mouse-tweaks') }
& python (Join-Path $repo 'tools/prepare-compatibility-qa.py') @prepare
if ($LASTEXITCODE -ne 0) { throw 'Unable to prepare pinned HUD dependencies.' }
$arguments = @('-I',(Join-Path $repo 'functional-tests/compatibility-coverage.init.gradle'),
    "-PcompatibilityQaManifest=$manifest",'-PsaltsSmokeRun=true',"-PsaltsSmokeProfile=$profile",
    '-PsaltsDesktopRunDiagnostics=false','-PsaltsDesktopRunTrace=false','--console=plain')
if ($Loader -eq 'fabric') {
    $arguments += @('-I',(Join-Path $repo 'functional-tests/packaged-qa.init.gradle'),
        '-PcompatibilityQaOriginalJars=true',"-PcompatibilityQaProject=:${Minecraft}:fabric",
        "-PqaMinecraft=$Minecraft", "-PqaScenario=$scenario", "-PqaRunId=$RunId",
        "-PqaExtension=$($Mode -ne 'core')",'-PqaIntegrationOnly=true','-PincludeFunctionalTests=false',
        '-PqaUseDistribution=true',":${Minecraft}:fabric:runPackagedGameplay")
} else {
    $task = if ($Mode -eq 'core') { ":${Minecraft}:${Loader}:runClient" } else { ":compatibility:${Minecraft}:${Loader}:runClient" }
    $arguments += @('-PincludeFunctionalTests=true',$task)
}
$env:SIU_QA_RAISED_ONLY = (!$HudVisibilityOnly -and !$HudMovementOnly).ToString().ToLowerInvariant()
$env:SIU_QA_HUD_MOVEMENT_ONLY = $HudMovementOnly.ToString().ToLowerInvariant()
$env:SIU_QA_HUD_VISIBILITY_ONLY = $HudVisibilityOnly.ToString().ToLowerInvariant()
$env:SIU_QA_RAISED_ABSENT = ($Mode -ne 'raised').ToString().ToLowerInvariant()
$env:SIU_QA_INTEGRATION_ONLY = 'true'
$env:SIU_QA_SCENARIO = $scenario
$env:SIU_GAMEPLAY_TESTS = 'true'
$env:SIU_FUNCTIONAL_TESTS = if ($Loader -eq 'fabric') { 'false' } else { 'true' }
$env:SIU_FUNCTIONAL_TEST_LOADER = $Loader
$env:SIU_QA_OUTPUT = $output
Push-Location $repo
try {
    & (Join-Path $repo 'gradlew.bat') @arguments *> (Join-Path $output 'launch.log')
    if ($LASTEXITCODE -ne 0) { throw "HUD gameplay launch failed: $output" }
    $report = Get-Content -LiteralPath (Join-Path $output 'results.json') -Raw | ConvertFrom-Json
    if ($report.scope -ne $(if ($HudMovementOnly) { 'window-hosted-hud-movement' } elseif ($HudVisibilityOnly) { 'window-hosted-hud-visibility' } else { 'raised-hud-interoperability' }) -or $report.failures -ne 0) { throw "HUD checks failed: $output" }
    if (($null -ne $report.loadedMods.PSObject.Properties['raised']) -ne ($Mode -eq 'raised')) { throw 'Unexpected Raised presence.' }
    if (($null -ne $report.loadedMods.PSObject.Properties['salts_inventory_compat']) -ne ($Mode -ne 'core')) { throw 'Unexpected Compatibility presence.' }
    if ($Loader -eq 'fabric' -and ($report.coreCodeSource -notmatch '\.jar' -or
            ($Mode -ne 'core' -and $report.compatibilityCodeSource -notmatch '\.jar'))) { throw 'Packaged HUD QA loaded a source implementation.' }
    Write-Output "HUD PASS $Minecraft $Loader $Mode ($($report.results.Count) records): $output"
} finally {
    Pop-Location
    foreach ($name in @('SIU_QA_HUD_MOVEMENT_ONLY','SIU_QA_HUD_VISIBILITY_ONLY','SIU_QA_RAISED_ONLY','SIU_QA_RAISED_ABSENT','SIU_QA_INTEGRATION_ONLY','SIU_QA_SCENARIO',
        'SIU_GAMEPLAY_TESTS','SIU_FUNCTIONAL_TESTS','SIU_FUNCTIONAL_TEST_LOADER','SIU_QA_OUTPUT')) {
        Remove-Item -LiteralPath "Env:$name" -ErrorAction SilentlyContinue
    }
}
