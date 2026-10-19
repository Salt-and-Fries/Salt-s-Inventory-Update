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
        // Every supported loader compiles these same game-independent classes.
        java.setSrcDirs(listOf(rootProject.file("controlify/common/src/main/java")))
    }
    test {
        java.setSrcDirs(listOf("../common/src/test/java", "../tests", "../versions/26.3/common/src/test/java"))
    }
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
}

// Exercise one legacy copy without adding the workaround to shared/modern production output.
tasks.named<org.gradle.api.tasks.compile.JavaCompile>("compileTestJava") {
    source(rootProject.file("controlify/versions/1.21.1/common/src/main/java/com/salts_inventory_update/controlify/model/CursorWarpEchoTracker.java"))
}

tasks.test {
    useJUnitPlatform()
}
