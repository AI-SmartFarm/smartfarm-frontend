plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.smartfarm.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.smartfarm.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    // XML 안의 View를 findViewById 없이 binding.아이디 로 쓰기 위한 설정
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // 기본 Android / UI
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Navigation (화면 이동)
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.4")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.4")

    // ── 보조 담당이 나중에 추가할 것들 (지금은 주석) ──
    // Retrofit (Backend API 통신)
    // implementation("com.squareup.retrofit2:retrofit:2.11.0")
    // implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    // 코루틴 + lifecycleScope (Polling)
    // implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    // FCM은 Firebase 프로젝트 연결(google-services.json) 후 추가
}
