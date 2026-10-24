param([ValidatePattern('^[a-zA-Z0-9_-]+$')][string]$RunId='smart-sort')
$ErrorActionPreference='Stop'
$repo=(Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$output=Join-Path $repo "build/gameplay-qa/packaged-core/26.3-fabric-$RunId"
New-Item -ItemType Directory -Force -Path $output | Out-Null
Remove-Item -LiteralPath (Join-Path $output 'results.json') -ErrorAction SilentlyContinue
$arguments=@('-I',(Join-Path $repo 'functional-tests/packaged-qa.init.gradle'),
 '-PqaMinecraft=26.3','-PqaScenario=core',"-PqaRunId=$RunId",'-PqaExtension=false',
 '-PsaltsSmokeRun=true',"-PsaltsSmokeProfile=$RunId",'-PsaltsDesktopRunDiagnostics=false',
 '-PsaltsDesktopRunTrace=false',':26.3:fabric:runPackagedGameplay','--console=plain')
$previousSortFlag=$env:SIU_QA_INVENTORY_SORT_ONLY
$env:SIU_QA_INVENTORY_SORT_ONLY='true'
Push-Location $repo
try {
 & ./gradlew.bat @arguments *> (Join-Path $output 'launch.log')
 if($LASTEXITCODE -ne 0){throw "Inventory sort launch failed: $output"}
 $report=Get-Content (Join-Path $output 'results.json') -Raw | ConvertFrom-Json
 if($report.scope -ne 'in-place-inventory-sorting' -or $report.failures -ne 0){throw "Inventory sorting failed: $output"}
 Write-Output "Inventory sort PASS ($($report.results.Count) records): $output"
} finally {
 Pop-Location
 if($null -eq $previousSortFlag){Remove-Item -LiteralPath Env:SIU_QA_INVENTORY_SORT_ONLY -ErrorAction SilentlyContinue}
 else {$env:SIU_QA_INVENTORY_SORT_ONLY=$previousSortFlag}
}
