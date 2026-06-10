plugins {

    alias(libs.plugins.android.application)

    alias(libs.plugins.kotlin.android)

    alias(libs.plugins.kotlin.compose)

    id("kotlin-kapt")

}



android {

    namespace = "com.example.myapplication"

    compileSdk {

        version = release(36)

    }



    defaultConfig {

        applicationId = "com.example.myapplication"

        minSdk = 26  // 升级到26以支持Apache POI

        targetSdk = 36

        versionCode = 1

        versionName = "1.0"



        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"



        // 提供给网络层使用的后端 Base URL，可通过 BuildConfig.API_BASE_URL 访问
        // 当前使用 Android 模拟器，因此这里配置为 10.0.2.2
        buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8080/\"")
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

    buildFeatures {

        compose = true

        // 显式启用 BuildConfig 生成，方便通过 BuildConfig 读取自定义配置

        buildConfig = true

    }

}



dependencies {

    implementation(libs.androidx.core.ktx)

    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.compose.ui)

    implementation(libs.androidx.compose.ui.graphics)

    implementation(libs.androidx.compose.ui.tooling.preview)

    implementation(libs.androidx.compose.material3)

    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)

    implementation("androidx.compose.material:material-icons-extended")

    

    // Room

    implementation(libs.androidx.room.runtime)

    implementation(libs.androidx.room.ktx)

    kapt(libs.androidx.room.compiler)

    

    // Navigation

    implementation(libs.androidx.navigation.compose)

    

    // ViewModel

    implementation(libs.androidx.lifecycle.viewmodel.compose)

    

    // WorkManager

    implementation(libs.androidx.work.runtime.ktx)

    

    // Retrofit & Gson

    implementation(libs.retrofit)

    implementation(libs.retrofit.gson)

    implementation(libs.gson)

    

    // OkHttp (for AI model service)

    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    

    // Coroutines

    implementation(libs.kotlinx.coroutines.android)

    

    // Coil for image loading

    implementation("io.coil-kt:coil-compose:2.5.0")

    

    // ZXing for QR code generation and scanning

    implementation("com.google.zxing:core:3.5.2")

    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    

    // Apache POI for Excel file parsing

    implementation("org.apache.poi:poi:5.2.5")

    implementation("org.apache.poi:poi-ooxml:5.2.5")

    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    

    // CSV parsing (simple implementation, no external library needed)

    

    // 添加 FFmpegKit 用于音频转码

    implementation("com.arthenica:ffmpeg-kit-full:4.5.LTS")

    

    // 阿里云 OSS Android SDK

    implementation("com.aliyun.dpa:oss-android-sdk:2.9.21")

    

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.junit)

    androidTestImplementation(libs.androidx.espresso.core)

    androidTestImplementation(platform(libs.androidx.compose.bom))

    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)

    debugImplementation(libs.androidx.compose.ui.test.manifest)



}