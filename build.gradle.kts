plugins {
    kotlin("multiplatform") version "2.3.10"
    kotlin("plugin.serialization") version "2.3.10"
}

group = "com.kari.ktris"
description = "Ktris - A cross-platform Tetris engine for JVM"

repositories {
    mavenCentral()
}

kotlin {
    jvm {
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    js {
        browser {
            binaries.executable()
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jvmMain by getting {
            dependsOn(commonMain)
            dependencies {

            }
        }
        val jvmTest by getting {
            dependsOn(commonTest)
            dependencies {
                implementation(kotlin("test-junit5"))
            }
        }
        val jsMain by getting {
            dependsOn(commonMain)
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-browser:0.3")
            }
        }
        val jsTest by getting {
            dependsOn(commonTest)
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}

tasks.register<JavaExec>("runDemo") {
    group = "application"
    description = "Runs the Tetris Swing demonstration"
    mainClass.set("demo.SwingTetrisTestKt")
    classpath = sourceSets["jvmMain"].runtimeClasspath
}