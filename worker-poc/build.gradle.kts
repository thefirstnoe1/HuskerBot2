plugins {
    kotlin("multiplatform") version "1.9.22"
}

repositories {
    mavenCentral()
}

kotlin {
    @OptIn(org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl::class)
    wasmJs {
        binaries.executable()
        nodejs()
    }
    
    sourceSets {
        val wasmJsMain by getting {
            dependencies {
                // Removed coroutines for now - not yet supported in Kotlin/Wasm
            }
        }
    }
}