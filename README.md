# flutter-kmp

## Installation

Add the KSP gradle plugin to your multiplatform project's build.gradle.kts file, if you have subprojects, add it to the subproject's build.gradle.kts file.

```kotlin
// android/shared/build.gradle.kts

plugins {
    // from gradlePluginPortal()
    id("com.google.devtools.ksp") version "1.9.23-1.0.20"
}
```

Then add the `flutter-kmp` to the commonMain source set dependencies. Also add the generated common source set to the commonMain source set:

```kotlin
// android/shared/build.gradle.kts

val commonMain by getting {
    kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
    dependencies {
        // ...
        implementation("de.voize:flutter-kmp:<version>")
    }
}
```

Add `flutter-kmp-stubs` as `compileOnly` dependency to the androidMain source set:

```kotlin
// android/shared/build.gradle.kts

val androidMain by getting {
    dependencies {
        // ...
        compileOnly("de.voize:flutter-kmp-stubs:<version>")
    }
}
```

Then add `flutter-kmp-ksp` to the KSP configurations:

```kotlin
// android/shared/build.gradle.kts

dependencies {
    add("kspCommonMainMetadata", "de.voize:flutter-kmp-ksp:<version>")
    add("kspAndroid", "de.voize:flutter-kmp-ksp:<version>")
    add("kspIosX64", "de.voize:flutter-kmp-ksp:<version>")
    add("kspIosArm64", "de.voize:flutter-kmp-ksp:<version>")
    // (if needed) add("kspIosSimulatorArm64", "de.voize:flutter-kmp-ksp:<version>")
}
```

Configure the KSP task dependencies and copy the generated Dart files to your flutter plugin:

```kotlin
// android/shared/build.gradle.kts

tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>>().configureEach {
    if(name != "kspCommonMainKotlinMetadata") {
        dependsOn("kspCommonMainKotlinMetadata")
    } else {
        finalizedBy("copyGeneratedDartFiles")
    }
}

tasks.register<Copy>("copyGeneratedDartFiles") {
    dependsOn("kspCommonMainKotlinMetadata")
    from("build/generated/ksp/metadata/commonMain/resources/flutterkmp")
    into("path/to/flutter/lib/generated")
}
```

Register the generated Android module classes (ending with `...Android.kt`) in your Flutter plugin Android class:

```kotlin
class MyFlutterPlugin: FlutterPlugin {
    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        MyFlutterModuleAndroid()
        // ...
    }

    ...
}
```

The generated Dart code uses the `json_serializable` package.
Add the following dependencies to your Flutter plugin:

```
flutter pub add json_annotation dev:build_runner dev:json_serializable
```

Then after generating Dart data classes make sure to the following command to generate the serialization code:

```
dart run build_runner build --delete-conflicting-outputs
```
