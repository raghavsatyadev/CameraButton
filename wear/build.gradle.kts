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
  compileSdk { version = release(36) }

  defaultConfig {
    applicationId = "io.github.raghavsatyadev.camerabutton"
    minSdk = 30
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"
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
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  useLibrary("wear-sdk")
  buildFeatures { compose = true }
}

dependencies {
  implementation(libs.play.services.wearable)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material)
  implementation(libs.androidx.compose.foundation)
  implementation(libs.androidx.wear.tooling.preview)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.core.splashscreen)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.tooling)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
}
