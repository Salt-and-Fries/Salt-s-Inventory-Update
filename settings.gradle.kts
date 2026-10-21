pluginManagement {
    repositories {
        maven { url = uri("https://maven.leclowndu93150.dev/releases") }
        gradlePluginPortal()
        mavenCentral()
        maven { url = uri("https://maven.fabricmc.net/") }
        maven { url = uri("https://maven.neoforged.net/releases") }
        maven { url = uri("https://repo.spongepowered.org/repository/maven-public/") }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
    id("dev.prism.settings") version "0.6.4"
}

rootProject.name = "Salt's Inventory Update"

prism {
    sharedCommon("versions/common")

    version("26.1.2") {
        common()
        fabric()
        neoforge()
    }
    version("26.2") {
        common()
        fabric()
        neoforge()
    }
    version("26.3") {
        common()
        fabric()
        neoforge()
    }
    version("1.21.11") {
        common()
        fabric()
        neoforge()
    }
    version("1.21.1") {
        common()
        fabric()
        neoforge()
    }
    version("1.20.1") {
        common()
        fabric()
        forge()
    }
    mod("controlify") {
        version("1.20.1") {
            common()
            fabric()
            forge()
        }
        version("1.21.1") {
            common()
            fabric()
            neoforge()
        }
        version("1.21.11") {
            common()
            fabric()
            neoforge()
        }
        version("26.1.2") {
            common()
            fabric()
            neoforge()
        }
        version("26.2") {
            common()
            fabric()
            neoforge()
        }
        version("26.3") {
            common()
            fabric()
            neoforge()
        }
    }
    mod("compatibility") {
        version("26.1.2") {
            common()
            fabric()
            neoforge()
        }
        version("26.2") {
            common()
            fabric()
            neoforge()
        }
        version("26.3") {
            common()
            fabric()
            neoforge()
        }
        version("1.21.11") {
            common()
            fabric()
            neoforge()
        }
        version("1.21.1") {
            common()
            fabric()
            neoforge()
        }
        version("1.20.1") {
            common()
            fabric()
            forge()
        }
    }

}

// Keep core project paths stable; the optional mod has its own source tree.
for (gameVersion in listOf("1.20.1", "1.21.1", "1.21.11", "26.1.2", "26.2", "26.3")) {
    project(":compatibility:$gameVersion").projectDir = file("compatibility/versions/$gameVersion")
    for (loader in listOf("common", "fabric", if (gameVersion == "1.20.1") "forge" else "neoforge")) {
        project(":compatibility:$gameVersion:$loader").projectDir = file("compatibility/versions/$gameVersion/$loader")
    }
}
project(":compatibility").projectDir = file("compatibility")

project(":controlify").projectDir = file("controlify")
for (gameVersion in listOf("1.20.1", "1.21.1", "1.21.11", "26.1.2", "26.2", "26.3")) {
    project(":controlify:$gameVersion").projectDir = file("controlify/versions/$gameVersion")
    for (loader in listOf("common", "fabric", if (gameVersion == "1.20.1") "forge" else "neoforge")) {
        project(":controlify:$gameVersion:$loader").projectDir = file("controlify/versions/$gameVersion/$loader")
    }
}
include(":controlify:model")
project(":controlify:model").projectDir = file("controlify/model")
