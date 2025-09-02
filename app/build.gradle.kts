plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.IMsenior"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.IMsenior"
        minSdk = 24
        targetSdk = 35
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
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.firestore)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation ("androidx.camera:camera-core:1.3.0")
    implementation ("androidx.camera:camera-camera2:1.3.0")
    implementation ("androidx.camera:camera-lifecycle:1.3.0")
    implementation ("androidx.camera:camera-view:1.3.0")

    // ML Kit 條碼掃描
    implementation ("com.google.mlkit:barcode-scanning:17.2.0")

    // OkHttp
    implementation ("com.squareup.okhttp3:okhttp:4.9.3")
    implementation(platform("com.google.firebase:firebase-bom:33.11.0"))
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-firestore")
    implementation ("com.google.android.material:material:1.12.0")

    // ZXing 掃描庫
    implementation ("com.journeyapps:zxing-android-embedded:4.3.0")

    //Auth

    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.firebaseui:firebase-ui-auth:9.0.0")

    //AI logic
    implementation("com.google.firebase:firebase-ai:17.2.0")




    // Firebase
    //implementation ("com.google.firebase:firebase-database-ktx")
    // Import the BoM for the Firebase platform
    //implementation(platform("com.google.firebase:firebase-bom:33.10.0"))

    // Add the dependency for the Realtime Database library
    // When using the BoM, you don't specify versions in Firebase library dependencies
    //implementation("com.google.firebase:firebase-database")

    //implementation("com.google.firebase:firebase-core:21.1.1")
}