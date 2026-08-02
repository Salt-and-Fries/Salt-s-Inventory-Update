import org.gradle.api.GradleException
import org.gradle.api.Named
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Usage
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.testing.Test
import java.security.MessageDigest
import java.util.zip.ZipFile

plugins {
    id("dev.prism")
}

group = "com.salts_inventory_update"
version = "0.1.2"

val modMenuVersions = mapOf(
    "1.20.1" to "7.2.2",
    "1.21.1" to "11.0.4",
    "1.21.11" to "17.0.0",
    "26.1.2" to "19.0.0-alpha.1",
    "26.2" to "20.0.0-beta.4"
)

val includeFunctionalTests = providers.gradleProperty("includeFunctionalTests")
    .map { it.equals("true", ignoreCase = true) }
    .orElse(false)
val enableDesktopRunDiagnostics = providers.gradleProperty("saltsDesktopRunDiagnostics")
    .map { it.equals("true", ignoreCase = true) }
    .orElse(true)
val enableDesktopRunTrace = providers.gradleProperty("saltsDesktopRunTrace")
    .map { it.equals("true", ignoreCase = true) }
    .orElse(true)
val includeJeiRuntime = providers.gradleProperty("includeJeiRuntime")
    .map { it.equals("true", ignoreCase = true) }
    .orElse(false)
val includeReiRuntime = providers.gradleProperty("includeReiRuntime")
    .map { it.equals("true", ignoreCase = true) }
    .orElse(false)

val nonFabricSharedSourceExcludes = listOf(
    "**/SaltsInventoryUpdateFabric.java",
    "**/SaltsInventoryUpdateFabricClient.java",
    "**/compat/rei/SaltsReiClientPlugin.java"
)

fun fabricPlatformSourceDir(minecraftVersion: String) =
    rootProject.file(
        if (minecraftVersion == "1.20.1") {
            "versions/fabric-platform-legacy/src/main/java"
        } else {
            "versions/fabric-platform-modern/src/main/java"
        }
    )

fun SourceSet.addLoaderSourceDirs(minecraftVersion: String, loaderName: String) {
    val sourceDirs = listOf(
        rootProject.file("versions/$minecraftVersion/fabric/src/main/java"),
        rootProject.file("versions/$loaderName-shim/src/main/java")
    )
    val currentDirs = java.srcDirs.map { it.canonicalFile }.toMutableSet()
    sourceDirs.forEach { sourceDir ->
        if (currentDirs.add(sourceDir.canonicalFile)) {
            java.srcDir(sourceDir)
        }
    }
    java.exclude(nonFabricSharedSourceExcludes)
}

fun SourceSet.addFunctionalTestSourceDir() {
    val sourceDir = rootProject.file("functional-tests/src/main/java")
    val currentDirs = java.srcDirs.map { it.canonicalFile }.toMutableSet()
    if (currentDirs.add(sourceDir.canonicalFile)) {
        java.srcDir(sourceDir)
    }
}

fun SourceSet.addFabricModMenuSourceDir() {
    val sourceDir = rootProject.file("versions/fabric-modmenu/src/main/java")
    val currentDirs = java.srcDirs.map { it.canonicalFile }.toMutableSet()
    if (currentDirs.add(sourceDir.canonicalFile)) {
        java.srcDir(sourceDir)
    }
}

fun SourceSet.addFabricPlatformSourceDir(minecraftVersion: String) {
    val sourceDir = fabricPlatformSourceDir(minecraftVersion)
    val currentDirs = java.srcDirs.map { it.canonicalFile }.toMutableSet()
    if (currentDirs.add(sourceDir.canonicalFile)) {
        java.srcDir(sourceDir)
    }
}

