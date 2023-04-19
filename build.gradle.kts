import org.jetbrains.kotlin.gradle.tasks.*

defaultTasks("clean", "build", "installDist")

plugins {
    kotlin("multiplatform")
    application
}

fun coroutines(module: String) = "org.jetbrains.kotlinx:kotlinx-coroutines-$module:${extra["kotlinx-coroutines.version"]}"
fun yass2(module: String) = "ch.softappeal.yass2:yass2-$module:${extra["yass2.version"]}"

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
                implementation(yass2("core"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(coroutines("core"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(yass2("generate"))
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
