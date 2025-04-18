plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinCocoapods)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinSerialization)
}

val flutterKmpVersion = "0.1.0-rc.6"

kotlin {
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

    applyDefaultHierarchyTemplate()

    cocoapods {
        name = "FlutterKmpExample"
        version = "0.1.0"

        framework {
            homepage = "https://github.com/voize-gmbh/flutter-kmp"
            summary = "Shared Kotlin code for flutter-kmp example"
            baseName = "flutterkmpexample"
        }
    }

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

        val iosX64Main by getting {
            kotlin.srcDir("build/generated/ksp/iosX64/iosX64Main/kotlin")
        }
        val iosArm64Main by getting {
            kotlin.srcDir("build/generated/ksp/iosArm64/iosArm64Main/kotlin")
        }
        val iosSimulatorArm64Main by getting {
            kotlin.srcDir("build/generated/ksp/iosSimulatorArm64/iosSimulatorArm64Main/kotlin")
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