prism {
    metadata {
        modId = "salts_inventory_update"
        name = "Salt's Inventory Update"
        description = "Salt's Inventory Update upgrades Minecraft inventories with expandable player storage and desktop-style movable container windows. Move, pin, ghost-pin, resize, and snap supported inventory screens, and optionally browse supported recipe browsers in a Salt desktop window with ingredient search, recipe and uses views, bookmarks, history, and Move Items transfers where available. Tune the experience with /saltsinventory config or the mod-list config button. API hooks are available for add-ons and supported screens.\\n\\nDiscord: https://discord.gg/kfdE9gGGxP\\nAPI: https://salt-and-fries.github.io/Salt-s-Inventory-Update/\\nSource: https://github.com/Salt-and-Fries/Salt-s-Inventory-Update\\nDonate: https://www.paypal.com/donate/?business=ERE5F32WV4NWN&no_recurring=1&currency_code=USD"
        license = "MIT"
    }

    // Optional publishing skeleton, modeled after Animal Weights.
    // Fill in project IDs before uncommenting.
    /*
    publishing {
        type = BETA

        curseforge {
            accessToken = providers.environmentVariable("CURSEFORGE_TOKEN")
            projectId = "000000"
        }

        modrinth {
            accessToken = providers.environmentVariable("MODRINTH_TOKEN")
            projectId = "salts-inventory-update"
        }
    }
    */

    // Uncomment when you need CurseMaven dependencies.
    // curseMaven()

    sharedCommon {
        dependencies {
            // Dependencies here are visible to every Minecraft version's common source set.
            // compileOnly("com.google.code.gson:gson:2.10.1")
        }
    }

    version("26.1.2") {
        fabric {
            loaderVersion = "0.19.2"
            fabricApi("0.149.0+26.1.2")
        }
        neoforge {
            loaderVersion = "26.1.2.59-beta"
        }
    }

    version("26.2") {
        fabric {
            loaderVersion = "0.19.3"
            fabricApi("0.153.0+26.2")
        }
        neoforge {
            loaderVersion = "26.2.0.7-beta"
        }
    }

    version("1.21.11") {
        fabric {
            loaderVersion = "0.19.2"
            fabricApi("0.141.4+1.21.11")
        }
        neoforge {
            loaderVersion = "21.11.42"
        }
    }

    version("1.21.1") {
        fabric {
            loaderVersion = "0.16.10"
            fabricApi("0.116.1+1.21.1")
        }
        neoforge {
            loaderVersion = "21.1.95"
        }
    }

    version("1.20.1") {
        fabric {
            loaderVersion = "0.16.10"
            fabricApi("0.92.6+1.20.1")
        }
        forge {
            loaderVersion = "47.4.0"
        }
    }
}

