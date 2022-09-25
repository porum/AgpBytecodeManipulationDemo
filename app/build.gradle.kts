import org.aspectj.lang.annotation.Aspect

plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
}

apply<AsmTransformPlugin>()
apply<JavassistDemoPlugin>()
apply<AspectJDemoPlugin>()

android {
  namespace = "com.panda912.agpbytecodemanipulationdemo"
  compileSdk = 32

  defaultConfig {
    applicationId = "com.panda912.agpbytecodemanipulationdemo"
    minSdk = 21
    targetSdk = 32
    versionCode = 1
    versionName = "1.0"
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
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }
  kotlinOptions {
    jvmTarget = "1.8"
  }
}

dependencies {
  implementation("androidx.core:core-ktx:1.7.0")
  implementation("androidx.appcompat:appcompat:1.4.1")
  implementation("com.google.android.material:material:1.5.0")
}

androidComponents {

}