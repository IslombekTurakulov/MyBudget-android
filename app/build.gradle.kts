plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.dagger.hilt)
    id("kotlin-parcelize")
    kotlin("plugin.serialization")
    id("com.google.gms.google-services")
    id("androidx.navigation.safeargs.kotlin")
    id("com.google.firebase.crashlytics")
}


android {
    namespace = "ru.iuturakulov.mybudget"
    compileSdk = 34

    defaultConfig {
        applicationId = "ru.iuturakulov.mybudget"
        minSdk = 28
        targetSdk = 34
        versionCode = 5
        versionName = "1.0.4"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    viewBinding {
        enable = true
    }
    lint {
        baseline = file("lint-baseline.xml")
        abortOnError = false
        checkReleaseBuilds = false
    }

    signingConfigs {
        create("release") {
            storeFile = file("${project.rootDir}/mybudget.keystore")
            storePassword = project.findProperty("keystorePassword") as String? ?: ""
            keyAlias = "key0"
            keyPassword = project.findProperty("keyPassword") as String? ?: ""

            println("Debug: Using keystore at: ${storeFile?.absolutePath}")
            println("Debug: Keystore exists: ${storeFile?.exists()}")
            println("Debug: Using key alias: $keyAlias")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson.converter)
    implementation(libs.okhttp.logging.interceptor)

    // Dependency Injection
    implementation(libs.hilt.android)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.datastore.core.android)
    implementation(libs.androidx.material3.android)
    ksp(libs.hilt.android.compiler)

//    implementation(libs.hilt.lifecycle.viewmodel)
//    ksp(libs.hilt.compiler)

    // Encrypted Shared preferences
    implementation(libs.androidx.security.crypto)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Room (для локального кэша, опционально)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // WorkManager для уведомлений и фоновых задач
    implementation(libs.androidx.work.runtime.ktx)

    // ML Kit для OCR
    implementation(libs.mlkit.text.recognition)

    // MPAndroidChart для диаграмм
    implementation(libs.mpandroidchart)

    // OpenCSV для экспорта в CSV
    implementation(libs.opencsv)

    // PDF libraries
    implementation(libs.apache.poi)
    // или
    // implementation(libs.itextpdf.core)

    implementation(libs.timber)


    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    // push-notifications
    implementation(platform("com.google.firebase:firebase-bom:33.10.0"))
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-analytics")

    // image extension
    implementation("com.github.chrisbanes:PhotoView:2.3.0")

    implementation("com.leinardi.android:speed-dial:3.3.0")
    implementation("com.airbnb.android:lottie:6.6.0")

    // Firebase BOM + Messaging
    implementation(platform("com.google.firebase:firebase-bom:32.2.0"))
    implementation("com.google.firebase:firebase-messaging")

    // Ktor HTTP client
    implementation("io.ktor:ktor-client-okhttp:2.3.1")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.1")

    implementation(libs.emoji.google)

    implementation("io.github.chaosleung:pinview:1.4.4")
    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Glide core library
    implementation(libs.glide)

    // Glide annotation processor for KSP
    ksp(libs.glide.ksp)
}
