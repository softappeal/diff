defaultTasks("clean", "build", "installDist")

plugins {
    alias(libs.plugins.jvm)
    alias(libs.plugins.ksp)
    application
}

repositories {
    mavenCentral()
}

kotlin {
    compilerOptions {
        extraWarnings.set(true)
        freeCompilerArgs.add("-Xname-based-destructuring=complete")
        allWarningsAsErrors.set(true)
    }
}

dependencies {
    implementation(libs.yass2.core)
    implementation(libs.coroutines.core)
    testImplementation(kotlin("test"))
    ksp(libs.yass2.generate)
}

application {
    mainClass.set("ch.softappeal.diff.MainKt")
}
