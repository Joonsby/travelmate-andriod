plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// 공개돼도 되는 값들 (클라이언트 ID·앱 키). 비밀값은 여기 두지 않는다.
val kakaoNativeAppKey = "8497a352c443adafdc49eba8e1317b13"
val googleWebClientId = "740283544468-k2q3sgeomh7rrh72olofdokl9rv52pd2.apps.googleusercontent.com"
// 로컬 서버로 시험할 때: ./gradlew installDebug -Ptravelmate.apiBaseUrl=http://10.0.2.2:3000/api
val apiBaseUrl = providers.gradleProperty("travelmate.apiBaseUrl")
    .getOrElse("https://api-travelmate.fit-plate.com/api")

android {
    namespace = "com.joonsby.travelmate"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.joonsby.travelmate"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        buildConfigField("String", "KAKAO_NATIVE_APP_KEY", "\"$kakaoNativeAppKey\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
        manifestPlaceholders["kakaoScheme"] = "kakao$kakaoNativeAppKey"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))
    implementation(project(":feature:auth"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.kakao.user)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}
