plugins {
    id("application")
    kotlin("jvm") version "2.0.21"
    id("org.graalvm.buildtools.native") version "0.10.3"
}

group = "com.kari.jtris"
version = "0.0.1-SNAPSHOT"
description = "Jtris - A cross-platform Tetris engine for JVM and Native"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk:1.13.12")

}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
        vendor.set(JvmVendorSpec.GRAAL_VM)
    }

    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)

        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = listOf("-Xjsr305=strict")
    }
}

application {
    mainClass.set("handson.SwingTetrisTestKt")
}

tasks.register<JavaExec>("runPreview") {
    group = "application"
    description = "Runs the Tetris console demonstration"
    mainClass.set("handson.SwingTetrisTestKt")
    classpath = sourceSets["main"].runtimeClasspath
}