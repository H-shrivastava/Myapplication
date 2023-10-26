package vdo.ai.android.core

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import android.util.Log
import androidx.annotation.ColorRes
import androidx.annotation.NonNull
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.NativeAdOptions
import vdo.ai.android.core.base.VdoBaseBuilder
import vdo.ai.android.core.listeners.VdoNativeTemplateAdListener
import vdo.ai.android.core.manager.VdoManager
import vdo.ai.android.core.models.AdUnitsItem
import vdo.ai.android.core.models.ErrorLogDto
import vdo.ai.android.core.models.GetTagConfigDto
import vdo.ai.android.core.models.VdoAdError
import vdo.ai.android.core.nativeAd.NativeTemplateStyle
import vdo.ai.android.core.nativeAd.TemplateView
import vdo.ai.android.core.networking.RetrofitHelper
import vdo.ai.android.core.utils.*
import kotlin.properties.Delegates

/**
 *  created by Ashish Saini at 6th Oct 2023
 *
 **/
class VdoNativeTemplateAd(builder: VdoNativeTemplateAdBuilder) {

    protected  val TAG = VdoNativeTemplateAd::class.java.simpleName
    private val mActivity : Activity = builder.activity
    private val mPackageName : String = builder.activity.packageName
    private val mEnvironment : String = builder.mEnvironment
    private val mTagName : String = builder.mTagName
    private val mTemplateView : TemplateView? = builder.mTemplateView
    private val mNativeAdOptions :NativeAdOptions = builder.mNativeAdOptions
    private val mListener : VdoNativeTemplateAdListener = builder.mListener
    private val tagConfigService = RetrofitHelper.getTagConfigServices(mActivity)
    private val logPixelService = RetrofitHelper.getLogPixelServices(mActivity)
    private val errorLogService = RetrofitHelper.getErrorLogServices(mActivity)
    private var tagConfigDto: GetTagConfigDto?=null
    private val background :Int = builder.background
    private var adUnitItem: AdUnitsItem?= null
    private var isMediationIsAllowed: Boolean =builder.mIsMediationAllowed
    private var refreshAllowed :Boolean =builder.mRefreshAllowed
    private var mIsPageViewLogged :Boolean = builder.mIsPageViewLogged
    private var mIsPageViewMatchLogged :Boolean = builder.mIsPageViewMatchLogged


    init {
        PixelApiHelper.logPixel(mActivity, mEnvironment, logPixelService, VdoKUtils.getPixelDto(packageName = mPackageName, pageUrl = "", tagName = mTagName, event = VdoEventNames.LOADED))
        VdoManager.initializeAdsSdk(mActivity)
        getTagConfig()
    }

     class VdoNativeTemplateAdBuilder() : VdoBaseBuilder(){
        var mTemplateView: TemplateView?= null
        lateinit var mNativeAdOptions: NativeAdOptions
        lateinit var mListener: VdoNativeTemplateAdListener
        var background by Delegates.notNull<Int>()

        fun withContext(activity: Activity): VdoNativeTemplateAdBuilder {
            this.activity = activity
            return this
        }

        fun setEnvironment(environment: String): VdoNativeTemplateAdBuilder {
            this.mEnvironment = environment
            return this
        }

        fun setTagName(tagName: String): VdoNativeTemplateAdBuilder {
            this.mTagName = tagName
            return this
        }

        fun setTemplateView(templateView: TemplateView?): VdoNativeTemplateAdBuilder {
            this.mTemplateView = templateView
            return this
        }

        fun setAllowRefresh(refresh:Boolean) : VdoNativeTemplateAdBuilder {
            this.mRefreshAllowed = refresh
            return this
        }

        fun setMediation(mediationFlag:Boolean): VdoNativeTemplateAdBuilder {
            this.mIsMediationAllowed=mediationFlag
            return this
        }

        fun setMediaAspectRatio( aspectRatio: VdoMediaAspectRatio): VdoNativeTemplateAdBuilder {
            mNativeAdOptions = NativeAdOptions.Builder()
                .setVideoOptions(VideoOptions.Builder().setStartMuted(true).build())
                .setMediaAspectRatio(aspectRatio.ratio).build()
            return this
        }

        fun setBackgroundColor(@ColorRes color: Int): VdoNativeTemplateAdBuilder{
            this.background = color
            return this
        }

        fun setListener(@NonNull listener: VdoNativeTemplateAdListener): VdoNativeTemplateAdBuilder {
            this.mListener = listener
            return this
        }

        fun build(): VdoNativeTemplateAd {
            return VdoNativeTemplateAd(this)
        }
    }

