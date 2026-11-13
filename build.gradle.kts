plugins {
    alias(libs.plugins.jvm)
    alias(libs.plugins.ksp)
    alias(libs.plugins.boot)
}

repositories {
    mavenCentral()
}

kotlin {
    sourceSets {
        main {
            kotlin.srcDir("src")
        }
        test {
            kotlin.srcDir("test")
            resources.srcDir("testResources")
        }
    }

    compilerOptions {
        extraWarnings.set(true)
        freeCompilerArgs.add("-Xname-based-destructuring=complete")
        allWarningsAsErrors.set(true)
    }
}

dependencies {
    implementation(libs.yass2.serialize)
    implementation(libs.coroutines.core)
    testImplementation(kotlin("test"))
    ksp(libs.yass2.generate)
}

tasks.bootJar {
    mainClass.set("ch.softappeal.diff.MainKt")
}
