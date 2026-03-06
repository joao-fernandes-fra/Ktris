plugins {
    id("application")
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.serialization") version "2.0.21"
}

group = "com.kari.ktris"
description = "Ktris - A cross-platform Tetris engine for JVM"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

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
    mainClass.set("demo.KtrisKt")
}

tasks.register<JavaExec>("runDemo") {
    group = "application"
    description = "Runs the Tetris swing demonstration"
    mainClass.set("demo.KtrisKt")
    classpath = sourceSets["main"].runtimeClasspath
}