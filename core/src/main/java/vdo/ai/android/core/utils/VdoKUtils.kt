package vdo.ai.android.core.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Base64
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration
import vdo.ai.android.core.VdoBannerAd
import vdo.ai.android.core.models.EventDto
import vdo.ai.android.core.models.GetTagConfigDto
import vdo.ai.android.core.models.PixelDto
import vdo.ai.android.core.models.VdoAdError
import java.util.*
/**
 *  created by Ashish Saini at 4th Oct 2023
 *
 **/
object VdoKUtils {

    private val TAG = VdoKUtils::class.java.simpleName.toString()
    const val PLATFORM = "android"
    const val TYPE_BANNER = "banner"
    const val TYPE_VIDEO = "VIDEO"
    const val Ad_FAILED_REFRESH_TIME = 5L
    const val AD_REFRESH_MEDIATION=32L
    const val AD_REFRESH=30L
    private lateinit var handler: Handler

    var currentMediationIdentifyer:Int?= null
    var previousMap:MutableMap<Int, Int>?= mutableMapOf()

    private const val APPLICATION_ID= "com.google.android.gms.ads.APPLICATION_ID"

    fun addFragment(fragmentManager: FragmentManager, containerId : Int, fragment: Fragment){

        fragmentManager.beginTransaction().add(containerId, fragment)
            .addToBackStack(null).commit()
    }

    fun replaceFragment(fragmentManager: FragmentManager, containerId : Int, fragment: Fragment){
        fragmentManager.beginTransaction().replace(containerId, fragment)
            .addToBackStack(null).commit()
    }

    fun getUserAgent(context: Context): String{
        val androidVersion = getAndroidVersion()
        val deviceName = getDeviceName()
        val appName = getAppName(context)
        val appVersion = getAppVersionName(context)
        return "$androidVersion; $deviceName; $appName/$appVersion"
    }

