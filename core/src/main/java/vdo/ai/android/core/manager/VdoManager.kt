package vdo.ai.android.core.manager

import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import vdo.ai.android.core.utils.VdoKUtils

/**
 *  created by Ashish Saini at 4th Oct 2023
 *
 **/
class VdoManager {

    companion object {

        fun setDeviceId(context: Context){

            val deviceId = VdoKUtils.getDeviceId(context)
            val testDeviceIds = mutableListOf(deviceId)
            val configuration = RequestConfiguration.Builder().setTestDeviceIds(testDeviceIds).build()
            MobileAds.setRequestConfiguration(configuration)
        }

        @JvmStatic
        fun initializeAdsSdk(context: Context){
            // initialize GAM sdk
            MobileAds.initialize(context) {
            }
        }

        @JvmStatic
        fun getAdManagerAdRequest(): AdManagerAdRequest {
            return AdManagerAdRequest.Builder().build()
        }

        @JvmStatic
        fun getAdRequest(): AdRequest {
            return AdRequest.Builder().build()
        }

//        @JvmStatic
//        fun initMultiDex(context: Context){
//            MultiDex.install(context)
//        }


    }
}