subprojects {
    val minecraftVersion = parent?.name

    plugins.withId("java") {
        dependencies.add("compileOnly", "org.jspecify:jspecify:1.0.0")
        if (path == ":common") {
            dependencies.add("testImplementation", "org.junit.jupiter:junit-jupiter:5.11.4")
            dependencies.add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher:1.11.4")
            tasks.withType<Test>().configureEach {
                useJUnitPlatform()
            }
        }
        tasks.withType<Jar>().configureEach {
            exclude("**/*.pdn")
            from(rootProject.file("LICENSE")) {
                rename { "LICENSE_salts_inventory_update" }
            }
        }
    }

    repositories {
        maven {
            name = "Terraformers"
            url = uri("https://maven.terraformersmc.com/releases/")
        }
        maven {
            name = "Jared's Maven"
            url = uri("https://maven.blamejared.com/")
        }
        maven {
            name = "ModMaven"
            url = uri("https://modmaven.dev/")
        }
        maven {
            name = "Shedaniel"
            url = uri("https://maven.shedaniel.me/")
        }
    }

    tasks.withType<JavaExec>().configureEach {
        if (name == "runClient") {
            val runTask = this
            runTask.doFirst {
                if (enableDesktopRunDiagnostics.get()) {
                    runTask.systemProperty("salts_inventory_update.desktopDebug", "true")
                    if (enableDesktopRunTrace.get()) {
                        runTask.systemProperty("salts_inventory_update.desktopTrace", "true")
                    }
                    logger.lifecycle(
                        "Salt's Inventory Update desktop diagnostics enabled for ${runTask.path} " +
                            "(disable with -PsaltsDesktopRunDiagnostics=false)"
                    )
                }
            }
        }
    }

    if (minecraftVersion != null && name == "forge") {
        afterEvaluate {
            if (minecraftVersion == "1.20.1") {
                dependencies.add("annotationProcessor", "org.spongepowered:mixin:0.8.5:processor")
                val mainSourceSet = extensions.findByType(SourceSetContainer::class.java)?.named("main")?.get()
                val mixinExtension = extensions.findByName("mixin")
                if (mainSourceSet != null && mixinExtension != null) {
                    mixinExtension.javaClass
                        .getMethod("config", String::class.java)
                        .invoke(mixinExtension, "salts_inventory_update.mixins.json")
                    mixinExtension.javaClass
                        .getMethod("add", SourceSet::class.java, String::class.java)
                        .invoke(mixinExtension, mainSourceSet, "salts_inventory_update.refmap.json")
                    logger.lifecycle("Salt's Inventory Update Forge 1.20.1 mixin refmap generation enabled")
                } else {
                    logger.warn("Salt's Inventory Update Forge 1.20.1 mixin refmap generation could not be enabled")
                }
            }
        }
        plugins.withId("java") {
            tasks.named<Jar>("jar") {
                manifest {
                    attributes("MixinConfigs" to "salts_inventory_update.mixins.json")
                }
            }
        }
    }

    if (minecraftVersion != null && name == "fabric") {
        afterEvaluate {
            extensions.findByType(SourceSetContainer::class.java)?.named("main") {
                addFabricModMenuSourceDir()
                addFabricPlatformSourceDir(minecraftVersion)
            }
            modMenuVersions[minecraftVersion]?.let { modMenuVersion ->
                val configurationName = if (minecraftVersion.startsWith("26.")) "compileOnly" else "modCompileOnly"
                if (configurations.findByName(configurationName) != null) {
                    dependencies.add(configurationName, "com.terraformersmc:modmenu:$modMenuVersion")
                }
            }
        }
    }

    val jeiVersions = mapOf(
        "26.2" to "30.3.0.24",
        "26.1.2" to "29.5.0.28",
        "1.21.11" to "27.4.0.22",
        "1.21.1" to "19.25.1.332",
        "1.20.1" to "15.20.0.127"
    )
    if (minecraftVersion != null && (name == "fabric" || name == "neoforge" || name == "forge")) {
        afterEvaluate {
            jeiVersions[minecraftVersion]?.let { jeiVersion ->
                val jeiLoader = when (name) {
                    "forge" -> "forge"
                    "neoforge" -> "neoforge"
                    else -> "fabric"
                }
                val apiConfiguration = if (name == "fabric" && !minecraftVersion.startsWith("26.") && configurations.findByName("modCompileOnly") != null) {
                    "modCompileOnly"
                } else {
                    "compileOnly"
                }
                dependencies.add(apiConfiguration, "mezz.jei:jei-$minecraftVersion-$jeiLoader-api:$jeiVersion")
                if (!includeJeiRuntime.get()) {
                    return@let
                }
                if (name == "forge" && minecraftVersion == "1.20.1") {
                    @Suppress("UNCHECKED_CAST")
                    val mappingsType = Class.forName("net.neoforged.moddevgradle.legacyforge.internal.MinecraftMappings") as Class<Named>
                    val mappingsAttribute = Attribute.of("net.neoforged.moddevgradle.legacy.minecraft_mappings.v2", mappingsType)
                    val namedMappings = objects.named(mappingsType, "named")
                    val jeiForgeRuntimeNamed = configurations.maybeCreate("jeiForgeRuntimeNamed").apply {
                        isCanBeConsumed = false
                        isCanBeResolved = true
                        attributes {
                            attribute(mappingsAttribute, namedMappings)
                            attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.JAR_TYPE)
                            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, Usage.JAVA_RUNTIME))
                        }
                    }
                    dependencies.add(jeiForgeRuntimeNamed.name, "mezz.jei:jei-$minecraftVersion-$jeiLoader:$jeiVersion")
                    tasks.named<JavaExec>("runClient") {
                        classpath += files(jeiForgeRuntimeNamed)
                    }
                } else {
                    val runtimeConfiguration = if (name == "fabric" && !minecraftVersion.startsWith("26.") && configurations.findByName("modRuntimeOnly") != null) {
                        "modRuntimeOnly"
                    } else {
                        "runtimeOnly"
                    }
                    dependencies.add(runtimeConfiguration, "mezz.jei:jei-$minecraftVersion-$jeiLoader:$jeiVersion")
                }
            }
        }
    }

    val reiVersions = mapOf(
        "26.2" to "26.2.820",
        "26.1.2" to "26.1.818",
        "1.21.11" to "21.11.816",
        "1.21.1" to "16.0.799",
        "1.20.1" to "12.1.785"
    )
    val reiBasicMathVersions = mapOf(
        "26.2" to "0.6.1",
        "26.1.2" to "0.6.1",
        "1.21.11" to "0.6.1",
        "1.21.1" to "0.6.1",
        "1.20.1" to "0.6.1"
    )
    val reiClothConfigVersions = mapOf(
        "26.2" to "26.2.155",
        "26.1.2" to "26.1.154",
        "1.21.11" to "21.11.151",
        "1.21.1" to "15.0.130",
        "1.20.1" to "11.0.99"
    )
    val reiArchitecturyVersions = mapOf(
        "26.2" to "21.0.2",
        "26.1.2" to "20.0.6",
        "1.21.11" to "19.0.1",
        "1.21.1" to "13.0.6",
        "1.20.1" to "9.0.7"
    )
    if (minecraftVersion != null && (name == "fabric" || name == "neoforge" || name == "forge")) {
        afterEvaluate {
            reiVersions[minecraftVersion]?.let { reiVersion ->
                val reiLoader = name
                val apiConfiguration = if (name == "fabric" && !minecraftVersion.startsWith("26.") && configurations.findByName("modCompileOnly") != null) {
                    "modCompileOnly"
                } else {
                    "compileOnly"
                }
                dependencies.add(apiConfiguration, "me.shedaniel:RoughlyEnoughItems-api-$reiLoader:$reiVersion")
                dependencies.add(apiConfiguration, "me.shedaniel:RoughlyEnoughItems-default-plugin-$reiLoader:$reiVersion")
                reiBasicMathVersions[minecraftVersion]?.let { basicMathVersion ->
                    dependencies.add(apiConfiguration, "me.shedaniel.cloth:basic-math:$basicMathVersion")
                }
                reiClothConfigVersions[minecraftVersion]?.let { clothConfigVersion ->
                    dependencies.add(apiConfiguration, "me.shedaniel.cloth:cloth-config-$reiLoader:$clothConfigVersion")
                }
                reiArchitecturyVersions[minecraftVersion]?.let { architecturyVersion ->
                    dependencies.add(apiConfiguration, "dev.architectury:architectury-$reiLoader:$architecturyVersion")
                }
                if (includeReiRuntime.get()) {
                    val runtimeConfiguration = if (name == "fabric" && !minecraftVersion.startsWith("26.") && configurations.findByName("modRuntimeOnly") != null) {
                        "modRuntimeOnly"
                    } else {
                        "runtimeOnly"
                    }
                    dependencies.add(runtimeConfiguration, "me.shedaniel:RoughlyEnoughItems-$reiLoader:$reiVersion")
                }
            }
        }
    }

    if (minecraftVersion != null && (name == "forge" || name == "neoforge")) {
        val loaderName = name
        afterEvaluate {
            extensions.findByType(SourceSetContainer::class.java)?.named("main") {
                addLoaderSourceDirs(minecraftVersion, loaderName)
            }
        }
    }

    if (minecraftVersion != null && (name == "fabric" || name == "forge" || name == "neoforge") && includeFunctionalTests.get()) {
        afterEvaluate {
            extensions.findByType(SourceSetContainer::class.java)?.named("main") {
                addFunctionalTestSourceDir()
            }
        }
    }
}

