param([string]$JavaHome=$env:JAVA_HOME)
$ErrorActionPreference='Stop'
$repo=(Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$cache=if($env:GRADLE_USER_HOME){$env:GRADLE_USER_HOME}else{Join-Path $HOME '.gradle'}
$joml=Get-ChildItem (Join-Path $cache 'caches/modules-2/files-2.1/org.joml/joml') -Recurse -Filter '*.jar' |
    Where-Object Name -Match '^joml-[0-9.]+\.jar$' |
    Sort-Object FullName -Descending | Select-Object -First 1 -ExpandProperty FullName
if(!$joml){throw 'Build :26.3:fabric:compileJava first to resolve JOML.'}
$javac=if($JavaHome){Join-Path $JavaHome 'bin/javac.exe'}else{(Get-Command javac).Source}
$java=if($JavaHome){Join-Path $JavaHome 'bin/java.exe'}else{(Get-Command java).Source}
$output=Join-Path $repo 'build/spatial-gui-analysis/transition-unit'
& $javac -cp $joml -d $output (Join-Path $repo 'core-integrations/spatial-gui/src/main/java/com/salts_inventory_update/spatial/SpatialSurfaceTransition.java') (Join-Path $repo 'functional-tests/spatial-unit/SpatialSurfaceTransitionTest.java')
if($LASTEXITCODE -ne 0){throw 'Spatial transition test compilation failed.'}
& $java -cp "$output;$joml" com.salts_inventory_update.spatial.SpatialSurfaceTransitionTest
if($LASTEXITCODE -ne 0){throw 'Spatial transition continuity assertions failed.'}
