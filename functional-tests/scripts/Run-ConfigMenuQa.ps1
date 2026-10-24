param(
 [ValidateSet('1.20.1','1.21.1','1.21.11','26.1.2','26.2','26.3')][string]$Minecraft='26.3',
 [ValidateSet('fabric','forge','neoforge')][string]$Loader='fabric',
 [switch]$UseDistribution,[switch]$ExpectMissing,[switch]$WithoutModMenu,
 [ValidatePattern('^[a-zA-Z0-9_-]*$')][string]$RunId=''
)
$ErrorActionPreference='Stop'
$repo=(Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
if(!$RunId){$RunId=if($ExpectMissing){'config-menu-before'}elseif($WithoutModMenu){'config-menu-standalone'}else{'config-menu-fixed'}}
$output=Join-Path $repo $(if($Loader -eq 'fabric'){"build/gameplay-qa/packaged-core/$Minecraft-fabric-$runId"}else{"build/gameplay-qa/packaged-native/$Minecraft-$Loader-$RunId"})
New-Item -ItemType Directory -Force -Path $output | Out-Null
Remove-Item -LiteralPath (Join-Path $output 'results.json') -ErrorAction SilentlyContinue
$manifest=Join-Path $output 'upstream-fixtures.json'
$prepare=@('--minecraft',$Minecraft,'--loader',$Loader,'--scenario','core','--output',$manifest)
if($Loader -eq 'fabric' -and !$WithoutModMenu){$prepare+=@('--extra-project','modmenu')}
& python (Join-Path $repo 'tools/prepare-compatibility-qa.py') @prepare
if($LASTEXITCODE -ne 0){throw 'Config QA dependency preparation failed'}
$arguments=@('-PsaltsSmokeRun=true',"-PsaltsSmokeProfile=$runId",'-PsaltsDesktopRunDiagnostics=false','-PsaltsDesktopRunTrace=false','--console=plain')
if($Loader -eq 'fabric'){
 $arguments+=@('-I',(Join-Path $repo 'functional-tests/compatibility-coverage.init.gradle'),'-I',(Join-Path $repo 'functional-tests/packaged-qa.init.gradle'),"-PcompatibilityQaManifest=$manifest","-PcompatibilityQaProject=:${Minecraft}:fabric",'-PcompatibilityQaOriginalJars=true',"-PqaMinecraft=$Minecraft",'-PqaScenario=core',"-PqaRunId=$runId",'-PqaExtension=false',":${Minecraft}:fabric:runPackagedGameplay")
 if($UseDistribution){$arguments+='-PqaUseDistribution=true'}
}else{
 $arguments+=@('-I',(Join-Path $repo 'functional-tests/chest-close-qa.init.gradle'),'-PqaFullGameplay=true','-PqaConfigMenu=true','-PqaScenario=core',
    '-PincludeFunctionalTests=false',"-PqaMinecraft=$Minecraft","-PqaLoader=$Loader","-PqaRunId=$RunId",":${Minecraft}:${Loader}:runFullGameplayQa")
}
$env:SIU_QA_CONFIG_MENU='true';$env:SIU_FUNCTIONAL_TEST_LOADER=$Loader
$env:SIU_QA_CONFIG_EXPECT_MISSING=$ExpectMissing.ToString().ToLowerInvariant()
$env:SIU_QA_CONFIG_WITHOUT_MODMENU=$WithoutModMenu.ToString().ToLowerInvariant()
Push-Location $repo
try{
 & ./gradlew.bat @arguments *> (Join-Path $output 'launch.log')
 if($LASTEXITCODE -ne 0){throw "Config menu launch failed: $output"}
 $report=Get-Content (Join-Path $output 'results.json') -Raw | ConvertFrom-Json
 if($report.scope -ne 'config-menu' -or $report.failures -ne 0){throw "Config menu checks failed: $output"}
 if($report.coreCodeSource -notmatch '\.jar'){throw 'Config QA loaded a source implementation'}
 Write-Output "Config menu PASS $Minecraft/$Loader ($($report.results.Count) records): $output"
}finally{
 Pop-Location
 foreach($name in @('SIU_QA_CONFIG_MENU','SIU_FUNCTIONAL_TEST_LOADER','SIU_QA_CONFIG_EXPECT_MISSING','SIU_QA_CONFIG_WITHOUT_MODMENU')){Remove-Item -LiteralPath "Env:$name" -ErrorAction SilentlyContinue}
}
