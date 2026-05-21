plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.hilt.android)
  alias(libs.plugins.google.services)
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
  implementation("com.google.mlkit:language-id:17.0.6")

  // Hilt — inyección de dependencias
  implementation(libs.hilt.android)
  annotationProcessor(libs.hilt.compiler)

  // ViewModel + LiveData
  implementation(libs.lifecycle.viewmodel)
  implementation(libs.lifecycle.livedata)

  // Firebase
  implementation(platform(libs.firebase.bom))
  implementation(libs.firebase.auth)
  implementation(libs.firebase.firestore)
  implementation(libs.firebase.storage)
  implementation(libs.firebase.functions)
  implementation(libs.firebase.ui.firestore)

  // Inicio de sesión con Google
  implementation(libs.play.services.auth)

  implementation(libs.flexbox)

//UNIT TESTS (JVM local)
  testImplementation("junit:junit:4.13.2")
  testImplementation("org.mockito:mockito-core:5.11.0")
  testImplementation("androidx.arch.core:core-testing:2.2.0")

//INSTRUMENTED TESTS (emulador)
  androidTestImplementation("androidx.test.ext:junit:1.2.1")
  androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
  androidTestImplementation("androidx.test:rules:1.6.1")
  androidTestImplementation("androidx.test:runner:1.6.2")
  androidTestImplementation("androidx.navigation:navigation-testing:2.8.9")
  androidTestImplementation("androidx.fragment:fragment-testing:1.8.5")
  androidTestImplementation("org.mockito:mockito-android:5.11.0")

  // Imagen circular (perfil)
  implementation(libs.circleimageview)
}
