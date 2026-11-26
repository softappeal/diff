plugins {
    alias(libs.plugins.jvm)
    alias(libs.plugins.boot)
}

repositories {
    mavenCentral()
}

kotlin {
    sourceSets {
        main { kotlin.srcDir("src") }
        test { kotlin.srcDir("test") }
    }

    compilerOptions {
        extraWarnings.set(true)
        freeCompilerArgs.add("-Xname-based-destructuring=complete")
        allWarningsAsErrors.set(true)
    }
}

dependencies {
    implementation(libs.coroutines.core)
    testImplementation(kotlin("test"))
}

tasks.bootJar {
    mainClass.set("ch.softappeal.diff.MainKt")
}
