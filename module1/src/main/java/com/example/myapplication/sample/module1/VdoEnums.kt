package com.example.myapplication.sample.module1

//import com.google.android.gms.ads.AdSize

/**
 *  created by Ashish Saini at 4th Oct 2023
 *
 **/

enum class VdoMediaAspectRatio(val ratio:Int){
    UNKNOWN(0),  ANY(1), LANDSCAPE(2), PORTRAIT(3), SQUARE(4)
}

enum class FailureType(val code :Int){
    API(0), GOOGLE_AD_MANAGER(1), APPLOVIN(2), IRON_SOURCE(3)
}

enum class AdType(val code :Int){
     GOOGLE_AD_MANAGER(1), APPLOVIN(2), IRON_SOURCE(3)
}

enum class ErrorFilterType(val code :String){
    API_FAILURE("api_failure"), LOAD("load_failure"), RELEASE_PLAYER_FAILURE("release_player_failure"),
    SHOW("show_failure")
}