    private fun getTagConfig() {
        ConfigApiHelper.getConfig(tagConfigService, mPackageName,
            mTagName, object : ConfigApiHelper.ConfigApiListener{

                override fun onSuccess(tagConfigDto: GetTagConfigDto) {
                   this@VdoNativeTemplateAd.tagConfigDto = tagConfigDto
                    loadNativeAd()
                }
                override fun onFailure(code: Int, errorMessage: String?) {
                    Log.e(TAG, "onFailure >>>>> $errorMessage")
                    setErrorLog(null, ErrorFilterType.API_FAILURE, errorMessage)
                    val adError = VdoAdError(code,"", errorMessage,0, "", FailureType.API)
                    mListener.onAdFailedToLoad(adError)
                }
        })
    }
    fun ifShouldAllowLoadAd(){
        if (isMediationIsAllowed){
            destroy()
            loadNativeAd()
        }
    }
    private fun loadNativeAd() {
        try{
            if (tagConfigDto == null)
                return

            if (VdoKUtils.isAppInForegrounded()){
                if (!VdoKUtils.isConfigAllowed(tagConfigDto)){

                    adUnitItem = tagConfigDto?.adunits?.get(0)
                    adUnitItem?.adUrl?.let {
                        if (!mIsPageViewLogged){
                            mIsPageViewLogged = true
                            PixelApiHelper.logPixel(mActivity, mEnvironment, logPixelService, VdoKUtils.getPixelDto(packageName = mPackageName, pageUrl = "", tagName = mTagName, event = VdoEventNames.PAGE_VIEW))
                        }
                        val adLoader: AdLoader = AdLoader.Builder(mActivity, it)
                            .forNativeAd { nativeAd ->
                                Log.e(TAG, "GAM forNativeAd >>>>>>>>>>>")

                                if (isMediationIsAllowed){
                                    mListener.forNativeAd(nativeAd)
                                }else{
                                    val background = ColorDrawable(mActivity.resources.getColor(background))
                                    val styles: NativeTemplateStyle = NativeTemplateStyle.Builder()
                                        .withMainBackgroundColor(background).build()

                                    mTemplateView?.setStyles(styles)
                                    mTemplateView?.setNativeAd(nativeAd)
                                }
                            }
                            .withAdListener(object : AdListener() {

                                override fun onAdFailedToLoad(adError: LoadAdError) {
                                    Log.e(TAG, "GAM Ad failed to load $adError")
                                    reloadNativeAd(VdoKUtils.Ad_FAILED_REFRESH_TIME)
                                    mListener.onAdFailedToLoad(VdoKUtils.getAdError(adError))
                                }

                                override fun onAdLoaded() {
                                    super.onAdLoaded()
                                    Log.d(TAG, "GAM Ad was loaded")
                                    mListener.onAdLoaded()
                                }

                                override fun onAdImpression() {
                                    super.onAdImpression()
                                    Log.d(TAG, "GAM Ad was impression")
                                    eventAdLoaded()
                                    eventAdImpression()
                                }

                                override fun onAdClosed() {
                                    super.onAdClosed()
                                    Log.d(TAG, "GAM Ad was clicked.")
                                    mListener.onAdClicked()
                                }
                            }).withNativeAdOptions(mNativeAdOptions).build()

                        adLoader.loadAd(VdoManager.getAdManagerAdRequest())
                    }
                }else{
                    PixelApiHelper.logPixel(mActivity, mEnvironment, logPixelService, VdoKUtils.getPixelDto(packageName = mPackageName, pageUrl = "", tagName = mTagName , event = VdoEventNames.BLOCK_APP, reason = tagConfigDto?.reason))
                }
            }else{
                Log.d(TAG, "Ads is not showing due to app is in background ")
                reloadNativeAd(VdoKUtils.Ad_FAILED_REFRESH_TIME)
            }
        }catch (e:Exception){
            setErrorLog(e, ErrorFilterType.LOAD)
        }
    }

    private fun reloadNativeAd(seconds:Long){
        if(refreshAllowed){ VdoKUtils.getMyHandler().postDelayed(runnable, seconds * 1000) }
    }

    private val runnable:Runnable = Runnable {
        loadNativeAd()
    }

    private fun eventAdLoaded(){
        val eventDataDto = VdoKUtils.getEventData(0, adUnitItem?.partner, 0.5, VdoKUtils.TYPE_VIDEO)
        if (!mIsPageViewMatchLogged){
            mIsPageViewMatchLogged = true
            PixelApiHelper.logPixel(mActivity, mEnvironment, logPixelService, VdoKUtils.getPixelDto(packageName = mPackageName, pageUrl = "",
            tagName = mTagName, event = VdoEventNames.PAGE_VIEW_MATCH, eventDataDto= eventDataDto))
        }
        PixelApiHelper.logPixel(mActivity, mEnvironment, logPixelService, VdoKUtils.getPixelDto(packageName = mPackageName, pageUrl = "",
            tagName = mTagName, event = VdoEventNames.AD_MATCH, eventDataDto= eventDataDto))
    }

    private fun eventAdImpression(){

        val eventDataDto = VdoKUtils.getEventData(0, adUnitItem?.partner, 0.5)
        PixelApiHelper.logPixel(mActivity, mEnvironment, logPixelService, VdoKUtils.getPixelDto(packageName = mPackageName, pageUrl = "",
            tagName = mTagName, event = VdoEventNames.VIEWABLE_IMPRESSION, eventDataDto= eventDataDto))

        mListener.onAdImpression()
    }

    private fun setErrorLog(e:Exception?, errorType:ErrorFilterType, errorMessage:String?=null){
        var message:String = if (e != null) {
            Log.getStackTraceString(e)
        }else{
            errorMessage?:""
        }
//        message = "filterType.code + message"
        PixelApiHelper.logError(mEnvironment, errorLogService, ErrorLogDto(message, mTagName))
        PixelApiHelper.logPixel(mActivity, mEnvironment, logPixelService, VdoKUtils.getPixelDto(packageName = mPackageName, pageUrl = "",
            tagName = mTagName , event = VdoEventNames.ERROR, errorCode = errorType.code))
    }

    fun destroy(){
        VdoKUtils.getMyHandler().removeCallbacks(runnable)
    }
}