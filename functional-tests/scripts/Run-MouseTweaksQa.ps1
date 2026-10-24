param(
    [ValidateSet('1.20.1','1.21.1','1.21.11','26.1.2','26.2','26.3')][string]$Minecraft,
    [ValidateSet('fabric','forge','neoforge')][string]$Loader,
    [ValidatePattern('^[a-z0-9-]+$')][string]$RunId
)
$ErrorActionPreference='Stop'
$repo=(Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
& python (Join-Path $repo 'tools/prepare-mouse-tweaks-qa.py') --minecraft $Minecraft --loader $Loader
if($LASTEXITCODE -ne 0){throw 'Mouse Tweaks fixture preparation failed'}
$argsList=@('-I','functional-tests/chest-close-qa.init.gradle','-PqaMouseTweaksOnly=true',
    "-PqaMinecraft=$Minecraft","-PqaLoader=$Loader","-PqaRunId=$RunId",'-PsaltsDesktopRunDiagnostics=false','-PsaltsDesktopRunTrace=false',
    ":${Minecraft}:${Loader}:runMouseTweaksQa",'--no-parallel','--max-workers=2','--console=plain')
$output=Join-Path $repo "build/mouse-tweaks/gameplay/$Minecraft-$Loader-$RunId"
New-Item -ItemType Directory -Path $output -Force | Out-Null
if(Test-Path -LiteralPath (Join-Path $output 'results.json')){throw 'Choose a fresh run ID'}
Push-Location -LiteralPath $repo
try{
    & ./gradlew.bat @argsList *> (Join-Path $output 'launch.log')
    if($LASTEXITCODE -ne 0){throw "Mouse Tweaks launch failed: $output"}
    $report=Get-Content -LiteralPath (Join-Path $output 'results.json') -Raw | ConvertFrom-Json
    if($report.failures -ne 0 -or $report.scope -ne 'mouse-tweaks-core-only' -or !$report.loadedMods.mousetweaks -or
        $report.loadedMods.salts_inventory_compat -or $report.loadedMods.salts_inventory_controlify -or $report.coreCodeSource -notmatch '\.jar'){
        throw "Mouse Tweaks gameplay failed: $output"
    }
    Write-Output "Mouse Tweaks PASS $Minecraft/$Loader ($($report.results.Count) records): $output"
}finally{Pop-Location}
