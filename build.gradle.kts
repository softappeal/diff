defaultTasks("clean", "build", "installDist")

plugins {
    alias(libs.plugins.jvm)
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.yass2.core)
    implementation(libs.coroutines.core)
    testImplementation(libs.yass2.generate)
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("ch.softappeal.diff.MainKt")
}