    @SuppressLint("HardwareIds")
    fun getDeviceId(context:Context) :String{
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)

    }
    private fun getAndroidVersion() : String{
        return "Android ${Build.VERSION.RELEASE}"
    }

    private fun getDeviceName(): String{
        return Build.BRAND +" "+ Build.MODEL
    }

    private fun getAppName (context:Context):String{
        var appName = ""
        try {
            val packageManager: PackageManager = context.packageManager
            val info = packageManager.getApplicationInfo(context.packageName,
                PackageManager.GET_META_DATA)
            appName =  packageManager.getApplicationLabel(info) as String
            appName =  Base64.encodeToString(appName.toByteArray(), Base64.NO_WRAP);
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return appName
    }

    private fun getAppVersionName(context:Context):String {
        var versionName = ""
        try {
            val pInfo: PackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            versionName = pInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return versionName
    }

    fun getEventData(offset:Int, partner:String?, cpm:Double, type:String? = null):EventDto{
        return EventDto(offset,partner,cpm,type)
    }

    fun getPixelDto(packageName:String, pageUrl:String, tagName:String,
                    event:String, reason:String?= null, eventData: Int? = 1,
                    uId:String? = "", eventDataDto: EventDto?=null, errorCode:String?= null) : PixelDto {
        var param: Any? = errorCode?: eventDataDto ?: reason ?: eventData
//        if (code != null)
//            param = null

        return PixelDto(packageName, pageUrl, tagName, event, param, uId)
    }

    fun isConfigAllowed(tagConfig: GetTagConfigDto?):Boolean{
        return tagConfig?.allowed?.equals("disabled", true) == true
    }

    fun getAdError(error: Any?): VdoAdError?{
        var adError : VdoAdError?=null
        when (error) {
            is LoadAdError -> {
                adError = VdoAdError(error.code, error.domain, error.message, 0, "",  FailureType.GOOGLE_AD_MANAGER)
            }
            is AdError -> {
                adError = VdoAdError(error.code, error.domain, error.message, 0,"", FailureType.GOOGLE_AD_MANAGER)
            }
//            is MaxError -> {
//                adError = Z1AdError(error.code,  "", error.message, error.mediatedNetworkErrorCode, error.mediatedNetworkErrorMessage, FailureType.APPLOVIN)
//            }
//            is IronSourceError -> {
//                adError = Z1AdError(error.errorCode,  "", error.errorMessage, 0, "", FailureType.IRON_SOURCE)
//            }
        }
        return adError
    }

    fun isAppInForegrounded(): Boolean {
        val appProcessInfo = RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(appProcessInfo)
        return appProcessInfo.importance == IMPORTANCE_FOREGROUND ||
                appProcessInfo.importance == IMPORTANCE_VISIBLE
    }

    fun getMyHandler(): Handler {
        if(this::handler.isInitialized.not()){
            handler = Handler(Looper.getMainLooper())
        }
        return handler
    }


//    fun getIronSourceAdSize(mAdSize:Z1AdSize):ISBannerSize{
//        val size = when (mAdSize) {
//            Z1AdSize.BANNER -> {
//                ISBannerSize.BANNER
//            }
//            Z1AdSize.MEDIUM_RECTANGLE -> {
//                ISBannerSize.RECTANGLE
//            }
//            Z1AdSize.LARGE_BANNER -> {
//                ISBannerSize.LARGE
//            }
//            else -> {
//                ISBannerSize.SMART
//            }
//        }
//        return size
//    }

//    fun getApplovinAdView(mActivity:Activity, mApplovinAdUnitId:String, mAdSize: Z1AdSize):MaxAdView{
//        return when (mAdSize) {
//            Z1AdSize.BANNER -> {
//                MaxAdView(mApplovinAdUnitId, mActivity)
//            }
//            Z1AdSize.MEDIUM_RECTANGLE -> {
//                MaxAdView(mApplovinAdUnitId, MaxAdFormat.MREC, mActivity)
//            }
//            Z1AdSize.LARGE_BANNER -> {
//                MaxAdView(mApplovinAdUnitId, MaxAdFormat.LEADER, mActivity)
//            }
//            else -> {
//                MaxAdView(mApplovinAdUnitId, mActivity)
//            }
//        }
//    }

//    fun getApplovinBannerHeight(mActivity: Activity, mAdSize: Z1AdSize):Int{
//        return  when (mAdSize) {
//            Z1AdSize.BANNER -> {
//                50.toPx(mActivity)
//            }
//            Z1AdSize.MEDIUM_RECTANGLE -> {
//                250.toPx(mActivity)
//            }
//            Z1AdSize.LARGE_BANNER -> {
//                100.toPx(mActivity)
//            }
//            else -> {
//                50.toPx(mActivity)
//            }
//        }
//    }

//    fun getRandomString(length: Int) : String {
//        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
//        return (1..length)
//            .map { allowedChars.random() }
//            .joinToString("")
//    }

    fun getMediationBannerAdSize(mediationBannerAdConfiguration: MediationBannerAdConfiguration?):VdoAdSize{

        var adSize : VdoAdSize = VdoAdSize.BANNER

        val mediationAdSize : AdSize?= mediationBannerAdConfiguration?.adSize
        if (mediationAdSize == null){
            return adSize
        }else{
            if(mediationAdSize.width == 320 && mediationAdSize.height == 50){
                adSize = VdoAdSize.BANNER
            } else if (mediationAdSize.width == 300 && mediationAdSize.height == 250){
                adSize = VdoAdSize.MEDIUM_RECTANGLE
            } else if (mediationAdSize.width == 320 && mediationAdSize.height == 100){
                adSize = VdoAdSize.LARGE_BANNER
            } else if (mediationAdSize.width == 468 && mediationAdSize.height == 60){
                adSize = VdoAdSize.FULL_BANNER
            } else if (mediationAdSize.width == 728 && mediationAdSize.height == 90){
                adSize = VdoAdSize.LEADERBOARD
            }
        }
        return adSize
    }

    fun updateApplicationId(context: Context, newApplicationId:String ): Boolean{
        try {
            val applicationInfo = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
            val bundle = applicationInfo.metaData
            val currentApplicationId = bundle.getString(APPLICATION_ID)
            Log.d(TAG, "currentApplicationId Found: $currentApplicationId")
            //you can replace your key APPLICATION_ID here  "ca-app-pub-3940256099942544~3347511713"
            applicationInfo.metaData.putString(APPLICATION_ID, newApplicationId)
            val newApplicationId = bundle.getString(APPLICATION_ID)
            Log.d(TAG, "newApplicationId Found: $newApplicationId")
            return true
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Failed to load meta-data, NameNotFound: " + e.message)
        } catch (e: NullPointerException) {
            Log.e(TAG, "Failed to load meta-data, NullPointer: " + e.message)
        }
        return false
    }

    @JvmStatic
    fun destroyBanner(vdoBannerAd: VdoBannerAd?){
        vdoBannerAd?.apply {
            playerHandler?.releasePlayer(true)
            playerHandler =null
            adManagerAdView?.destroy()
            adManagerAdView = null
            mAdContainer = null
            getMyHandler().removeCallbacks(this.runnable)
        }
    }

    @JvmStatic
    fun onPause(activity:Activity?, bannerAd:VdoBannerAd?){
        bannerAd?.adManagerAdView?.pause()
    }

    @JvmStatic
    fun onResume(activity: Activity?, bannerAd: VdoBannerAd?){
        bannerAd?.adManagerAdView?.resume()
    }


}