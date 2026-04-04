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
            implementation(libs.ktor.client.content.negotiation)

            // Serialization
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.serialization.kotlinx.json)

            // Image loading (Coil 3 is multiplatform)
            implementation(libs.coil.compose)

            // Compose Multiplatform resources (embedded images, etc.)
            implementation(compose.components.resources)

            // Material Icons Extended (cross-platform vector icons for UI)
            implementation(compose.materialIconsExtended)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.compose.ui.test)
            implementation(libs.coil.test)
            implementation(libs.kotlinx.coroutines.test)
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

        val iosX64Test by getting
        val iosArm64Test by getting
        val iosSimulatorArm64Test by getting
        val iosTest by creating {
            dependsOn(commonTest.get())
            iosX64Test.dependsOn(this)
            iosArm64Test.dependsOn(this)
            iosSimulatorArm64Test.dependsOn(this)
        }

        val androidUnitTest by getting {
            dependencies {
                implementation(libs.robolectric)
                implementation(libs.compose.ui.test.junit4)
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

    flavorDimensions += "environment"

    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            buildConfigField("boolean", "SHOW_SERVER_CONFIG", "true")
            buildConfigField("boolean", "USE_TLS", "false")
            buildConfigField("String", "DEFAULT_HOST", "\"10.0.2.2\"")
            buildConfigField("int", "DEFAULT_PORT", "8080")
            buildConfigField("String", "PLATFORM_API_URL", "\"http://10.0.2.2:3002/api/v1\"")
        }
        create("prod") {
            dimension = "environment"
            buildConfigField("boolean", "SHOW_SERVER_CONFIG", "false")
            buildConfigField("boolean", "USE_TLS", "true")
            buildConfigField("String", "DEFAULT_HOST", "\"play.neomud.com\"")
            buildConfigField("int", "DEFAULT_PORT", "443")
            buildConfigField("String", "PLATFORM_API_URL", "\"https://api.neomud.com/api/v1\"")
        }
    }

    buildFeatures {
        buildConfig = true
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
    val androidHome = System.getenv("ANDROID_HOME")
        ?: System.getenv("ANDROID_SDK_ROOT")
        ?: if (org.gradle.internal.os.OperatingSystem.current().isMacOsX)
            "${System.getProperty("user.home")}/Library/Android/sdk"
        else
            "${System.getenv("LOCALAPPDATA")}/Android/Sdk"
    val emulatorBin = if (org.gradle.internal.os.OperatingSystem.current().isWindows) "emulator.exe" else "emulator"
    commandLine("$androidHome/emulator/$emulatorBin", "-avd", "Medium_Phone_API_35")
}

tasks.register<Exec>("startSimulator") {
    group = "simulator"
    description = "Boot the iOS Simulator and open the Simulator app"
    onlyIf { org.gradle.internal.os.OperatingSystem.current().isMacOsX }
    commandLine("open", "-a", "Simulator")
}

tasks.register<Exec>("installIosClient") {
    group = "simulator"
    description = "Build and install the iOS client on the Simulator"
    onlyIf { org.gradle.internal.os.OperatingSystem.current().isMacOsX }
    workingDir(project.rootDir.resolve("iosApp"))
    commandLine("bash", "-c", """
        set -euo pipefail
        UDID=$(xcrun simctl list devices available -j | python3 -c "
import json, sys
devices = json.load(sys.stdin)['devices']
for runtime, devs in devices.items():
    for d in devs:
        if 'iPhone 17' in d['name']:
            print(d['udid']); sys.exit(0)
sys.exit(1)
")
        echo "Found iPhone 17 simulator: ${'$'}UDID"
        xcrun simctl boot "${'$'}UDID" 2>/dev/null || true
        open -a Simulator
        xcodebuild \
            -project NeoMud.xcodeproj \
            -scheme NeoMud \
            -configuration Debug \
            -destination "id=${'$'}UDID" \
            -derivedDataPath build \
            clean build
        xcrun simctl install "${'$'}UDID" build/Build/Products/Debug-iphonesimulator/NeoMud.app
        xcrun simctl launch "${'$'}UDID" com.neomud.NeoMud
        echo "NeoMud installed and launched on iPhone 17 simulator"
    """.trimIndent())
}
