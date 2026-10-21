param(
    [string[]]$Scenarios = @('extension-empty','browser-jei','browser-rei','browser-emi','toms','sophisticated','ae2'),
    [string[]]$Minecraft = @(),
    [string[]]$Loaders = @(),
    [ValidatePattern('^[a-zA-Z0-9_-]+$')][string]$RunId = ('coverage-' + (Get-Date -Format 'yyyyMMdd-HHmmss')),
    [switch]$IncludeCore,
    [string[]]$ResumeFrom = @(),
    [string[]]$RerunScenarios = @()
)
$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$lock = Get-Content -LiteralPath (Join-Path $repo 'compatibility/coverage-targets.json') -Raw | ConvertFrom-Json
$reportDirectory = Join-Path $repo "build/compatibility-coverage/runs/$RunId"
New-Item -ItemType Directory -Path $reportDirectory -Force | Out-Null
$reportFile = Join-Path $reportDirectory 'results.json'
$cases = [System.Collections.Generic.List[object]]::new()
$providers = @{'browser-jei'='jei';'browser-rei'='rei';'browser-emi'='emi';'toms'='toms-storage';'sophisticated'='sophisticated-core';'ae2'='ae2'}
$previous = @{}
foreach ($previousReport in $ResumeFrom) {
    foreach ($row in @(Get-Content -LiteralPath $previousReport -Raw | ConvertFrom-Json)) {
        if ($row.status -notin @('PASS','PREVIOUS_PASS') -or $row.scenario -in $RerunScenarios) { continue }
        $evidenceFile = Join-Path $row.output 'results.json'
        $evidenceLog = Join-Path $row.output 'launch.log'
        if (!(Test-Path -LiteralPath $evidenceFile) -or !(Test-Path -LiteralPath $evidenceLog)) { throw "Missing previous gameplay evidence: $($row.output)" }
        $evidence = Get-Content -LiteralPath $evidenceFile -Raw | ConvertFrom-Json
        if ($evidence.failures -ne 0 -or $evidence.minecraft -ne $row.minecraft -or $evidence.loader -ne $row.loader -or $evidence.scenario -ne $row.scenario -or
            !(Select-String -LiteralPath $evidenceLog -Pattern 'SIU_GAMEPLAY_SUMMARY status=PASS' -Quiet) -or
            !(Select-String -LiteralPath $evidenceLog -Pattern 'BUILD SUCCESSFUL' -Quiet)) { throw "Previous gameplay result is not a completed pass: $($row.output)" }
        $target = $lock.targets | Where-Object { $_.minecraft -eq $row.minecraft -and $_.loader -eq $row.loader }
        $expected = @()
        if ($providers.ContainsKey($row.scenario)) { $expected = @($providers[$row.scenario]) }
        if ($row.scenario -eq 'sophisticated') { $expected = @('sophisticated-core','sophisticated-backpacks','sophisticated-storage') }
        if ($row.scenario -eq 'browser-rei' -and $row.loader -eq 'fabric') { $expected += 'modmenu' }
        $fixture = Get-Content -LiteralPath (Join-Path $row.output 'upstream-fixtures.json') -Raw | ConvertFrom-Json
        foreach ($integration in $expected) {
            if ($target.integrations.$integration.version_id -notin $fixture.versions.version_id) { throw "Previous upstream pin changed: $($row.minecraft)/$($row.loader)/$integration" }
        }
        $previous["$($row.minecraft)/$($row.loader)/$($row.scenario)"] = [pscustomobject]@{ row=$row; report=$previousReport }
    }
}
foreach ($target in $lock.targets) {
    if ($Minecraft.Count -and $target.minecraft -notin $Minecraft) { continue }
    if ($Loaders.Count -and $target.loader -notin $Loaders) { continue }
    $selected = @($Scenarios)
    if ($IncludeCore) { $selected = @('core') + $selected }
    foreach ($scenario in $selected) {
        if ($providers.ContainsKey($scenario) -and !$target.integrations.($providers[$scenario]).available) { continue }
        $cases.Add([pscustomobject]@{ minecraft=$target.minecraft; loader=$target.loader; scenario=$scenario; status='PENDING'; startedAt=$null; finishedAt=$null; error=$null; output=$null })
    }
}
if ($cases.Count -eq 0) { throw 'No eligible gameplay cases selected.' }
$cases | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $reportFile -Encoding utf8
foreach ($case in $cases) {
    $key = "$($case.minecraft)/$($case.loader)/$($case.scenario)"
    if ($previous.ContainsKey($key)) {
        $proof = $previous[$key]
        $case.status = 'PREVIOUS_PASS'; $case.output = $proof.row.output
        $case.startedAt = $proof.row.startedAt; $case.finishedAt = $proof.row.finishedAt
        $case | Add-Member -NotePropertyName evidenceFrom -NotePropertyValue $proof.report
        $cases | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $reportFile -Encoding utf8
        Write-Output "PREVIOUS PASS $key; $($case.output)"
        continue
    }
    if (Test-Path -LiteralPath (Join-Path $reportDirectory 'stop.request')) { break }
    $case.status = 'RUNNING'
    $case.startedAt = (Get-Date).ToString('o')
    $case.output = Join-Path $repo "build/gameplay-qa/$($case.scenario)/$($case.minecraft)-$($case.loader)-$RunId"
    $cases | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $reportFile -Encoding utf8
    Write-Output "START $($case.minecraft) $($case.loader) $($case.scenario)"
    try {
        & (Join-Path $PSScriptRoot 'Run-GameplayQa.ps1') -Minecraft $case.minecraft -Loader $case.loader -Scenario $case.scenario -IntegrationOnly -UpstreamSnapshot -RunId $RunId
        $case.status = 'PASS'
    } catch {
        $case.status = 'FAIL'
        $case.error = $_.Exception.Message
        Write-Output "FAIL $($case.minecraft) $($case.loader) $($case.scenario): $($case.error)"
    }
    $case.finishedAt = (Get-Date).ToString('o')
    $cases | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $reportFile -Encoding utf8
}
$passed = @($cases | Where-Object status -in @('PASS','PREVIOUS_PASS')).Count
$failed = @($cases | Where-Object status -eq 'FAIL').Count
$pending = @($cases | Where-Object status -eq 'PENDING').Count
Write-Output "Compatibility gameplay matrix: $passed passed, $failed failed, $pending pending; $reportFile"
if ($failed -or $pending) { exit 1 }
exit 0
