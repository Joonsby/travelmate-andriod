// Phase 0 실험 앱. 본 앱(:app)과 따로 설치된다. 결론이 나면 모듈째 지운다.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.joonsby.travelmate.spike"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.joonsby.travelmate.spike"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "phase0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.lottie.compose)
    implementation(libs.livekit.android)
    implementation(libs.okhttp)
}
