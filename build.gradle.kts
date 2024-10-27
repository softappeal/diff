defaultTasks("clean", "build", "installDist")

plugins {
    alias(libs.plugins.jvm)
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("ch.softappeal.diff.MainKt")
}
