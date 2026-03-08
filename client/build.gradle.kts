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

                // Audio (JavaFX Media — need platform-classified JARs)
                val javafxPlatform = when {
                    org.gradle.internal.os.OperatingSystem.current().isWindows -> "win"
                    org.gradle.internal.os.OperatingSystem.current().isMacOsX -> "mac"
                    else -> "linux"
                }
                val javafxVersion = libs.versions.javafx.get()
                for (module in listOf("base", "graphics", "media", "swing")) {
                    implementation("org.openjfx:javafx-$module:$javafxVersion:$javafxPlatform")
                }

                // Logging (SLF4J/Logback — shared with server)
                implementation(libs.logback.classic)
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
