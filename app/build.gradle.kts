import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.androidx.baselineprofile)
    jacoco
}

val localProps = Properties().apply {
    rootProject.file("local.properties").inputStream().use { load(it) }
}

fun requireLocalProp(key: String): String =
    checkNotNull(localProps.getProperty(key)) {
        "$key is missing from local.properties"
    }

android {
    namespace = "com.ruizurraca.luziatestdavid"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.ruizurraca.luziatestdavid"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        // Benchmark build type: mirrors release (R8 + resource-shrink)
        // but uses the debug signing config so the APK is installable,
        // and is not debuggable so ART can AOT-optimize the baseline
        // profile. Only used by the :baseline-profile test module.
        create("benchmark") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += "release"
            isDebuggable = false
        }
    }

    flavorDimensions += "environment"
    productFlavors {
        create("staging") {
            dimension = "environment"
            applicationIdSuffix = ".staging"
            buildConfigField(
                "String",
                "BASE_URL",
                "\"${requireLocalProp("BASE_URL_STAGING")}\""
            )
        }
        create("production") {
            dimension = "environment"
            buildConfigField(
                "String",
                "BASE_URL",
                "\"${requireLocalProp("BASE_URL_PRODUCTION")}\""
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            it.useJUnitPlatform()
            // Intentionally NOT setting maxParallelForks — measured on 2026-04-19
            // with 6 forks (half of 12 cores), wall-clock regressed 29 s → 48 s.
            // Fork-JVM startup (~3-5 s × 6) + Robolectric per-fork shadow-classloader
            // init exceed the parallelism gain at a 442-test / ~29 s suite size.
            // Revisit when serial runtime exceeds ~2-3 min. ROADMAP 10.5.B.
        }
    }

    lint {
        baseline = file("lint-baseline.xml")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

jacoco {
    toolVersion = "0.8.12"
}

// AGP 9 emits post-transform bytecode under intermediates/classes/<variant>/transformClassesWithAsm/dirs —
// that path is what the JaCoCo agent instrumented at test-time, so the report must point there.
tasks.register<JacocoReport>("jacocoStagingDebugCoverageReport") {
    group = "verification"
    description = "Generate Jacoco coverage report for the stagingDebug unit-test run."
    dependsOn("testStagingDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    val coverageExcludes = listOf(
        "**/R.class",
        "**/R\$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*",
        "**/*\$WhenMappings.*",
        "**/*_Factory*.*",
        "**/*_MembersInjector.*",
        "**/*_HiltModules*.*",
        "**/Hilt_*.*",
        "**/Dagger*Component*.*",
        "**/*_GeneratedInjector.*",
        "**/*_Impl.*",
        "**/*Impl_*.*",
        "**/ComposableSingletons*",
    )

    classDirectories.setFrom(
        fileTree(layout.buildDirectory.dir("intermediates/classes/stagingDebug/transformStagingDebugClassesWithAsm/dirs")) {
            exclude(coverageExcludes)
        }
    )

    sourceDirectories.setFrom(files("${project.projectDir}/src/main/java"))

    executionData.setFrom(
        fileTree(layout.buildDirectory) {
            include("outputs/unit_test_code_coverage/stagingDebugUnitTest/testStagingDebugUnitTest.exec")
        }
    )
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(libs.junit.vintage.engine)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.robolectric)
    testImplementation(libs.hilt.android.testing)
    kspTest(libs.hilt.compiler)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Consumes the generated baseline profile from the :baseline-profile
    // producer module at build time; the plugin merges it into the APK.
    "baselineProfile"(project(":baseline-profile"))
}