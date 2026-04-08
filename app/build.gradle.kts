plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.hilt.android)

}
android {
  namespace = "com.clank.app"
  compileSdk = 36

  buildFeatures {
    viewBinding = true
  }

  defaultConfig {
    applicationId = "com.clank.app"
    minSdk = 26
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
      )
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
}


dependencies {
  implementation(libs.androidx.appcompat)
  implementation(libs.material)
  implementation(libs.androidx.activity)
  implementation(libs.androidx.constraintlayout)

  // Navegación
  implementation(libs.androidx.navigation.fragment)
  implementation(libs.androidx.navigation.ui)

  // Carga de imágenes
  implementation(libs.glide)

  // Idiomas
  implementation("com.google.mlkit:translate:17.0.3")

  // Hilt — inyección de dependencias
  implementation(libs.hilt.android)
  implementation(libs.google.firebase.firestore)
  implementation(libs.google.firebase.auth)
  annotationProcessor(libs.hilt.compiler)

  // ViewModel + LiveData
  implementation(libs.lifecycle.viewmodel)
  implementation(libs.lifecycle.livedata)

  //Firebase
  implementation(platform(libs.firebase.bom))
  implementation(libs.firebase.auth)
  implementation(libs.firebase.firestore)
  implementation(libs.firebase.storage)

  // Tests
  testImplementation(libs.junit)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)


}
