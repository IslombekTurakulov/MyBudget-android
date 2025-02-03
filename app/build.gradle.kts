plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.dagger.hilt)
    id("kotlin-parcelize")
    id("androidx.navigation.safeargs.kotlin")
}


android {
    namespace = "ru.iuturakulov.mybudget"
    compileSdk = 34

    defaultConfig {
        applicationId = "ru.iuturakulov.mybudget"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    viewBinding {
        enable = true
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

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Glide core library
    implementation(libs.glide)

    // Glide annotation processor for KSP
    ksp(libs.glide.ksp)
}