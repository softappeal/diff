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
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        val commonTest by getting {
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
