import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
    id("com.android.library")   // Step 3b: Android-Target für :game
}

android {
    namespace = "game"
    compileSdk = 34
    defaultConfig { minSdk = 24 }
}

// :game is the KorGE rendering module. It depends on the pure :core logic
// and uses KorGE (the chosen engine) for rendering. KorGE is consumed as a
// plain library (not via the KorGE Gradle plugin) so the Kotlin plugin version
// stays consistent across all modules. Headless CI can compile this; opening
// the GL window is manual/local.
kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)   // Android → Dex, kein JVM_21-Zwang
        }
    }

    jvm("desktop") {
        compilerOptions {
            // KorGE 6.0.0 publishes its JVM artifacts (incl. inline functions
            // like container()/image()/sceneContainer()/changeTo()) as JVM
            // target 21 bytecode. Inlining that into a JVM 17 target fails, so
            // :game desktop targets 21. Android uses Dex (no JVM target clash).
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":core"))
                implementation("com.soywiz.korge:korge:6.0.0")
            }
        }
        val desktopMain by getting { /* Main.kt + Hd2dStage.kt live here */ }
        val androidMain by getting { /* KorGE inherited from commonMain */ }
    }
}

// Desktop runtime classpath helper. resourcesVfs resolves via the classpath, so
// the repo root is added (assets/ live there, referenced as "assets/...").
fun JavaExec.desktopRuntime() {
    val c = kotlin.targets.getByName("desktop").compilations.getByName("main")
    dependsOn(c.compileTaskProvider)
    classpath = files(c.output.allOutputs) + c.runtimeDependencyFiles + files(rootProject.projectDir)
    workingDir = rootProject.projectDir
}

// Render the real game scenes to PNGs under build/screenshots/ via KorGE's
// headless offscreen GL renderer (mesa EGL surfaceless — no window needed).
// Requires: libegl1 libegl-mesa0 libgl1-mesa-dri (see .devcontainer / scripts/setup-gl.sh).
tasks.register<JavaExec>("screenshot") {
    desktopRuntime()
    mainClass.set("game.ScreenshotHarnessKt")
    environment("EGL_PLATFORM", "surfaceless")
    environment("LIBGL_ALWAYS_SOFTWARE", "1")
}

// Launch the actual game window (manual/local; needs a real or virtual display).
tasks.register<JavaExec>("run") {
    desktopRuntime()
    mainClass.set("game.MainKt")
}
