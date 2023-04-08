pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        kotlin("jvm").version(extra["kotlin.version"] as String)
    }
}

rootProject.name = "diff"
