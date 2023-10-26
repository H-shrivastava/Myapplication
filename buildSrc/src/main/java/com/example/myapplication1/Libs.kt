package com.example.myapplication1

object Libs {

    const val KOTLIN_STDLIB = "org.jetbrains.kotlin:kotlin-stdlib:1.7.10"
    const val CORE_KTX = "androidx.core:core-ktx:1.3.1" // old 1.3.1
    const val MATERIAL_DESIGN = "com.google.android.material:material:1.2.1" // old latest 1.2.1
    const val MULTI_DEX = "androidx.multidex:multidex:2.0.1"
    const val GSON = "com.google.code.gson:gson:2.8.9"

    const val IRON_SOURCE_SDK = "com.ironsource.sdk:mediationsdk:7.3.1.1"
    // Note : This annotation used in NativeTemplateStyle
    const val ERROR_PRONE_ANNOTATIONS = "com.google.errorprone:error_prone_annotations:2.16"

    object PlayServices {
        const val ADS = "com.google.android.gms:play-services-ads:22.4.0"
        const val ADS_IDENTIFIER = "com.google.android.gms:play-services-ads-identifier:18.0.1"
    }

    object AndroidX {
        const val APPCOMPAT = "androidx.appcompat:appcompat:1.2.0"
        const val CONSTRAINT_LAYOUT = "androidx.constraintlayout:constraintlayout:2.1.4"
        const val CARD_VIEW = "androidx.cardview:cardview:1.0.0"
        const val WEBKIT = "androidx.webkit:webkit:1.4.0"
    }

    object Exoplayer {
        const val CORE = "com.google.android.exoplayer:exoplayer-core:2.19.0"
        const val UI = "com.google.android.exoplayer:exoplayer-ui:2.19.0"
        const val IMA = "com.google.android.exoplayer:extension-ima:2.19.0"
    }

    object Retrofit {
        const val RETROFIT = "com.squareup.retrofit2:retrofit:2.9.0"
        const val CONVERTER_GSON = "com.squareup.retrofit2:converter-gson:2.9.0"
        const val LOGGING_INTERCEPTOR = "com.squareup.okhttp3:logging-interceptor:4.8.0"
        const val OK_HTTP = "com.squareup.okhttp3:okhttp:4.9.1"
    }
    object Coroutine {
        const val COROUTINES_CORE = "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1"
        const val COROUTINES_ANDROID = "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1"
    }

    object Applovin {
        const val APPLOVIN_SDK = "com.applovin:applovin-sdk:11.10.1"
        //  NOTE:- This dependency belongs from appOpen ad in applovin
        const val LIFECYCLE_PROCESS = "androidx.lifecycle:lifecycle-process:2.6.2"
    }

    object Test {

        const val JUNIT = "junit:junit:4.13.2"
        const val ANDROIDX_JUNIT= "androidx.test.ext:junit:1.1.5"
        const val ESPRESSO_CORE = "androidx.test.espresso:espresso-core:3.5.1"

    }


//    object Teads {
//        fun sdk(version: String) = "tv.teads.sdk.android:sdk:$version@aar"
//        fun admobAdapter(version: String) = "tv.teads.sdk.android:admobadapter:$version@aar"
//        fun applovinAdapter(version: String) = "tv.teads.sdk.android:applovinadapter:$version@aar"
//        fun smartAdapter(version: String) = "tv.teads.sdk.android:smartadapter:$version@aar"
//    }




}