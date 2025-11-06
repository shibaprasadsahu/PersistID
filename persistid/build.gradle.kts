import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    id("maven-publish")
}

kotlin {
    // Android target
    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_17)
                }
            }
        }
        publishLibraryVariants("release")
    }

    // iOS targets
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "PersistId"
            isStatic = true
        }
    }

    sourceSets {
        // Common source set
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        // Android source set
        androidMain.dependencies {
            implementation(libs.androidx.lifecycle.runtime.ktx)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.kotlinx.coroutines.play.services)
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.androidx.work.runtime.ktx)
            implementation(libs.play.services.auth.blockstore)
            implementation(libs.core.ktx)

            // KVault for secure storage
            implementation(libs.kvault)
        }

        // iOS source set
        iosMain.dependencies {
            // KVault for secure storage (Keychain on iOS)
            implementation(libs.kvault)
        }

        // Android test
        val androidUnitTest by getting {
            dependencies {
                implementation(libs.junit)
                implementation(libs.kotlin.test)
                implementation(libs.truth)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.turbine)
                implementation(libs.mockk)
                implementation(libs.robolectric)
                implementation(libs.androidx.core.ktx)
            }
        }
    }
}

android {
    namespace = "com.shibaprasadsahu.persistid"
    compileSdk = 36

    defaultConfig {
        minSdk = 21
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Maven Publishing for JitPack
afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = "com.github.shibaprasadsahu"
                artifactId = "persistid"
                version = "0.2-alpha01"

                pom {
                    name.set("PersistId")
                    description.set("Modern Kotlin Multiplatform library for persistent device identifiers (Android + iOS)")
                    url.set("https://github.com/shibaprasadsahu/PersistID")

                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }

                    scm {
                        connection.set("scm:git:git://github.com/shibaprasadsahu/PersistID.git")
                        developerConnection.set("scm:git:ssh://github.com/shibaprasadsahu/PersistID.git")
                        url.set("https://github.com/shibaprasadsahu/PersistID")
                    }
                }
            }
        }
    }
}
