import org.gradle.api.GradleException
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.JavaExec

plugins {
    id("dev.prism")
}

group = "com.salts_inventory_update"
version = "0.1.0"

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

prism {
    metadata {
        modId = "salts_inventory_update"
        name = "Salt's Inventory Update"
        description = "Salt's Inventory Update upgrades Minecraft inventories with expandable player storage and desktop-style movable container windows. Move, pin, ghost-pin, resize, and snap supported inventory screens, then tune the experience with /saltsinventory config or the mod-list config button. API hooks are available for add-ons and supported screens.\\n\\nDiscord: https://discord.gg/kfdE9gGGxP\\nAPI: https://salt-and-fries.github.io/Salt-s-Inventory-Update/\\nSource: https://github.com/Salt-and-Fries/Salt-s-Inventory-Update\\nDonate: https://www.paypal.com/donate/?business=ERE5F32WV4NWN&no_recurring=1&currency_code=USD"
        license = "MIT"
    }

    // Optional publishing skeleton, modeled after Animal Weights.
    // Fill in project IDs before uncommenting.
    /*
    publishing {
        changelogFile = "CHANGELOG.md"
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
            }
        }
        plugins.withId("java-library") {
            extensions.findByType(SourceSetContainer::class.java)?.named("main") {
                addFabricModMenuSourceDir()
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
            }
            tasks.named<JavaCompile>("compileJava") {
                source(rootProject.file("versions/fabric-modmenu/src/main/java"))
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
}
