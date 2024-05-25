plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    kotlin("native.cocoapods")
    id("com.android.library")
}

android {
    namespace = "de.voize.flutterkmp"
    compileSdk = 33

    defaultConfig {
        minSdk = 21
    }

    compileOptions {
        sourceCompatibility(JavaVersion.VERSION_1_8)
        targetCompatibility(JavaVersion.VERSION_1_8)
    }
}

kotlin {
    jvmToolchain(17)

    jvm()
    androidTarget {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
        publishLibraryVariants("release")
    }

    cocoapods {
        // this cocoapods dependency is needed for the iOS EventStreamHandler utils
        pod("Flutter")
        
        // we do not need to create a podspec file because the KMP projects that use this library 
        // must produce a podspec that contains the Flutter cocoapod dependency
        noPodspec()
        
        ios.deploymentTarget = "11.0"
    }


    iosX64()
    iosArm64()
    iosSimulatorArm64()
    wasmJs { nodejs() }

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
            }
        }

        androidMain {
            dependencies {
                compileOnly(project(":flutter-kmp-stubs"))
            }
        }

        all {
            languageSettings.optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
            languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
        }
    }
    targets.all {
        compilations.all {
            kotlinOptions {
                freeCompilerArgs += listOf("-Xexpect-actual-classes")
            }
        }
    }
}

publishing {
    publications.withType<MavenPublication> {
        pom {
            name.set("flutter-kmp")
            description.set("Create Flutter Plugin code from Kotlin Multiplatform")
        }
    }
}
