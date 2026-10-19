param(
    [string[]]$Minecraft = @(),
    [ValidateSet('fabric','forge','neoforge')][string[]]$Loader = @(),
    [Alias('Target')][string[]]$SelectedTargets = @(),
    [switch]$ContinueAfterFailure
)

$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $false
$repository = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
$inventory = Get-Content -LiteralPath (Join-Path $repository 'controlify/targets.json') -Raw | ConvertFrom-Json
$targets = @($inventory.targets | Where-Object {
    (!$Minecraft.Count -or $_.minecraft -in $Minecraft) -and (!$Loader.Count -or $_.loader -in $Loader) -and
    (!$SelectedTargets.Count -or ($_.minecraft + ':' + $_.loader) -in $SelectedTargets)
})
if (!$targets.Count) { throw 'The selection does not match a pinned Minecraft/loader target.' }

Push-Location -LiteralPath $repository
try {
    $failed = @()
    foreach ($target in $targets) {
        $key = $target.minecraft + '-' + $target.loader
        $attempt = Join-Path $repository ('build/controlify-qa/' + $key + '/attempts/' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff'))
        New-Item -ItemType Directory -Path $attempt -Force | Out-Null
        $launchLog = Join-Path $attempt 'launch.log'
        Write-Output ('Running packaged controller QA: ' + $key)
        $task = ':controlify:' + $target.minecraft + ':' + $target.loader + ':runPackagedControlifyQa'
        & .\gradlew.bat '-I' 'controlify/functional-tests/controlify-qa.init.gradle' $task '--console=plain' '--no-parallel' *> $launchLog
        $launchExit = $LASTEXITCODE
        $resultDirectory = Join-Path $repository ('build/controlify-qa/' + $key)
        foreach ($name in @('results.json','staged-artifacts.json','forge-remap-lineage.json','screenshots')) {
            $evidence = Join-Path $resultDirectory $name
            if (Test-Path -LiteralPath $evidence) { Copy-Item -LiteralPath $evidence -Destination $attempt -Recurse }
        }
        $verificationExit = 1
        if ($launchExit -eq 0) {
            & python 'tools/verify-controlify-runtime.py' '--target' ($target.minecraft + ':' + $target.loader) *> (Join-Path $attempt 'verification.log')
            $verificationExit = $LASTEXITCODE
        }
        Get-Content -LiteralPath $launchLog -Tail 16
        if ($launchExit -ne 0 -or $verificationExit -ne 0) {
            $failed += $key
            Write-Output ('Packaged QA failed: ' + $key + '. Evidence: ' + $attempt)
            if (!$ContinueAfterFailure) { break }
        } else {
            Write-Output ('Packaged QA passed: ' + $key)
        }
    }
    if ($failed.Count) { throw ('Unverified controller targets: ' + ($failed -join ', ')) }
    if (!$Minecraft.Count -and !$Loader.Count -and !$SelectedTargets.Count) {
        & python 'tools/verify-controlify-runtime.py' '--summary' 'build/controlify-qa/validated-matrix.json'
        if ($LASTEXITCODE -ne 0) { throw 'The complete final-artifact runtime audit failed.' }
    }
} finally {
    Pop-Location
}
