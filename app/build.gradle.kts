
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.devtools.ksp")
    }
android {
    namespace = "shivam.sycodes.filefusion"
    compileSdk = 34

    defaultConfig {
        applicationId = "shivam.sycodes.filefusion"
        minSdk = 33
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        renderscriptTargetApi =35
        renderscriptSupportModeEnabled =true

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
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.work.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation (libs.androidx.fragment.ktx)
    implementation (libs.glide)
    implementation (libs.androidx.core)


    implementation(libs.androidx.room.runtime.v261)
    implementation(libs.androidx.room.ktx.v261)
    ksp(libs.androidx.room.compiler.v261)

    implementation(libs.kotlinx.coroutines.core.v163)
    implementation(libs.kotlinx.coroutines.android.v163)

}