plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "app.retra.emulation.nativecore"
    compileSdk = 37
    ndkVersion = "28.2.13676358"

    defaultConfig {
        minSdk = 26
        externalNativeBuild {
            cmake {
                cppFlags += listOf("-std=c++20", "-Wall", "-Wextra", "-Werror")
            }
        }
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:emulation"))
    implementation(project(":emulation:api"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
}

val fetchMgbaSource by tasks.registering(Exec::class) {
    group = "retra emulation"
    description = "Download and SHA-256 verify the pinned mGBA 0.10.5 DFSG source archive."
    workingDir(rootProject.projectDir)
    commandLine("sh", "scripts/fetch-mgba-archive.sh")
}

val buildMgbaCore by tasks.registering(Exec::class) {
    group = "retra emulation"
    description = "Build and stage mGBA libretro shared libraries for Android ABIs."
    workingDir(rootProject.projectDir)
    doFirst {
        val source = rootProject.file("third_party/mgba/upstream/CMakeLists.txt")
        require(source.isFile) {
            "Pinned mGBA source is missing. Run :emulation:native:fetchMgbaSource first."
        }
        require(!System.getenv("ANDROID_NDK_HOME").isNullOrBlank()) {
            "ANDROID_NDK_HOME must point to a reviewed Android NDK."
        }
    }
    commandLine("sh", "scripts/build-mgba-libretro-android.sh")
}
