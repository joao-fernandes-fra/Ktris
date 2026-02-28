plugins {
    id("application")
    kotlin("jvm") version "2.0.21"
}

group = "com.kari.jtris"
version = "0.0.1-SNAPSHOT"
description = "Ktris - A cross-platform Tetris engine for JVM"

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

application {
    mainClass.set("demo.SwingTetrisTestKt")
}

tasks.register<JavaExec>("runDemo") {
    group = "application"
    description = "Runs the Tetris swing demonstration"
    mainClass.set("demo.SwingTetrisTestKt")
    classpath = sourceSets["main"].runtimeClasspath
}