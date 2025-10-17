plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // ✅ PERBAIKAN: Apply plugin dengan benar
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.todolistapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.todolistapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
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
}

dependencies {
    // --- DEPENDENSI INTI ANDROIDX & MATERIAL DESIGN ---
    // Definisikan versi
    val cameraxVersion = "1.3.1"
    val coroutinesVersion = "1.7.3"
    val glideVersion = "4.16.0"

    // --- FIREBASE & COROUTINES ---
    implementation(platform("com.google.firebase:firebase-bom:33.0.0"))

    // Firebase
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")

    // ✅ TAMBAHAN: Kotlinx Coroutines Play Services (untuk .await())
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:$coroutinesVersion")

    // Kotlin Coroutines & Lifecycle KTX
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")

    // --- CAMERA X dependencies ---
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")
    implementation("androidx.camera:camera-extensions:$cameraxVersion")

    // ✅ TAMBAHAN: Guava (untuk ListenableFuture)
    implementation("com.google.guava:guava:31.1-android")

    // --- LIBRARIES LAINNYA ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // --- FIREBASE (Authentication) ---
    // Firebase BoM (Bill of Materials) - Mengelola versi library Firebase secara otomatis
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-auth-ktx")

    // --- LAYANAN LOGIN PIHAK KETIGA ---
    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    // Facebook Login
    implementation("com.facebook.android:facebook-login:17.0.0")
    // AndroidX Credentials (terkadang dibutuhkan bersamaan dengan login)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    // --- KOTLIN COROUTINES & LIFECYCLE ---
    // Untuk operasi asynchronous
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")

    // --- UI & UTILITIES LAINNYA ---
    // Circle ImageView
    implementation("com.google.android.material:material:1.9.0")

    // Custom Libraries
    implementation("de.hdodenhof:circleimageview:3.1.0")
    // Gson (untuk konversi objek Java ke JSON dan sebaliknya)
    implementation("com.google.code.gson:gson:2.10.1")
    // CameraX (untuk fungsionalitas kamera)

    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:1.2.0-alpha02")
    // Swipe to Refresh
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    // Glide (untuk memuat gambar)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // --- DEPENDENSI UNTUK TESTING ---
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // Glide
    implementation("com.github.bumptech.glide:glide:$glideVersion")
    annotationProcessor("com.github.bumptech.glide:compiler:$glideVersion")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}