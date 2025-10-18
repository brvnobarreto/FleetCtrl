plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
  id("kotlin-kapt")
  id("dagger.hilt.android.plugin")
  id("com.google.gms.google-services")
}

android {
  namespace = "dev.barreto.fleetctrl"
  compileSdk = 36

  defaultConfig {
    applicationId = "dev.barreto.fleetctrl"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    isCoreLibraryDesugaringEnabled = true
  }
  kotlinOptions {
    jvmTarget = "17"
  }
  buildFeatures {
    compose = true
  }

  packagingOptions {
    resources {
      excludes += setOf(
        "META-INF/LICENSE.md",
        "META-INF/LICENSE-notice.md",
        "META-INF/DEPENDENCIES",
        "META-INF/LICENSE",
        "META-INF/NOTICE"
      )
    }
  }
}

dependencies {

  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  // Pull-to-refresh (Compose Material APIs live in material)
  implementation("androidx.compose.material:material")
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Firebase BOM (gerencia versões automaticamente)
  implementation(platform("com.google.firebase:firebase-bom:32.7.0"))

  // Firebase Auth + Google Sign In
  implementation("com.google.firebase:firebase-auth-ktx")
  implementation("com.google.android.gms:play-services-auth:20.7.0")

  // Firestore database
  implementation("com.google.firebase:firebase-firestore-ktx")
  // Firebase Cloud Messaging
  implementation("com.google.firebase:firebase-messaging-ktx")
  // App Check
  implementation("com.google.firebase:firebase-appcheck-ktx")
  implementation("com.google.firebase:firebase-appcheck-playintegrity")

    // Storage database
    implementation("com.google.firebase:firebase-storage-ktx")
    
    // Stetho para debug do banco de dados
    debugImplementation("com.facebook.stetho:stetho:1.6.0")
    debugImplementation("com.facebook.stetho:stetho-okhttp3:1.6.0")

  // Room Database
  implementation("androidx.room:room-runtime:2.6.1")
  implementation("androidx.room:room-ktx:2.6.1")
  kapt("androidx.room:room-compiler:2.6.1")
  
  // Coroutines
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
  
  // Hilt Dependency Injection
  implementation("com.google.dagger:hilt-android:2.48")
  kapt("com.google.dagger:hilt-compiler:2.48")
  // Hilt Navigation for Compose ViewModels
  implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
  
  // DataStore
  implementation("androidx.datastore:datastore-preferences:1.0.0")
  implementation("androidx.datastore:datastore-preferences-core:1.0.0")

  // WorkManager fallback sync
  implementation("androidx.work:work-runtime-ktx:2.9.0")
  implementation("androidx.hilt:hilt-work:1.2.0")
  kapt("androidx.hilt:hilt-compiler:1.2.0")
  
  // Coil para carregamento de imagens
  implementation("io.coil-kt:coil-compose:2.5.0")
  implementation("io.coil-kt:coil-video:2.5.0")
  
  // Desugaring para LocalDateTime
  coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
  // Test dependencies
  testImplementation(libs.junit)
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
  testImplementation("io.mockk:mockk:1.13.8")
  testImplementation("app.cash.turbine:turbine:1.0.0")
  testImplementation("androidx.arch.core:core-testing:2.2.0")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.9.10")
  
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation("io.mockk:mockk-android:1.13.8")
  
  // Database testing dependencies
  androidTestImplementation("androidx.room:room-testing:2.6.1")
  androidTestImplementation("androidx.test:runner:1.5.2")
  androidTestImplementation("androidx.test:rules:1.5.0")
  androidTestImplementation("androidx.test.ext:junit:1.1.5")
  
  // Additional testing dependencies
  testImplementation("androidx.arch.core:core-testing:2.2.0")
  testImplementation("androidx.test:core:1.5.0")
  testImplementation("androidx.test:rules:1.5.0")
  testImplementation("androidx.test:runner:1.5.2")
  debugImplementation(libs.androidx.compose.ui.tooling)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
}
