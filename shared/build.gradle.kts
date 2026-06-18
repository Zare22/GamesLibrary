import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.kotlinSerialization)
}

// Generates IgdbCredentials.kt from local.properties (or IGDB_CLIENT_ID/SECRET env vars). The output
// lives under build/ so the secrets are never tracked; empty when absent, so builds still compile.
val generateIgdbCredentials by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/igdb/kotlin")
    outputs.dir(outputDir)
    val props = Properties()
    val localProps = rootProject.file("local.properties")
    if (localProps.exists()) localProps.inputStream().use { props.load(it) }
    val clientId = props.getProperty("igdb.clientId") ?: System.getenv("IGDB_CLIENT_ID") ?: ""
    val clientSecret = props.getProperty("igdb.clientSecret") ?: System.getenv("IGDB_CLIENT_SECRET") ?: ""
    inputs.property("clientId", clientId)
    inputs.property("clientSecret", clientSecret)
    doLast {
        fun esc(value: String) = value.replace("\\", "\\\\").replace("\"", "\\\"").replace("$", "\\$")
        val pkgDir = outputDir.get().asFile.resolve("hr/kotwave/gameslibrary/igdb")
        pkgDir.mkdirs()
        pkgDir.resolve("IgdbCredentials.kt").writeText(
            """
            |package hr.kotwave.gameslibrary.igdb
            |
            |internal object IgdbCredentials {
            |    const val CLIENT_ID = "${esc(clientId)}"
            |    const val CLIENT_SECRET = "${esc(clientSecret)}"
            |}
            |
            """.trimMargin(),
        )
    }
}

kotlin {
    // The Room-generated database constructor is an expect/actual object (still Beta).
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    jvm()

    sourceSets {
        commonMain {
            kotlin.srcDir(generateIgdbCredentials)
            dependencies {
                api(project.dependencies.platform(libs.koin.bom))
                api(libs.koin.core)
                implementation(libs.room.runtime)
                implementation(libs.sqlite.bundled)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.logging)
            }
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
        androidMain.dependencies {
            implementation(libs.koin.android)
            implementation(libs.ktor.client.okhttp)
        }
        val jvmMain by getting {
            dependencies {
                implementation(libs.ktor.client.okhttp)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}

android {
    namespace = "hr.kotwave.gameslibrary.shared"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspJvm", libs.room.compiler)
}
