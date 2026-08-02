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
import org.gradle.api.tasks.Sync
import java.util.zip.ZipFile

plugins {
    id("dev.prism")
}

group = "com.salts_inventory_update"
version = "0.1.1"

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
                if (minecraftVersion == "1.20.1" && project.name == "forge") {
                    if (!runTask.args.contains("--mixin.config")) {
                        runTask.args("--mixin.config", "salts_inventory_update.mixins.json")
                    }
                    logger.lifecycle("Salt's Inventory Update Forge 1.20.1 mixin config launch arg enabled")
                }
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
        plugins.withId("java") {
            extensions.findByType(SourceSetContainer::class.java)?.named("main") {
                addFabricModMenuSourceDir()
                addFabricPlatformSourceDir(minecraftVersion)
            }
        }
        plugins.withId("java-library") {
            extensions.findByType(SourceSetContainer::class.java)?.named("main") {
                addFabricModMenuSourceDir()
                addFabricPlatformSourceDir(minecraftVersion)
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
        plugins.withId("java") {
            extensions.findByType(SourceSetContainer::class.java)?.named("main") {
                addLoaderSourceDirs(minecraftVersion, loaderName)
            }
        }
        plugins.withId("java-library") {
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
        plugins.withId("java") {
            extensions.findByType(SourceSetContainer::class.java)?.named("main") {
                addFunctionalTestSourceDir()
            }
        }
        plugins.withId("java-library") {
            extensions.findByType(SourceSetContainer::class.java)?.named("main") {
                addFunctionalTestSourceDir()
            }
        }
    }
}

gradle.projectsEvaluated {
    subprojects {
        val minecraftVersion = parent?.name
        if (minecraftVersion != null && name == "fabric") {
            extensions.findByType(SourceSetContainer::class.java)?.named("main") {
                addFabricModMenuSourceDir()
                addFabricPlatformSourceDir(minecraftVersion)
            }
            tasks.named<JavaCompile>("compileJava") {
                source(rootProject.file("versions/fabric-modmenu/src/main/java"))
                source(fabricPlatformSourceDir(minecraftVersion))
            }
        }

        if (minecraftVersion != null && (name == "forge" || name == "neoforge")) {
            val loaderName = name
            extensions.findByType(SourceSetContainer::class.java)?.named("main") {
                addLoaderSourceDirs(minecraftVersion, loaderName)
            }
            tasks.named<JavaCompile>("compileJava") {
                val sourceDirs = listOf(
                    rootProject.file("versions/$minecraftVersion/fabric/src/main/java"),
                    rootProject.file("versions/$loaderName-shim/src/main/java")
                )
                exclude(nonFabricSharedSourceExcludes)
                val currentSourceFiles = source.files.map { it.canonicalFile }.toMutableSet()
                sourceDirs.forEach { sourceDir ->
                    if (currentSourceFiles.add(sourceDir.canonicalFile)) {
                        source(sourceDir)
                    }
                }
            }
        }

        if (minecraftVersion != null && (name == "fabric" || name == "forge" || name == "neoforge") && includeFunctionalTests.get()) {
            extensions.findByType(SourceSetContainer::class.java)?.named("main") {
                addFunctionalTestSourceDir()
            }
            tasks.named<JavaCompile>("compileJava") {
                source(rootProject.file("functional-tests/src/main/java"))
            }
        }
    }

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
        description = "Collects final uploadable mod jars into build/upload-jars."
        dependsOn(loaderAssembleTasks)
        mustRunAfter(allProjectBuildTasks)

        into(layout.buildDirectory.dir("upload-jars"))
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

    val rootBuild = tasks.findByName("build")?.let { tasks.named("build") } ?: tasks.register("build") {
        group = "build"
        description = "Assembles and tests every project, then collects uploadable mod jars."
    }
    rootBuild.configure {
        dependsOn(allProjectBuildTasks)
        dependsOn(collectModJars)
        dependsOn(verifyNonFabricModJars)
    }
}
