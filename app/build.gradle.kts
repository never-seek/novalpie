import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Release signing is configured out of band so no key material lands in the repo.
// Create signing.properties (gitignored) with storeFile/storePassword/keyAlias/keyPassword.
// Without it, release builds stay unsigned exactly as they were before.
val signingProps = Properties().apply {
    val file = rootProject.file("signing.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val hasSigningConfig = signingProps.getProperty("storeFile") != null

android {
    namespace = "com.novalpie.nativeapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.novalpie.app"
        minSdk = 23
        targetSdk = 35
        versionCode = 2026090101
        versionName = "2.0.0-native-beta6"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    if (hasSigningConfig) {
        signingConfigs {
            create("release") {
                storeFile = rootProject.file(signingProps.getProperty("storeFile"))
                storePassword = signingProps.getProperty("storePassword")
                keyAlias = signingProps.getProperty("keyAlias")
                keyPassword = signingProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            // Was minifyEnabled false, which shipped release builds unshrunk and
            // unobfuscated. R8 is now on; see proguard-rules.pro for the reflection
            // surface that has to be kept (org.json, OkHttp, Coil, the WebView bridge).
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (hasSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    lint {
        warningsAsErrors = false
        abortOnError = true
        // Deliberately no baseline file: a fresh `lintVital` run reports zero fatal
        // issues, so a baseline would only abort the first build and then silently
        // absorb whatever regressions came later.
        // This app is zh-CN only by design, so translation completeness is not a defect.
        // The stable date-based version code is deliberately monotonic but close to Int.MAX_VALUE.
        disable += setOf("MissingTranslation", "ExtraTranslation", "HighAppVersionCode")
    }

    packaging {
        resources.excludes += setOf(
            "/META-INF/{AL2.0,LGPL2.1}",
            "/META-INF/DEPENDENCIES",
            "/META-INF/LICENSE*",
        )
    }
}

// Robolectric downloads framework jars lazily at test runtime. Its default Maven
// cache follows the user-profile junction on this machine, which is not writable
// in the project sandbox. Keep the test-only cache alongside build outputs so
// every Debug/Release unit-test process has a stable, project-local location.
tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
    systemProperty(
        "maven.repo.local",
        rootProject.layout.buildDirectory.dir("robolectric-m2").get().asFile.absolutePath,
    )
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.splashscreen)
    implementation(libs.androidx.webkit)

    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.re2j)

    testImplementation(libs.junit)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.junit)
}
