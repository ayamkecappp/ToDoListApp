plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
// ... existing code ...
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")

    // --- TAMBAHAN: Dependensi Firebase ---
    // Firebase BoM (Bill of Materials) - direkomendasikan
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))

    // Firebase Authentication
    implementation("com.google.firebase:firebase-auth-ktx")

    // Google Sign-In (untuk login Google)
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // Facebook Login (untuk login Facebook)
    implementation("com.facebook.android:facebook-login:17.0.0")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("com.google.android.material:material:1.9.0")
    implementation("de.hdodenhof:circleimageview:3.1.0")

    val cameraxVersion = "1.2.3"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:1.2.0-alpha02")

    // BARU: Tambahkan baris ini untuk SwipeRefreshLayout
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    // Tambahkan Glide untuk memuat gambar dan GIF
    implementation("com.github.bumptech.glide:glide:4.16.0")
    // Jika Anda menggunakan Kotlin/KSP, tambahkan annotation processor
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // 2. KOTLIN COROUTINES (Untuk logika looping chat: Job, CoroutineScope, delay)
    // Gunakan versi sesuai dengan versi Kotlin Anda. Versi berikut umum digunakan:
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Coroutines LifeCycle KTX (Penting untuk CoroutineScope dan lifecycle)
    // Versi ini biasanya cocok dengan versi Coroutines di atas
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    // Firebase BoM (Bill of Materials) - direkomendasikan
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))

    // Firebase Authentication
    implementation("com.google.firebase:firebase-auth-ktx")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // Facebook Login
    implementation("com.facebook.android:facebook-login:17.0.0")

    // Dependensi Android lainnya
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
}