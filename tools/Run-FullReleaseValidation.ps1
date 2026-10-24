param(
    [ValidatePattern('^[a-z0-9-]+$')][string]$RunId='fullval-20261023',
    [string[]]$Phases=@('core','compatibility','toms-pickup','mouse-tweaks','raised','spatial','controller','ae2-atlas','config','ancient-remnants','accessories','dedicated-server','startup')
)
$ErrorActionPreference='Stop'
$repo=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
Push-Location -LiteralPath $repo
try {
    foreach($phase in $Phases){
        & python tools/run-full-validation.py --phase $phase --run-id $RunId --resume
        if($LASTEXITCODE -ne 0){throw "Full validation stopped at $phase; inspect the preserved failing attempt"}
    }
}finally{Pop-Location}