gradle.projectsEvaluated {
    tasks.register("functionalTestCompile") {
        group = "verification"
        description = "Compiles every loader/version with the shared functional test harness. Use -PincludeFunctionalTests=true."
        doFirst {
            if (!includeFunctionalTests.get()) {
                throw GradleException("functionalTestCompile requires -PincludeFunctionalTests=true")
            }
        }
        dependsOn(subprojects
            .filter { it.parent?.name != null && (it.name == "fabric" || it.name == "forge" || it.name == "neoforge") }
            .map { it.tasks.named("compileJava") })
    }

    val uploadableLoaderProjects = subprojects
        .filter { it.parent?.name != null && it.name in setOf("fabric", "forge", "neoforge") }
    val allProjectBuildTasks = subprojects.mapNotNull { subproject ->
        subproject.tasks.findByName("build")?.let { subproject.tasks.named("build") }
    }
    val loaderAssembleTasks = uploadableLoaderProjects.map { it.tasks.named("assemble") }
    val modVersion = version.toString()

    val collectModJars = tasks.register<Sync>("collectModJars") {
        group = "build"
        description = "Collects the ten uploadable mod jars into a clean versioned release directory."
        dependsOn(loaderAssembleTasks)
        mustRunAfter(allProjectBuildTasks)

        into(layout.buildDirectory.dir("release/$modVersion"))
        duplicatesStrategy = DuplicatesStrategy.FAIL

        uploadableLoaderProjects.forEach { loaderProject ->
            from(loaderProject.layout.buildDirectory.dir("libs")) {
                include("*-$modVersion.jar")
            }
        }

        doLast {
            logger.lifecycle("Collected uploadable mod jars in ${destinationDir}")
        }
    }

    val sourceFeatureParity = tasks.register<Exec>("sourceFeatureParity") {
        group = "verification"
        description = "Runs the cross-version and cross-loader source parity contract."
        val shell = if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) "powershell" else "pwsh"
        commandLine(
            shell,
            "-NoProfile",
            "-ExecutionPolicy",
            "Bypass",
            "-File",
            rootProject.file("functional-tests/scripts/Test-SourceFeatureParity.ps1").absolutePath,
            "-RepoRoot",
            rootProject.projectDir.absolutePath
        )
    }

    val verifyNonFabricModJars = tasks.register("verifyNonFabricModJars") {
        group = "verification"
        description = "Fails if Forge or NeoForge upload jars contain Fabric packages or Fabric entrypoint classes."
        val nonFabricProjects = uploadableLoaderProjects.filter { it.name == "forge" || it.name == "neoforge" }
        dependsOn(nonFabricProjects.map { it.tasks.named("assemble") })

        doLast {
            val forbiddenEntries = listOf("fabric.mod.json")
            val forbiddenNameFragments = listOf(
                "SaltsInventoryUpdateFabric.class",
                "SaltsInventoryUpdateFabricClient.class"
            )
            nonFabricProjects.forEach { loaderProject ->
                val jars = loaderProject.layout.buildDirectory.dir("libs").get().asFileTree
                    .matching {
                        include("*-$modVersion.jar")
                        exclude("*-sources.jar")
                    }
                    .files
                jars.forEach { jarFile ->
                    ZipFile(jarFile).use { zip ->
                        val forbidden = zip.entries().asSequence()
                            .map { it.name }
                            .filter { entry ->
                                entry.startsWith("net/fabricmc/") ||
                                    entry in forbiddenEntries ||
                                    forbiddenNameFragments.any(entry::contains)
                            }
                            .toList()
                        if (forbidden.isNotEmpty()) {
                            throw GradleException(
                                "Non-Fabric jar ${jarFile.name} contains forbidden Fabric entries: " +
                                    forbidden.take(20).joinToString(", ")
                            )
                        }
                    }
                }
            }
        }
    }

    val verifyReleaseBundle = tasks.register("verifyReleaseBundle") {
        group = "verification"
        description = "Verifies release count, metadata, resources, loader isolation, and package hygiene."
        dependsOn(collectModJars)
        dependsOn(verifyNonFabricModJars)

        doLast {
            val releaseDirectory = collectModJars.get().destinationDir
            val jars = releaseDirectory.listFiles { file ->
                file.isFile && file.extension.equals("jar", ignoreCase = true)
            }?.sortedBy { it.name } ?: emptyList()
            if (jars.size != 10) {
                throw GradleException("Expected exactly 10 release jars in $releaseDirectory, found ${jars.size}")
            }

            val expectedLoaders = mapOf(
                "1.20.1" to mapOf("fabric" to null, "forge" to "[47.4.0,48)"),
                "1.21.1" to mapOf("fabric" to null, "neoforge" to "[21.1.95,21.2)"),
                "1.21.11" to mapOf("fabric" to null, "neoforge" to "[21.11.42,21.12)"),
                "26.1.2" to mapOf("fabric" to null, "neoforge" to "[26.1.2.59-beta,26.1.3)"),
                "26.2" to mapOf("fabric" to null, "neoforge" to "[26.2.0.7-beta,26.3)")
            )
            val languageLoaderRanges = mapOf(
                "1.20.1" to "[47,48)",
                "1.21.1" to "[4.0.34,5)",
                "1.21.11" to "[10.0.36,11)",
                "26.1.2" to "[11.0.13,12)",
                "26.2" to "[11.0.13,12)"
            )
            val packFormats = mapOf(
                "1.20.1" to 15,
                "1.21.1" to 34,
                "1.21.11" to 75,
                "26.1.2" to 84,
                "26.2" to 88
            )
            val boundedPackFormatVersions = setOf("1.21.11", "26.1.2", "26.2")
            val fabricLoaderMinimums = mapOf(
                "1.20.1" to "0.16.10",
                "1.21.1" to "0.16.10",
                "1.21.11" to "0.19.2",
                "26.1.2" to "0.19.2",
                "26.2" to "0.19.3"
            )
            val javaRequirements = mapOf(
                "1.20.1" to 17,
                "1.21.1" to 21,
                "1.21.11" to 21,
                "26.1.2" to 25,
                "26.2" to 25
            )
            val javaMajors = mapOf(
                "1.20.1" to 61,
                "1.21.1" to 65,
                "1.21.11" to 65,
                "26.1.2" to 69,
                "26.2" to 69
            )
            val mixinCompatibility = mapOf(
                "1.20.1" to "JAVA_17",
                "1.21.1" to "JAVA_21",
                "1.21.11" to "JAVA_21",
                "26.1.2" to "JAVA_25",
                "26.2" to "JAVA_25"
            )

            expectedLoaders.forEach { (minecraftVersion, loaders) ->
                loaders.forEach { (loader, loaderRange) ->
                    val matches = jars.filter { jar ->
                        val name = jar.name.lowercase()
                        name.contains("-$minecraftVersion-") &&
                            name.contains("-${loader.lowercase()}-") &&
                            name.endsWith("-$modVersion.jar")
                    }
                    if (matches.size != 1) {
                        throw GradleException(
                            "Expected one $minecraftVersion $loader $modVersion jar, found ${matches.map { it.name }}"
                        )
                    }

                    val jarFile = matches.single()
                    ZipFile(jarFile).use { zip ->
                        val entries = zip.entries().asSequence().map { it.name }.toSet()
                        val forbidden = entries.filter { entry ->
                            entry.endsWith(".pdn", ignoreCase = true) ||
                                entry.startsWith("org/jspecify/") ||
                                entry.contains("/functionaltest/") ||
                                entry.contains("FunctionalTestHarness")
                        }
                        if (forbidden.isNotEmpty()) {
                            throw GradleException("${jarFile.name} contains forbidden release entries: ${forbidden.take(20)}")
                        }

                        val required = setOf(
                            "assets/salts_inventory_update/icon.png",
                            "LICENSE_salts_inventory_update",
                            "pack.mcmeta",
                            "salts_inventory_update.mixins.json"
                        )
                        val missing = required - entries
                        if (missing.isNotEmpty()) {
                            throw GradleException("${jarFile.name} is missing required entries: $missing")
                        }

                        fun textEntry(name: String): String {
                            val entry = zip.getEntry(name)
                                ?: throw GradleException("${jarFile.name} is missing $name")
                            return zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).use { it.readText() }
                        }

                        val pack = textEntry("pack.mcmeta")
                        if (!pack.contains("\"pack_format\": ${packFormats.getValue(minecraftVersion)}")) {
                            throw GradleException("${jarFile.name} has the wrong pack format for $minecraftVersion")
                        }
                        if (minecraftVersion in boundedPackFormatVersions) {
                            val expectedPackFormat = packFormats.getValue(minecraftVersion)
                            if (!pack.contains("\"min_format\": $expectedPackFormat") ||
                                !pack.contains("\"max_format\": $expectedPackFormat")) {
                                throw GradleException("${jarFile.name} has incorrect pack format bounds for $minecraftVersion")
                            }
                        }

                        val mixin = textEntry("salts_inventory_update.mixins.json")
                        if (!mixin.contains("\"compatibilityLevel\": \"${mixinCompatibility.getValue(minecraftVersion)}\"")) {
                            throw GradleException("${jarFile.name} has a Mixin compatibility level inconsistent with its Java target")
                        }
                        val classEntry = zip.getEntry("com/salts_inventory_update/SaltsInventoryUpdate.class")
                            ?: throw GradleException("${jarFile.name} is missing its main mod class")
                        val classHeader = zip.getInputStream(classEntry).use { it.readNBytes(8) }
                        if (classHeader.size != 8) {
                            throw GradleException("${jarFile.name} has a truncated main mod class")
                        }
                        val classMajor = ((classHeader[6].toInt() and 0xFF) * 256) +
                            (classHeader[7].toInt() and 0xFF)
                        if (classMajor != javaMajors.getValue(minecraftVersion)) {
                            throw GradleException(
                                "${jarFile.name} class major $classMajor does not match Minecraft $minecraftVersion"
                            )
                        }

                        val metadataName = when (loader) {
                            "fabric" -> "fabric.mod.json"
                            "forge" -> "META-INF/mods.toml"
                            else -> "META-INF/neoforge.mods.toml"
                        }
                        val metadata = textEntry(metadataName)
                        val expandedFiles = listOf(metadataName to metadata, "pack.mcmeta" to pack)
                        expandedFiles.forEach { (name, contents) ->
                            if (contents.contains("${'$'}{")) {
                                throw GradleException("${jarFile.name} contains an unexpanded placeholder in $name")
                            }
                        }
                        if (loader == "fabric") {
                            if (!metadata.contains("\"id\": \"salts_inventory_update\"") ||
                                !metadata.contains("\"version\": \"$modVersion\"") ||
                                !metadata.contains("\"license\": \"MIT\"")) {
                                throw GradleException("${jarFile.name} has incorrect Fabric identity, version, or license metadata")
                            }
                            if (!metadata.contains("\"minecraft\": \"$minecraftVersion\"")) {
                                throw GradleException("${jarFile.name} does not target exactly Minecraft $minecraftVersion")
                            }
                            val fabricLoaderMinimum = fabricLoaderMinimums.getValue(minecraftVersion)
                            if (!metadata.contains("\"fabricloader\": \">=$fabricLoaderMinimum\"")) {
                                throw GradleException("${jarFile.name} has the wrong Fabric Loader requirement")
                            }
                            val javaRequirement = javaRequirements.getValue(minecraftVersion)
                            if (!metadata.contains("\"java\": \">=$javaRequirement\"")) {
                                throw GradleException("${jarFile.name} has the wrong Java runtime requirement")
                            }
                            if ("META-INF/mods.toml" in entries || "META-INF/neoforge.mods.toml" in entries) {
                                throw GradleException("${jarFile.name} contains non-Fabric loader metadata")
                            }
                            if (!metadata.contains("SaltsInventoryUpdateFabric") ||
                                !metadata.contains("SaltsInventoryUpdateFabricClient")) {
                                throw GradleException("${jarFile.name} is missing Fabric entrypoints")
                            }
                        } else {
                            if ("fabric.mod.json" in entries) {
                                throw GradleException("${jarFile.name} contains Fabric metadata")
                            }
                            if (!metadata.contains("modId = \"salts_inventory_update\"") ||
                                !metadata.contains("version = \"$modVersion\"") ||
                                !metadata.contains("license = \"MIT\"")) {
                                throw GradleException("${jarFile.name} has incorrect $loader identity, version, or license metadata")
                            }
                            fun dependencyBlock(dependencyModId: String): String? {
                                val blocks = Regex(
                                    """(?ms)^\[\[dependencies\.salts_inventory_update]]\s*\R.*?(?=^\[\[|\z)"""
                                ).findAll(metadata).map { it.value }
                                val modIdLine = "modId = \"$dependencyModId\""
                                return blocks.singleOrNull { block ->
                                    block.lineSequence().any { line -> line.trim() == modIdLine }
                                }
                            }

                            val minecraftDependency = dependencyBlock("minecraft")
                                ?: throw GradleException("${jarFile.name} is missing its structured Minecraft dependency")
                            if (!minecraftDependency.contains("versionRange = \"[$minecraftVersion]\"")) {
                                throw GradleException("${jarFile.name} does not use an exact Minecraft dependency range")
                            }
                            val languageLoaderRange = languageLoaderRanges.getValue(minecraftVersion)
                            if (!metadata.contains("loaderVersion = \"$languageLoaderRange\"")) {
                                throw GradleException("${jarFile.name} has the wrong javafml language-loader range")
                            }
                            if (loaderRange != null) {
                                val loaderDependency = dependencyBlock(loader)
                                    ?: throw GradleException("${jarFile.name} is missing its structured $loader dependency")
                                if (!loaderDependency.contains("versionRange = \"$loaderRange\"")) {
                                    throw GradleException("${jarFile.name} has the wrong $loader dependency range")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val writeReleaseChecksums = tasks.register("writeReleaseChecksums") {
        group = "build"
        description = "Writes SHA-256 checksums for the verified release jars."
        dependsOn(verifyReleaseBundle)

        doLast {
            val releaseDirectory = collectModJars.get().destinationDir
            val digest = MessageDigest.getInstance("SHA-256")
            val lines = releaseDirectory.listFiles { file -> file.extension.equals("jar", ignoreCase = true) }
                ?.sortedBy { it.name }
                ?.map { jar ->
                    digest.reset()
                    val hash = digest.digest(jar.readBytes()).joinToString("") { byte -> "%02x".format(byte) }
                    "$hash  ${jar.name}"
                }
                ?: emptyList()
            releaseDirectory.resolve("SHA256SUMS.txt").writeText(lines.joinToString(System.lineSeparator(), postfix = System.lineSeparator()))
        }
    }

    val rootCheck = tasks.findByName("check")?.let { tasks.named("check") } ?: tasks.register("check") {
        group = "verification"
        description = "Runs root source-parity and release verification checks."
    }
    rootCheck.configure {
        dependsOn(sourceFeatureParity)
        dependsOn(verifyReleaseBundle)
    }

    val rootBuild = tasks.findByName("build")?.let { tasks.named("build") } ?: tasks.register("build") {
        group = "build"
        description = "Assembles and tests every project, then collects uploadable mod jars."
    }
    rootBuild.configure {
        dependsOn(allProjectBuildTasks)
        dependsOn(sourceFeatureParity)
        dependsOn(writeReleaseChecksums)
    }
}
