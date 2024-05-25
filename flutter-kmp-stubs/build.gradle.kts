plugins {
    kotlin("jvm")
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

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {
                name.set("flutter-kmp-stubs")
                description.set("Stubs that help compiling Android code by Flutter KMP")
            }
        }
    }
}
