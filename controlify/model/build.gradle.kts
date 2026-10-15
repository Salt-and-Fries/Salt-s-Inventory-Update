plugins {
    java
}

repositories {
    mavenCentral()
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

sourceSets {
    main {
        java.setSrcDirs(listOf(
            "../common/src/main/java",
            "../versions/26.3/common/src/main/java/com/salts_inventory_update/controlify/model",
            "../versions/26.3/common/src/main/java/com/salts_inventory_update/controlify/input"
        ))
        // These two 26.3 directories also contain the native integration classes.
        java.exclude("ControlifyInputBridge.java", "WorkspaceContainerScreen.java", "WorkspaceScreenProcessor.java")
    }
    test {
        java.setSrcDirs(listOf("../common/src/test/java", "../tests", "../versions/26.3/common/src/test/java"))
    }
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
}

tasks.test {
    useJUnitPlatform()
}
