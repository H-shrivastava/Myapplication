import com.example.myapplication1.Constants
plugins {
    id("com.android.library")
    kotlin("android")
    id("maven-publish")
}

//afterEvaluate {
//    publishing {
//        publications {
//            release(MavenPublication) {
//                from components.release
//            }
//        }
//    }
//}

android {
    namespace = "com.example.myapplication.sample.module1"

    compileSdk = Constants.SDK_VERSION

    defaultConfig {
        minSdkVersion(Constants.MIN_SDK)
        targetSdkVersion(Constants.TARGET_SDK)
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }



    buildTypes {
        getByName("release") {
            isMinifyEnabled = false // IMPORTANT BIT else you release aar will have no classes
            proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
            )
        }
    }
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(android.sourceSets.getByName("main").java.srcDirs)
}

afterEvaluate {
    publishing {
        publications {
            val release by publications.registering(MavenPublication::class) {
                from(components["release"])
                artifact(sourcesJar.get())
                artifactId = "module1"
                groupId = "com.github.H-shrivastava.Myapplication"
                version = "1.0.5"  //github release of com.github.danbrough.jitpackdemo
            }
        }
    }
}

dependencies {
    implementation(com.example.myapplication1.Libs.KOTLIN_STDLIB)
}