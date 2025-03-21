import com.android.build.gradle.internal.api.ApkVariantOutputImpl
import com.android.build.gradle.tasks.PackageAndroidArtifact
import java.time.Instant

plugins {
    alias(libs.plugins.self.application)
    alias(libs.plugins.self.compose)
    alias(libs.plugins.self.hilt)
    alias(libs.plugins.self.room)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.lsplugin.resopt)
}

val baseVersionName = "1"
val devVersion = exec("git tag --contains HEAD").isEmpty()
val commitShaSuffix = commitSha.let { ".${it.substring(0, 7)}" }
val devSuffix = if (devVersion) ".dev" else ""

android {
    namespace = "me.garfieldhan.attestation"

    defaultConfig {
        applicationId = namespace
        versionName = "${baseVersionName}${commitShaSuffix}${devSuffix}"
        versionCode = commitCount
    }

    val releaseSigning = if (project.hasReleaseKeyStore) {
        signingConfigs.create("release") {
            storeFile = project.releaseKeyStore
            storePassword = project.releaseKeyStorePassword
            keyAlias = project.releaseKeyAlias
            keyPassword = project.releaseKeyPassword
            enableV3Signing = true
            enableV4Signing = true
        }
    } else {
        signingConfigs.getByName("debug")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        all {
            signingConfig = releaseSigning
            buildConfigField("boolean", "DEV_VERSION", devVersion.toString())
            buildConfigField("long", "BUILD_TIME", Instant.now().toEpochMilli().toString())
        }
    }

    buildFeatures {
        buildConfig = true
    }

    packaging.resources.excludes += "**"
    dependenciesInfo.includeInApk = false

    applicationVariants.configureEach {
        outputs.configureEach {
            if (this is ApkVariantOutputImpl) {
                outputFileName = "HanAttestationDetectDemo-${versionName}-${versionCode}-${name}.apk"
            }
        }
    }

    // https://stackoverflow.com/a/77745844
    tasks.withType<PackageAndroidArtifact> {
        doFirst { appMetadata.asFile.orNull?.writeText("") }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.compose.ui.util)
    implementation(libs.androidx.core)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.datastore.core)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.viewmodel.savedstate)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.protobuf)
    implementation(libs.timber)
    implementation(libs.org.bouncycastle.bcprov)
}