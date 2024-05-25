plugins {
    kotlin("jvm")
    kotlin("kapt")
}

kotlin {
    jvmToolchain(17)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
    withSourcesJar()
}

dependencies {
    implementation(libs.google.auto.service)
    kapt(libs.google.auto.service)
    implementation(libs.ksp.api)
    implementation(libs.swiftpoet)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.metadata)
    implementation(libs.kotlinpoet.ksp)
    implementation(libs.kotlinx.serialization.json)
}

kapt {
    correctErrorTypes = true
    includeCompileClasspath = false
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {
                name.set("flutter-kmp-ksp")
                description.set("Annotation processor for flutter-kmp")
            }
        }
    }
}
