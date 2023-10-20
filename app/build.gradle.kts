import com.example.myapplication1.*

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}


android {
    namespace = "com.example.myapplication"
    compileSdk = (Constants.SDK_VERSION)

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk =(Constants.MIN_SDK)
        targetSdk = (Constants.TARGET_SDK)
        versionCode = 1
        multiDexEnabled = true
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

//    buildTypes {
//        release {
//            minifyEnabled = false
//            proguardFiles (getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
//        }
//    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility  = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
}

dependencies {

    implementation ("androidx.core:core-ktx:1.9.0")
    implementation ("androidx.appcompat:appcompat:1.6.1")
    implementation ("com.google.android.material:material:1.9.0")
    implementation ("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation ("junit:junit:4.13.2")
    androidTestImplementation ("androidx.test.ext:junit:1.1.5")
    androidTestImplementation ("androidx.test.espresso:espresso-core:3.5.1")
    implementation (project(":module1"))
    implementation (project(":module2"))
//    implementation 'com.github.H-shrivastava.Myapplication:module1:1.0.2'

}