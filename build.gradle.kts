import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

defaultTasks("clean", "build", "installDist")

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.ksp)
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
    }
    dependencies {
        add("kspJvm", libs.yass2.ksp)
    }
}

repositories {
    mavenCentral()
}

application {
    mainClass.set("ch.softappeal.diff.MainKt")
}
