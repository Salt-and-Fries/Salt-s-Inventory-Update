param([string]$QaManifest)
$ErrorActionPreference = 'Stop'
$rolloutPackagingTasks = @()
foreach ($rolloutVersion in @('1.20.1', '1.21.1', '1.21.11', '26.1.2', '26.2', '26.3')) {
    $rolloutOtherLoader = if ($rolloutVersion -eq '1.20.1') { 'forge' } else { 'neoforge' }
    foreach ($rolloutLoader in @('fabric', $rolloutOtherLoader)) {
        $rolloutTask = if ($rolloutLoader -eq 'fabric' -and $rolloutVersion -in @('1.20.1', '1.21.1', '1.21.11')) {
            'remapJar'
        } elseif ($rolloutLoader -eq 'forge') { 'reobfJar' } else { 'jar' }
        $rolloutPackagingTasks += ":${rolloutVersion}:${rolloutLoader}:$rolloutTask"
        $rolloutPackagingTasks += ":compatibility:${rolloutVersion}:${rolloutLoader}:$rolloutTask"
    }
}
$rolloutBuildProperties = @()
if ($QaManifest) { $rolloutBuildProperties += "-PcompatibilityQaManifest=$QaManifest" }
& .\gradlew.bat '-Dorg.gradle.jvmargs=-Xmx1G' @rolloutBuildProperties @rolloutPackagingTasks --console=plain
exit $LASTEXITCODE
