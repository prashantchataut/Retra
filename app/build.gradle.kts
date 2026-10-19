plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "app.retra"
    compileSdk {
        version = release(37) { minorApiLevel = 0 }
    }

    defaultConfig {
        applicationId = "app.retra.emulator"
        minSdk = 26
        targetSdk = 37
        versionCode = 40
        versionName = "4.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Signed with the debug key unless a release keystore is configured, so CI artifacts install.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    androidResources {
        noCompress += listOf("gba")
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all { test ->
                test.maxHeapSize = "2g"
                test.systemProperty("robolectric.graphicsMode", "NATIVE")
                test.systemProperty("robolectric.pixelCopyRenderMode", "hardware")
            }
        }
    }
}

/** Copies the catalog manifest (repo root `catalog/`) into the APK assets as the offline fallback. */
abstract class CopyCatalogManifest : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val manifest: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun copy() {
        val dir = outputDir.get().asFile.resolve("catalog")
        dir.mkdirs()
        manifest.get().asFile.copyTo(dir.resolve("manifest.json"), overwrite = true)
    }
}

val copyCatalogManifest = tasks.register<CopyCatalogManifest>("copyCatalogManifest") {
    manifest.set(rootProject.layout.projectDirectory.file("catalog/manifest.json"))
}

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(copyCatalogManifest, CopyCatalogManifest::outputDir)
    }
}

roborazzi {
    outputDir.set(layout.projectDirectory.dir("src/test/screenshots"))
}

dependencies {
    implementation(project(":engine"))
    implementation(project(":patcher"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.browser)
    implementation(libs.haze)
    implementation(libs.haze.blur)
    implementation(libs.haze.glass)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
