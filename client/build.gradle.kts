plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.paparazzi)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "NeoMudClient"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared"))

            // Compose Multiplatform (JetBrains-published artifacts)
            implementation(libs.compose.material3)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.ui.tooling.preview)

            // Navigation + Lifecycle (JetBrains multiplatform versions)
            implementation(libs.navigation.compose)
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.lifecycle.runtime.compose)

            // Networking
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.websockets)

            // Serialization
            implementation(libs.kotlinx.serialization.json)

            // Image loading (Coil 3 is multiplatform)
            implementation(libs.coil.compose)

            // Compose Multiplatform resources (embedded images, etc.)
            implementation(compose.components.resources)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        androidMain.dependencies {
            // AndroidX (Android-specific)
            implementation(libs.activity.compose)

            // Networking (Android-specific engine)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.kotlinx.coroutines.android)

            // Image loading (Android-specific network backend)
            implementation(libs.coil.network.okhttp)
        }

        val desktopMain by getting {
            dependencies {
                // Compose Desktop
                implementation(compose.desktop.currentOs)

                // Networking (JVM engine)
                implementation(libs.ktor.client.cio)

                // Image loading (Ktor-based network backend for Desktop)
                implementation(libs.coil.network.ktor3)

                // Coroutines (Swing main dispatcher for Compose Desktop)
                implementation(libs.kotlinx.coroutines.swing)

                // Audio (JLayer — pure Java MP3 decoder, no native dependencies)
                implementation(libs.jlayer)

                // Logging (SLF4J/Logback — shared with server)
                implementation(libs.logback.classic)
            }
        }

        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain.get())
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
            dependencies {
                // Networking (Darwin engine for iOS)
                implementation(libs.ktor.client.darwin)

                // Image loading (Ktor-based network backend for iOS)
                implementation(libs.coil.network.ktor3)
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.robolectric)
                implementation(libs.compose.ui.test)
                implementation(libs.compose.ui.test.junit4)
                implementation(libs.coil.test)
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        val androidInstrumentedTest by getting {
            dependencies {
                implementation(libs.compose.ui.test)
                implementation(libs.compose.ui.test.junit4)
                implementation(libs.androidx.test.core)
                implementation(libs.androidx.test.runner)
                implementation(libs.androidx.test.ext.junit)
            }
        }
    }
}

android {
    namespace = "com.neomud.client"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.neomud.client"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

// Android-specific debug/test dependencies that need build-type configurations
// (not expressible through KMP source sets)
dependencies {
    "debugImplementation"(libs.compose.ui.tooling)
    "debugImplementation"(libs.compose.ui.test.manifest)
}

compose.desktop {
    application {
        mainClass = "com.neomud.client.MainKt"
        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg
            )
            packageName = "NeoMud"
            packageVersion = "1.0.0"
            windows {
                menuGroup = "NeoMud"
                upgradeUuid = "5a3e4b2c-1d7f-4e8a-9b6c-3f2e1d0a8b7c"
            }
        }
    }
}

tasks.register<Exec>("startEmulator") {
    group = "emulator"
    description = "Start the Android emulator"
    val emulatorPath = "${System.getenv("LOCALAPPDATA")}\\Android\\Sdk\\emulator\\emulator.exe"
    commandLine(emulatorPath, "-avd", "Medium_Phone_API_35")
}
