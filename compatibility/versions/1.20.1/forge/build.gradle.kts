// This native-menu hook belongs only to the 1.20.1 Compatibility mod.
tasks.withType<org.gradle.jvm.tasks.Jar>().matching { it.name == "jar" }.configureEach {
    inputs.file(layout.projectDirectory.file("feeding-trough-manifest.mf"))
    // Merge at archive writing time, after the shared loader tooling sets its configs.
    manifest.from(layout.projectDirectory.file("feeding-trough-manifest.mf").asFile) {
        eachEntry {
            if (key == "MixinConfigs") value = listOfNotNull(baseValue, mergeValue)
                .flatMap { it.toString().split(',') }.distinct().joinToString(",")
        }
    }
}

afterEvaluate {
    dependencies.add("compileOnly", "io.github.llamalad7:mixinextras-common:0.5.5")
    dependencies.add("annotationProcessor", "io.github.llamalad7:mixinextras-common:0.5.5")
    dependencies.add("jarJar", "io.github.llamalad7:mixinextras-forge:0.5.5")
    extensions.findByName("mixin")?.let { mixin ->
        mixin.javaClass.getMethod("config", String::class.java)
            .invoke(mixin, "salts_inventory_compat.ssn.mixins.json")
        mixin.javaClass.getMethod("config", String::class.java)
            .invoke(mixin, "salts_inventory_compat.ssn-transport.mixins.json")
        mixin.javaClass.getMethod("config", String::class.java)
            .invoke(mixin, "salts_inventory_compat.reviewed-native.mixins.json")
        mixin.javaClass.getMethod("config", String::class.java)
            .invoke(mixin, "salts_inventory_compat.simplyswords.mixins.json")
        mixin.javaClass.getMethod("config", String::class.java)
            .invoke(mixin, "salts_inventory_compat.feeding-trough.mixins.json")
        mixin.javaClass.getMethod("config", String::class.java)
            .invoke(mixin, "salts_inventory_compat.chipped.mixins.json")
    }
}
