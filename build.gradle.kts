import org.jetbrains.kotlin.gradle.tasks.*

defaultTasks("clean", "build", "installDist")

version = "2.0.11"

plugins {
    kotlin("jvm") version "1.6.21"
    application
}
val coroutinesCore = "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.1"
fun yass2(module: String) = "ch.softappeal.yass2:yass2-$module:10.0.0"

dependencies {
    implementation(yass2("core"))
    implementation(coroutinesCore)
    testImplementation(yass2("generate"))
    testImplementation(kotlin("test"))
}

repositories {
    mavenCentral()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        allWarningsAsErrors = true
        jvmTarget = "17"
    }
}

application {
    mainClass.set("ch.softappeal.but.MainKt")
}
