import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
}

android {
    namespace = "de.voize.flutterkmp"
    compileSdk = 36

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
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
        publishLibraryVariants("release")
    }

    fun KotlinNativeTarget.configureFlutterInterop() {
        val main by compilations.getting {
            val flutter by cinterops.creating {
                includeDirs("src/nativeInterop/cinterop/")
                packageName("flutter")
            }
        }
    }

    iosX64 { configureFlutterInterop() }
    iosArm64 { configureFlutterInterop() }
    iosSimulatorArm64 { configureFlutterInterop() }
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

        iosTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        all {
            languageSettings.optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
            languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
        }
    }
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
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
