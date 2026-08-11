plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:achievements"))
    testImplementation(kotlin("test-junit"))
    testImplementation(libs.junit)
}
