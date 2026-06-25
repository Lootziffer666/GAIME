plugins {
    kotlin("multiplatform")
    id("com.android.library")
}

// :core holds the pure, renderer-agnostic game logic (bark pipeline, Questbook,
// combat, world, chapters, finale overload, state machine). No Compose, no
// KorGE, no platform audio. Shared by :composeApp today and the future :game
// (KorGE) module. See .kiro/steering/rendering-engine.md.
kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    jvm("desktop")

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

android {
    namespace = "com.aiundb.gaime.core"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
