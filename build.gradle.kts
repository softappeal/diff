import org.jetbrains.kotlin.gradle.tasks.*

defaultTasks("clean", "build", "installDist")

plugins {
    @Suppress("DSL_SCOPE_VIOLATION") alias(libs.plugins.multiplatform) // https://github.com/gradle/gradle/issues/22797
    application
}

kotlin {
    jvm {
        withJava()
        tasks.withType<KotlinCompile> {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    js {
        nodejs()
    }

    targets.all {
        compilations.all {
            kotlinOptions {
                allWarningsAsErrors = true
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.yass2.core)
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(libs.coroutines.core)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jvmTest by getting {
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
