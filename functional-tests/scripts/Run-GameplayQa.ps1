param(
    [string]$Minecraft = '1.21.1',
    [ValidateSet('fabric','forge','neoforge')][string]$Loader = 'fabric',
    [ValidateSet('core','extension-empty','browser-jei','browser-emi','browser-rei','toms','sophisticated','ae2','ae2-atlas')][string]$Scenario = 'core',
    [string]$AtlasMenu = '',
    [switch]$CpuLifecycle,
    [switch]$IntegrationOnly,
    [switch]$UpstreamSnapshot,
    [ValidatePattern('^[a-zA-Z0-9_-]*$')][string]$RunId = ''
)
$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$profile = 'gameplay-' + $Scenario
if ($AtlasMenu -and ($Scenario -ne 'ae2-atlas' -or $AtlasMenu -notmatch '^[a-z0-9_-]+$')) { throw 'AtlasMenu requires ae2-atlas and a simple AE2 menu identifier.' }
if ($CpuLifecycle -and ($Scenario -ne 'ae2-atlas' -or $AtlasMenu -ne 'craftingcpu' -or
        !(($Minecraft -eq '1.20.1' -and $Loader -in @('fabric','forge')) -or
          ($Minecraft -in @('1.21.1','26.1.2','26.2','26.3') -and $Loader -eq 'neoforge')))) {
    throw 'CpuLifecycle requires a supported AE2 ae2-atlas craftingcpu focus.'
}
$qaOutputLeaf = "$Minecraft-$Loader"
if ($AtlasMenu) { $profile += '-focus-' + $AtlasMenu.Replace('_', '-'); $qaOutputLeaf += '-focus-' + $AtlasMenu }
if ($CpuLifecycle) { $profile += '-lifecycle'; $qaOutputLeaf += '-lifecycle' }
if ($RunId) { $profile += "-$RunId"; $qaOutputLeaf += "-$RunId" }
$isExtension = $Scenario -ne 'core'
$module = if ($isExtension) { 'compatibility/versions' } else { 'versions' }
$runPath = Join-Path $repo "$module/$Minecraft/$Loader/build/runtime-smoke/$profile"
$outPath = Join-Path $repo "build/gameplay-qa/$Scenario/$qaOutputLeaf"
New-Item -ItemType Directory -Force -Path $outPath | Out-Null
foreach ($captureDirectory in @('screenshots', 'atlas')) {
    $capturePath = Join-Path $outPath $captureDirectory
    if (Test-Path -LiteralPath $capturePath) {
        $resolvedCapture = (Resolve-Path -LiteralPath $capturePath).Path
        $expectedCapture = [System.IO.Path]::GetFullPath((Join-Path $repo "build/gameplay-qa/$Scenario/$qaOutputLeaf/$captureDirectory"))
        if ($resolvedCapture -ne $expectedCapture) { throw 'Unexpected gameplay capture output path.' }
        Remove-Item -LiteralPath $resolvedCapture -Recurse -Force
    }
}
Remove-Item -LiteralPath (Join-Path $outPath 'abort.request') -ErrorAction SilentlyContinue
Remove-Item -LiteralPath (Join-Path $outPath 'results.json') -ErrorAction SilentlyContinue
if ($Scenario -eq 'toms' -and !$UpstreamSnapshot) {
    $fixture = Join-Path $repo 'compatibility/versions/1.21.1/fabric/build/runtime-smoke/toms/mods/toms_storage_fabric-1.21-2.4.2.jar'
    if ($Minecraft -ne '1.21.1' -or $Loader -ne 'fabric' -or !(Test-Path -LiteralPath $fixture)) {
        throw 'Tom fixture requires the downloaded Fabric 1.21.1 jar; do not substitute an unverified version.'
    }
    New-Item -ItemType Directory -Force -Path (Join-Path $runPath 'mods') | Out-Null
    Copy-Item -LiteralPath $fixture -Destination (Join-Path $runPath 'mods') -Force
}
if (!$UpstreamSnapshot -and $Scenario -eq 'browser-rei' -and $Loader -eq 'fabric' -and $Minecraft -eq '1.21.1') {
    $gradleCache = if ($env:GRADLE_USER_HOME) { $env:GRADLE_USER_HOME } else { Join-Path $env:USERPROFILE '.gradle' }
    $modMenuJar = Get-ChildItem -LiteralPath (Join-Path $gradleCache 'caches/modules-2/files-2.1/com.terraformersmc/modmenu/11.0.4') -Recurse -Filter 'modmenu-11.0.4.jar' | Select-Object -First 1
    if (!$modMenuJar) { throw 'Cached ModMenu 11.0.4 fixture is required.' }
    New-Item -ItemType Directory -Force -Path (Join-Path $runPath 'mods') | Out-Null
    Copy-Item -LiteralPath $modMenuJar.FullName -Destination (Join-Path $runPath 'mods') -Force
}
$env:SIU_FUNCTIONAL_TESTS = 'true'
$env:SIU_GAMEPLAY_TESTS = 'true'
if ($IntegrationOnly) { $env:SIU_QA_INTEGRATION_ONLY = 'true' }
$env:SIU_QA_SCENARIO = $Scenario
$env:SIU_FUNCTIONAL_TEST_LOADER = $Loader
$env:SIU_QA_OUTPUT = $outPath
if ($AtlasMenu) { $env:SIU_AE2_ATLAS_FOCUS = $AtlasMenu }
if ($CpuLifecycle) { $env:SIU_AE2_CPU_LIFECYCLE_QA = 'true' }
$task = if ($isExtension) { ":compatibility:${Minecraft}:${Loader}:runClient" } else { ":${Minecraft}:${Loader}:runClient" }
$arguments = @('-PincludeFunctionalTests=true','-PsaltsSmokeRun=true',"-PsaltsSmokeProfile=$profile",'-PsaltsDesktopRunDiagnostics=false','-PsaltsDesktopRunTrace=false',$task,'--console=plain')
if ($UpstreamSnapshot) {
    $fixtureManifest = Join-Path $outPath 'upstream-fixtures.json'
    & python (Join-Path $repo 'tools/prepare-compatibility-qa.py') --minecraft $Minecraft --loader $Loader --scenario $Scenario --output $fixtureManifest
    if ($LASTEXITCODE -ne 0) { throw 'Unable to prepare the pinned upstream QA dependencies.' }
    $arguments += @('-I', (Join-Path $repo 'functional-tests/compatibility-coverage.init.gradle'), "-PcompatibilityQaManifest=$fixtureManifest")
    if ($Scenario -eq 'ae2-atlas') { $arguments += '-PincludeAe2AtlasQa=true' }
} else {
switch ($Scenario) {
    'browser-jei' { $arguments += '-PincludeJeiRuntime=true' }
    'browser-emi' { $arguments += '-PincludeEmiRuntime=true' }
    'browser-rei' { $arguments += '-PincludeReiRuntime=true' }
    'sophisticated' { $arguments += '-PincludeSophisticatedRuntime=true' }
    'ae2' { $arguments += '-PincludeAe2Runtime=true' }
    'ae2-atlas' { $arguments += @('-PincludeAe2Runtime=true','-PincludeAe2AtlasQa=true') }
}
}
$logPath = Join-Path $outPath 'launch.log'
Push-Location $repo
try {
    & (Join-Path $repo 'gradlew.bat') @arguments *> $logPath
    if ($LASTEXITCODE -ne 0) { throw "Gradle launch failed: $logPath" }
    $report = Get-Content -LiteralPath (Join-Path $outPath 'results.json') -Raw | ConvertFrom-Json
    if (!(Select-String -LiteralPath $logPath -Pattern 'SIU_GAMEPLAY_SUMMARY status=PASS' -Quiet)) {
        throw "Gameplay assertions failed ($($report.failures)): $logPath"
    }
    if ($CpuLifecycle) {
        $lifecycle = Get-Content -LiteralPath (Join-Path $outPath 'atlas/cpu-lifecycle.json') -Raw | ConvertFrom-Json
        if (!$lifecycle.nativeJobSubmitted -or !$lifecycle.clientActiveStateReceived -or
                !$lifecycle.cancelClickedThroughDesktop -or !$lifecycle.clientIdleStateReceived -or
                !$lifecycle.serverCancellationVerified -or $lifecycle.clientStatusFieldsAssignedByFixture -or
                $lifecycle.nativeJobsSubmitted -ne 2 -or $lifecycle.serverCancellationsVerified -ne 2) {
            throw 'Live crafting CPU lifecycle assertions failed.'
        }
    }
    Write-Output "$Scenario $Minecraft $Loader PASS: $($report.results.Count) records; $outPath"
} finally {
    Pop-Location
    foreach ($name in @('SIU_FUNCTIONAL_TESTS','SIU_GAMEPLAY_TESTS','SIU_QA_SCENARIO','SIU_FUNCTIONAL_TEST_LOADER','SIU_QA_OUTPUT','SIU_AE2_ATLAS_FOCUS','SIU_AE2_CPU_LIFECYCLE_QA','SIU_QA_INTEGRATION_ONLY')) {
        Remove-Item -LiteralPath "Env:$name" -ErrorAction SilentlyContinue
    }
}
