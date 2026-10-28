// This native-menu hook belongs only to the 1.20.1 Compatibility mod.
tasks.withType<org.gradle.jvm.tasks.Jar>().matching { it.name == "jar" }.configureEach {
    // Merge at archive writing time, after the shared loader tooling sets its configs.
    manifest.from(layout.projectDirectory.file("feeding-trough-manifest.mf").asFile) {
        eachEntry {
            if (key == "MixinConfigs") value = listOfNotNull(baseValue, mergeValue)
                .flatMap { it.toString().split(',') }.distinct().joinToString(",")
        }
    }
}

afterEvaluate {
    extensions.findByName("mixin")?.let { mixin ->
        mixin.javaClass.getMethod("config", String::class.java)
            .invoke(mixin, "salts_inventory_compat.simplyswords.mixins.json")
        mixin.javaClass.getMethod("config", String::class.java)
            .invoke(mixin, "salts_inventory_compat.feeding-trough.mixins.json")
        mixin.javaClass.getMethod("config", String::class.java)
            .invoke(mixin, "salts_inventory_compat.chipped.mixins.json")
    }
}
