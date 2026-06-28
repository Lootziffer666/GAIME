import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
}

// :game is the KorGE rendering module (Step 2 of the KorGE 2.5D migration —
// see .kiro/steering/rendering-engine.md). It depends on the pure :core logic
// and uses KorGE (the chosen engine) for rendering. KorGE is consumed as a
// plain library on the project's existing Kotlin 2.1.21 / jvm("desktop") setup
// (rather than via the opinionated KorGE Gradle plugin) so the Kotlin plugin
// version stays consistent across all modules and the existing build is never
// at risk. Headless CI can compile this; opening the GL window is manual/local.
kotlin {
    jvm("desktop") {
        compilerOptions {
            // KorGE 6.0.0 publishes its JVM artifacts (incl. inline functions
            // like container()/image()/sceneContainer()/changeTo()) as JVM
            // target 21 bytecode. Inlining that into a JVM 17 target fails, so
            // :game targets 21. This is desktop-only (no Android target), so it
            // does not affect :core/:composeApp which stay at 17 for Android.
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    sourceSets {
        val desktopMain by getting {
            dependencies {
                implementation(project(":core"))
                implementation("com.soywiz.korge:korge:6.0.0")
            }
        }
    }
}
