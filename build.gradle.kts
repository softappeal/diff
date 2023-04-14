import org.jetbrains.kotlin.gradle.tasks.*

defaultTasks("clean", "build", "installDist")

plugins {
    kotlin("jvm")
    application
}

val coroutinesCore = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${extra["kotlinx-coroutines.version"]}"
fun yass2(module: String) = "ch.softappeal.yass2:yass2-$module:${extra["yass2.version"]}"

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
    mainClass.set("ch.softappeal.diff.MainKt")
}
