import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

defaultTasks("clean", "build", "installDist")

plugins {
    alias(libs.plugins.multiplatform)
    application
}

kotlin {
    jvm {
        withJava()
    }
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        allWarningsAsErrors = true
    }
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.yass2.core)
            }
        }
        jvmMain {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        jvmTest {
            dependencies {
                implementation(libs.yass2.generate)
            }
        }
    }
}

repositories {
    mavenCentral()
}

application {
    mainClass.set("ch.softappeal.diff.MainKt")
}
