pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }

    plugins {
        id("com.android.application") version "8.6.1"
        id("org.jetbrains.kotlin.android") version "2.0.0"
        id("com.google.dagger.hilt.android") version "2.51"
        id("com.google.devtools.ksp") version "2.0.0-1.0.23"
        id("com.google.gms.google-services") version "4.4.2" apply false
    }
}

rootProject.name = "MyBudget"
include(":app")