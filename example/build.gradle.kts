plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinSerialization)
}

val flutterKmpVersion = "0.1.0-rc.0"

kotlin {
    targetHierarchy.default()
    jvm()
    androidTarget {
        publishLibraryVariants("release")
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val androidMain by getting {
            dependencies {
                compileOnly("de.voize:flutter-kmp-stubs:$flutterKmpVersion")
            }
        }
        val commonMain by getting {
            dependencies {
                implementation("de.voize:flutter-kmp:$flutterKmpVersion")
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
}

android {
    namespace = "com.example.flutterkmp"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}

dependencies {
    add("kspCommonMainMetadata", "de.voize:flutter-kmp-ksp:$flutterKmpVersion")
    add("kspAndroid", "de.voize:flutter-kmp-ksp:$flutterKmpVersion")
    add("kspIosX64", "de.voize:flutter-kmp-ksp:$flutterKmpVersion")
    add("kspIosArm64", "de.voize:flutter-kmp-ksp:$flutterKmpVersion")
    add("kspIosSimulatorArm64", "de.voize:flutter-kmp-ksp:$flutterKmpVersion")
}

tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>>().configureEach {
    if (name != "kspCommonMainKotlinMetadata") {
        dependsOn("kspCommonMainKotlinMetadata")
    } else {
        finalizedBy("copyGeneratedDartFiles")
    }
}

tasks.register<Copy>("copyGeneratedDartFiles") {
    dependsOn("kspCommonMainKotlinMetadata")
    from("build/generated/ksp/metadata/commonMain/resources/flutterkmp")
    into("flutter/lib")
}
