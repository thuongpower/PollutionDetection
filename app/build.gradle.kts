plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.pollutiondetection"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.pollutiondetection"
        minSdk = 30
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
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // https://mvnrepository.com/artifact/org.tensorflow/tensorflow-lite
    runtimeOnly("org.tensorflow:tensorflow-lite:2.17.0")
    // https://mvnrepository.com/artifact/org.tensorflow/tensorflow-lite-support
    runtimeOnly("org.tensorflow:tensorflow-lite-support:0.5.0")
    // https://mvnrepository.com/artifact/org.tensorflow/tensorflow-lite-task-vision
    runtimeOnly("org.tensorflow:tensorflow-lite-task-vision:0.4.4")


    // https://mvnrepository.com/artifact/io.github.lucksiege/camerax
    implementation("io.github.lucksiege:camerax:v3.11.3")
    // https://mvnrepository.com/artifact/androidx.camera/camera-core
    implementation("androidx.camera:camera-core:1.4.2")
    // https://mvnrepository.com/artifact/androidx.camera/camera-lifecycle
    runtimeOnly("androidx.camera:camera-lifecycle:1.4.2")
    // https://mvnrepository.com/artifact/androidx.camera/camera-extensions
    runtimeOnly("androidx.camera:camera-extensions:1.4.2")
    // https://mvnrepository.com/artifact/androidx.camera/camera-camera2
    runtimeOnly("androidx.camera:camera-camera2:1.4.2")
}