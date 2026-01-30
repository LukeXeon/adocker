// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
}

// Initialize git submodules before build
tasks.register("initSubmodules") {
    description = "Initialize and update git submodules"
    group = "build setup"
    doLast {
        exec {
            commandLine("git", "submodule", "update", "--init", "--recursive")
        }
    }
}

// Make preBuild depend on submodule initialization for all projects
subprojects {
    tasks.configureEach {
        if (name == "preBuild") {
            dependsOn(rootProject.tasks.named("initSubmodules"))
        }
    }
}
