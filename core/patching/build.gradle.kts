plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core:rom"))
    testImplementation(kotlin("test-junit"))
    testImplementation(libs.junit)
}
