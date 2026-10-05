plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

kotlin {
    jvmToolchain(17)

    // Android target: the AGP KMP library plugin (com.android.kotlin.multiplatform.library).
    android {
        namespace = "com.example.summitread.shared"
        compileSdk = 36
        minSdk = 24

        // commonTest + androidHostTest run as plain JVM unit tests (:shared:testAndroidHostTest).
        withHostTest { }
    }

    // iOS targets. Apple targets cannot be compiled on Windows; they are declared here and built on a Mac
    // (GitHub Actions macOS runner, see docs/KMP-PLAN.md).
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "SummitReadShared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.datetime)
            api(libs.androidx.lifecycle.viewmodel)
            api(libs.room.runtime)
            api(libs.sqlite.bundled)
            api(libs.datastore.preferences.core)
            // Compose Multiplatform UI: design system (theme + ui/components). Screens still live in :app (phase B).
            api(libs.compose.mp.runtime)
            api(libs.compose.mp.ui.graphics)
            api(libs.compose.mp.foundation)
            api(libs.compose.mp.animation)
            api(libs.compose.mp.ui)
            api(compose.material3)
            api(libs.compose.mp.components.resources)
            api(libs.compose.icons.extended.kmp)
            api(libs.haze)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

// Room KMP: one schema directory, kept at the same logical path as before (<db class fqn>/<version>.json).
room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
}

compose.resources {
    packageOfResClass = "com.example.summitread.resources"
    generateResClass = always
}
