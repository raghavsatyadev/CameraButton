import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
}

val secretProps =
  Properties().apply {
    rootProject.file("secret.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
  }

android {
  namespace = "io.github.raghavsatyadev.camerabutton"
  compileSdk { version = release(37) }

  defaultConfig {
    applicationId = "io.github.raghavsatyadev.camerabutton"
    minSdk = 30
    targetSdk = 37
    versionCode = 2
    versionName = "2.0.0"
  }

  signingConfigs {
    create("release") {
      secretProps.getProperty("storeFile")?.let { storeFile = rootProject.file("wear/$it") }
      storePassword = secretProps.getProperty("storePassword")
      keyAlias = secretProps.getProperty("keyAlias")
      keyPassword = secretProps.getProperty("keyPassword")
    }
  }

  buildTypes {
    release {
      signingConfig = signingConfigs.getByName("release")
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }
  buildFeatures { compose = true }
}

kotlin { jvmToolchain(21) }

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.wear.compose.material3)
  implementation(libs.androidx.wear.compose.foundation)
  implementation(libs.androidx.wear.compose.navigation3)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.wear.compose.ui.tooling)
  implementation(libs.androidx.wear.tooling.preview)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.service)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.core.splashscreen)
  implementation(libs.androidx.datastore.preferences)
  testImplementation(libs.junit)
  debugImplementation(libs.androidx.compose.ui.tooling)
}
