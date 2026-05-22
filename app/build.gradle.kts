plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.hilt.android)
  alias(libs.plugins.google.services)
  alias(libs.plugins.allure)
}

val allureKotlinVersion = "2.4.0"

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

    testInstrumentationRunner = "com.clank.app.HiltTestRunner"
  }
  testOptions {
    execution = "ANDROIDX_TEST_ORCHESTRATOR"
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

allure {
  version.set("2.24.0")
}

dependencies {
  implementation(libs.androidx.appcompat)
  implementation(libs.material)
  implementation(libs.androidx.activity)
  implementation(libs.androidx.constraintlayout)

  implementation(libs.androidx.navigation.fragment)
  implementation(libs.androidx.navigation.ui)

  implementation(libs.glide)

  implementation("com.google.mlkit:translate:17.0.3")
  implementation("com.google.mlkit:language-id:17.0.6")

  implementation(libs.hilt.android)
  implementation(libs.androidx.lifecycle.viewmodel)
  annotationProcessor(libs.hilt.compiler)

  implementation(libs.lifecycle.viewmodel)
  implementation(libs.lifecycle.livedata)

  implementation(platform(libs.firebase.bom))
  implementation(libs.firebase.auth)
  implementation(libs.firebase.firestore)
  implementation(libs.firebase.storage)
  implementation(libs.firebase.functions)
  implementation(libs.firebase.ui.firestore)

  implementation(libs.play.services.auth)
  implementation(libs.flexbox)
  implementation(libs.circleimageview)

  // UNIT TESTS
  testImplementation("junit:junit:4.13.2")
  testImplementation("org.mockito:mockito-core:5.11.0")
  testImplementation("androidx.arch.core:core-testing:2.2.0")
  testImplementation("io.qameta.allure:allure-kotlin-model:$allureKotlinVersion")
  testImplementation("io.qameta.allure:allure-kotlin-commons:$allureKotlinVersion")
  testImplementation("io.qameta.allure:allure-kotlin-junit4:$allureKotlinVersion")

  // INSTRUMENTED TESTS
  androidTestImplementation("androidx.test.ext:junit:1.2.1")
  androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
  androidTestImplementation("androidx.test:rules:1.6.1")
  androidTestImplementation("androidx.test:runner:1.6.2")
  androidTestImplementation("androidx.navigation:navigation-testing:2.8.9")
  androidTestImplementation("androidx.fragment:fragment-testing:1.8.5")
  androidTestImplementation("org.mockito:mockito-android:5.11.0")

  // Hilt en androidTest
  androidTestImplementation("com.google.dagger:hilt-android-testing:2.51")
  androidTestAnnotationProcessor("com.google.dagger:hilt-android-compiler:2.51")

  // Allure en androidTest
  androidTestImplementation("io.qameta.allure:allure-kotlin-model:$allureKotlinVersion")
  androidTestImplementation("io.qameta.allure:allure-kotlin-commons:$allureKotlinVersion")
  androidTestImplementation("io.qameta.allure:allure-kotlin-junit4:$allureKotlinVersion")
  androidTestImplementation("io.qameta.allure:allure-kotlin-android:$allureKotlinVersion")

  // TestStorage para guardar resultados fuera de /data/data
  androidTestUtil("androidx.test:orchestrator:1.6.1")
}