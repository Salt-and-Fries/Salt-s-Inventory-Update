param(
    [ValidatePattern('^[a-z0-9-]+$')][string]$RunId = 'controlify011',
    [ValidateSet('baseline','focused','mods','all')][string]$Phase = 'all',
    [string[]]$SelectedTargets = @(),
    [switch]$Resume,
    [switch]$PlanOnly
)

$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $false
$repository = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
$pins = Get-Content -LiteralPath (Join-Path $repository 'controlify/targets.json') -Raw | ConvertFrom-Json
$cases = [System.Collections.Generic.List[object]]::new()
function Add-Case($target, [string]$scope, [string]$casePhase, [string]$browser = '') {
    if ($Phase -ne 'all' -and $casePhase -ne $Phase) { return }
    if ($SelectedTargets.Count -and ($target.minecraft + ':' + $target.loader) -notin $SelectedTargets) { return }
    $key = $target.minecraft + '-' + $target.loader
    if ($scope -ne 'all') { $key += '-' + $scope }
    $key += '-extended'
    if ($browser) { $key += '-' + $browser }
    $key += '-' + $RunId
    $cases.Add([pscustomobject]@{
        minecraft = $target.minecraft; loader = $target.loader; scope = $scope; browser = $browser
        phase = $casePhase; key = $key; status = 'PENDING'; startedAt = $null; finishedAt = $null
        launchLog = $null; verificationLog = $null; summary = $null; error = $null
    })
}
foreach ($target in $pins.targets) {
    $legacy = $target.minecraft -in @('1.20.1','1.21.1','1.21.11')
    Add-Case $target $(if ($legacy) { 'feature-parity' } else { 'all' }) 'baseline'
}
foreach ($target in $pins.targets) {
    if ($target.minecraft -in @('1.20.1','1.21.1','1.21.11')) {
        Add-Case $target 'playtest-adapters' 'focused'
    } else {
        Add-Case $target 'public-api' 'focused'
        Add-Case $target 'wheel-polish' 'focused'
    }
    if ($target.minecraft -eq '26.3') {
        foreach ($scope in @('playtest-fixes','status-tray','crafting-qol','wheel-stability')) {
            Add-Case $target $scope 'focused'
        }
        foreach ($browser in @('jei','rei')) { Add-Case $target 'mod-compat' 'mods' $browser }
    }
}
if (!$cases.Count) { throw 'No controller gameplay profiles match the selection.' }
if ($PlanOnly) { $cases | Select-Object minecraft,loader,scope,browser,phase,key; exit 0 }
$output = Join-Path $repository ('build/controlify-release-0.1.1/' + $RunId + '-' + $Phase)
New-Item -ItemType Directory -Path $output -Force | Out-Null
$manifest = Join-Path $output 'results.json'
function Save-Progress {
    [pscustomobject]@{ runId = $RunId; phase = $Phase; profiles = $cases.ToArray() } |
        ConvertTo-Json -Depth 12 | Set-Content -LiteralPath $manifest -Encoding utf8
}
function Verify-Case($case, [string]$summary, [string]$log) {
    $folder = Join-Path $repository ('build/controlify-qa/' + $case.key)
    if ($case.scope -eq 'all') {
        $suffix = $case.key.Substring(($case.minecraft + '-' + $case.loader).Length)
        $arguments = @('tools/verify-controlify-runtime.py','--target',($case.minecraft + ':' + $case.loader),
            '--artifacts','dist','--run-profile','auto',('--report-suffix=' + $suffix),'--with-extension',
            '--feature-parity','--summary',$summary)
    } elseif ($case.scope -in @('mod-compat','crafting-qol','wheel-stability')) {
        $arguments = @('tools/verify-controlify-compat-runtime.py','--report',$folder,'--artifacts','dist','--summary',$summary)
    } else {
        $arguments = @('tools/verify-controlify-focused-runtime.py','--report',$folder,'--artifacts','dist','--summary',$summary)
    }
    $savedErrorPreference = $ErrorActionPreference
    try {
        # Windows PowerShell 5 treats native stderr (including ordinary warnings) as errors.
        $ErrorActionPreference = 'Continue'
        & python @arguments *> $log
        $verificationExitCode = $LASTEXITCODE
    } finally { $ErrorActionPreference = $savedErrorPreference }
    return $verificationExitCode
}
$activeCase = $null
Push-Location -LiteralPath $repository
try {
    Save-Progress
    foreach ($case in $cases) {
        $activeCase = $case
        $case.summary = Join-Path $output ($case.key + '-verified.json')
        $case.verificationLog = Join-Path $output ($case.key + '-verification.log')
        $case.launchLog = Join-Path $output ($case.key + '-launch.log')
        if ($Resume -and (Test-Path -LiteralPath (Join-Path $repository ('build/controlify-qa/' + $case.key + '/results.json')))) {
            if ((Verify-Case $case $case.summary $case.verificationLog) -eq 0) {
                $case.status = 'PREVIOUS_PASS'; Save-Progress
                Write-Output ('VERIFIED CURRENT ARTIFACTS ' + $case.key)
                continue
            }
        }
        $attempt = Join-Path $output ('attempts/' + $case.key + '/' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff'))
        New-Item -ItemType Directory -Path $attempt -Force | Out-Null
        $evidence = Join-Path $repository ('build/controlify-qa/' + $case.key)
        if (Test-Path -LiteralPath $evidence) {
            Copy-Item -LiteralPath $evidence -Destination (Join-Path $attempt 'previous-evidence') -Recurse
        }
        $case.launchLog = Join-Path $attempt 'launch.log'
        $case.verificationLog = Join-Path $attempt 'verification.log'
        $case.summary = Join-Path $attempt 'verified.json'
        $case.status = 'RUNNING'; $case.startedAt = [DateTime]::UtcNow.ToString('o'); Save-Progress
        Write-Output ('START ' + $case.key)
        $arguments = @('-I','controlify/functional-tests/controlify-qa.init.gradle',
            (':controlify:' + $case.minecraft + ':' + $case.loader + ':runPackagedControlifyQa'),
            ('-PcontrolifyQaScope=' + $case.scope),'-PcontrolifyQaExtension=true',
            ('-PcontrolifyQaRunId=' + $RunId),'-PcontrolifyQaJvmVendor=Microsoft',
            '--no-parallel','--max-workers=2','--console=plain')
        if ($case.browser) { $arguments += '-PcontrolifyQaFixtures=' + $case.browser }
        $savedErrorPreference = $ErrorActionPreference
        try {
            $ErrorActionPreference = 'Continue'
            & .\gradlew.bat @arguments *> $case.launchLog
            $launchExit = $LASTEXITCODE
        } finally { $ErrorActionPreference = $savedErrorPreference }
        $verificationExit = if ($launchExit -eq 0) { Verify-Case $case $case.summary $case.verificationLog } else { -1 }
        $case.finishedAt = [DateTime]::UtcNow.ToString('o')
        if ($launchExit -eq 0 -and $verificationExit -eq 0) {
            $case.status = 'PASS'; Write-Output ('PASS ' + $case.key)
        } else {
            $case.status = 'FAIL'; $case.error = "Launch exit $launchExit; verification exit $verificationExit"
            Save-Progress
            throw ($case.key + ': ' + $case.error + '. Logs: ' + $case.launchLog + ', ' + $case.verificationLog)
        }
        Save-Progress
    }
    Write-Output ('Completed ' + $cases.Count + ' current-artifact controller profiles: ' + $manifest)
} catch {
    if ($activeCase -and $activeCase.status -eq 'RUNNING') {
        $activeCase.status = 'FAIL'; $activeCase.error = $_.Exception.Message
        $activeCase.finishedAt = [DateTime]::UtcNow.ToString('o'); Save-Progress
    }
    throw
} finally { Pop-Location }
exit 0
