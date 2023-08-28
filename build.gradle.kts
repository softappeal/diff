require(libs.versions.ksp.get().startsWith("${libs.versions.kotlin.get()}-")) {
    "kotlin version '${libs.versions.kotlin.get()}' must be a prefix of ksp version '${libs.versions.ksp.get()}'"
}

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
        ksp(libs.yass2.ksp)
    }
}

repositories {
    mavenCentral()
}

application {
    mainClass.set("ch.softappeal.diff.MainKt")
}
