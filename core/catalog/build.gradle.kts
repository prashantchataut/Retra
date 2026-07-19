plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:rom"))
    implementation(project(":core:download"))
    testImplementation(kotlin("test-junit"))
    testImplementation(libs.junit)
}
