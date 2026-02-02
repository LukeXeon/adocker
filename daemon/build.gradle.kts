plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.github.andock.daemon"
    compileSdk = 36

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "com.github.andock.daemon.HiltTestRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        aidl = true
        buildConfig = true
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Fix for Kotlin 2.2.21 compatibility with Hilt
    // Explicitly add kotlin-metadata-jvm to support newer Kotlin versions
    ksp(libs.kotlin.metadata.jvm)

    // Networking
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Archive handling
    implementation(libs.commons.compress)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.room.paging)
    ksp(libs.room.compiler)

    // Paging 3
    implementation(libs.androidx.paging.runtime)

    // Auto
    implementation(libs.slf4j.api)
    compileOnly(libs.auto.service.annotations)
    kapt(libs.auto.service)

    // Timber Logging
    implementation(libs.timber)

    // Shizuku
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)

    // http4k
    api(libs.http4k.core)

    // Flow Redux
    implementation(libs.flowredux)

    // ACRA
    implementation(libs.acra.mail)
    implementation(libs.acra.toast)

    // Guava
    implementation(libs.kotlinx.coroutines.guava)

    // Stub
    compileOnly(project(":stub"))

    implementation(project(":startup"))

    implementation(project(":proot"))
//    implementation(project(":gpu"))

    // Ksp
    ksp(project(":startup-ksp"))

    // Leakcanary
    debugImplementation(libs.leakcanary.android)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.hilt.android.testing)
    androidTestImplementation(libs.kotlinx.coroutines.core)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    kspAndroidTest(libs.hilt.compiler)
}