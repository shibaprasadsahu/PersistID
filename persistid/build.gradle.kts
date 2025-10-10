import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
}

android {
    namespace = "com.shibaprasadsahu.persistid"
    compileSdk {
        version = release(36)
    }

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
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
}

dependencies {
    // Lifecycle for lifecycle-aware callbacks
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // DataStore Preferences (async storage)
    implementation(libs.androidx.datastore.preferences)

    // WorkManager for background backup sync
    implementation(libs.androidx.work.runtime.ktx)

    // BlockStore (for cloud backup)
    implementation(libs.play.services.auth.blockstore)
    implementation(libs.core.ktx)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.core.ktx)
}


// Maven Publishing for JitPack
afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = "com.github.shibaprasadsahu"
                artifactId = "persistid"
                version = "0.1-alpha01"

                pom {
                    name.set("PersistId")
                    description.set("Modern Android library for persistent device identifiers with cloud backup")
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