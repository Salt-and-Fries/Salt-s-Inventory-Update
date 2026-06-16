plugins {
    id("dev.prism")
}

group = "com.salts_inventory_update"
version = "0.1.0"

prism {
    metadata {
        modId = "salts_inventory_update"
        name = "Salt's Inventory Update"
        description = "Inventory quality-of-life updates for Minecraft."
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
