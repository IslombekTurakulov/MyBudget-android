pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }

    plugins {
        id("com.android.application") version "8.1.1"
        id("org.jetbrains.kotlin.android") version "1.9.0"
        id("com.google.dagger.hilt.android") version "2.44"
        id("com.google.devtools.ksp") version "1.9.0-1.0.11"
    }
}

rootProject.name = "MyBudget"
include(":app")