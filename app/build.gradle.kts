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
    aaptOptions{
        noCompress += "pt"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        mlModelBinding = true
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
    implementation("org.tensorflow:tensorflow-lite:2.17.0")
    // https://mvnrepository.com/artifact/org.tensorflow/tensorflow-lite-support
    implementation("org.tensorflow:tensorflow-lite-support:0.5.0")
    // https://mvnrepository.com/artifact/org.tensorflow/tensorflow-lite-task-vision
    implementation("org.tensorflow:tensorflow-lite-task-vision:0.4.4")


    // https://mvnrepository.com/artifact/io.github.lucksiege/camerax
    implementation("io.github.lucksiege:camerax:v3.11.3"){
        exclude(group = "com.google.ai.edge.litert", module = "litert-api")
    }
    // https://mvnrepository.com/artifact/androidx.camera/camera-core
    implementation("androidx.camera:camera-core:1.4.2")
    // https://mvnrepository.com/artifact/androidx.camera/camera-lifecycle
    implementation("androidx.camera:camera-lifecycle:1.4.2")
    // https://mvnrepository.com/artifact/androidx.camera/camera-extensions
    implementation("androidx.camera:camera-extensions:1.4.2")
    // https://mvnrepository.com/artifact/androidx.camera/camera-camera2
    implementation("androidx.camera:camera-camera2:1.4.2")

    configurations.all {
        exclude(group= "com.google.ai.edge.litert", module= "litert-api")
        exclude (group= "com.google.ai.edge.litert", module= "litert-support-api")
    }

    // https://mvnrepository.com/artifact/org.pytorch/pytorch_android
    implementation("org.pytorch:pytorch_android:2.1.0")
    // https://mvnrepository.com/artifact/org.pytorch/pytorch_android_torchvision
    implementation("org.pytorch:pytorch_android_torchvision:2.1.0")

}