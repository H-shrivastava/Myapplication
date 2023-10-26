package vdo.ai.android.core.manager

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import vdo.ai.android.core.VdoAppOpenAd
import vdo.ai.android.core.listeners.OnShowAdCompleteListener
import vdo.ai.android.core.listeners.VdoAdErrorListener
import vdo.ai.android.core.models.AdUnitsItem
import vdo.ai.android.core.models.ErrorLogDto
import vdo.ai.android.core.models.GetTagConfigDto
import vdo.ai.android.core.utils.*
import vdo.ai.android.core.utils.ConfigApiHelper
import vdo.ai.android.core.utils.PixelApiHelper
import vdo.ai.android.core.utils.VdoEventNames
import java.util.*

/**
 *  created by Ashish Saini at 6th Oct 2023
 *
 **/
 class VdoAppOpenManager(private val vdoAppOpenAd:VdoAppOpenAd) : AppOpenAd.AppOpenAdLoadCallback(), VdoAdErrorListener {

    protected  val TAG  = VdoAppOpenManager::class.java.simpleName
    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    var isShowingAd = false

    private var loadTime: Long = 0

    private var tagConfigDto: GetTagConfigDto?=null
    private var adUnitItem: AdUnitsItem?= null
    lateinit var activity: Activity
    var mOnShowAdCompleteListener: OnShowAdCompleteListener?= null
    private var isShowAdFromThirdParty :Boolean = false

    fun showAdIfAvailable(activity: Activity, onShowAdCompleteListener: OnShowAdCompleteListener) {
        this.mOnShowAdCompleteListener = onShowAdCompleteListener
        this.activity = activity
        getTagConfig()
    }

    private fun getTagConfig() {
        ConfigApiHelper.getConfig(vdoAppOpenAd.tagConfigService, vdoAppOpenAd.mPackageName,
            vdoAppOpenAd.mTagName, object : ConfigApiHelper.ConfigApiListener{

                override fun onSuccess(tagConfigDto: GetTagConfigDto) {
                    this@VdoAppOpenManager.tagConfigDto = tagConfigDto
                    loaAppOpenAd(activity)
                }

                override fun onFailure(code: Int, errorMessage: String?) {
                    Log.e(TAG, "onFailure >>>>> $errorMessage")
                    setErrorLog(null, ErrorFilterType.API_FAILURE, errorMessage)
                    mOnShowAdCompleteListener?.onShowAdComplete()
                }
            })
    }

    private fun loaAppOpenAd(activity: Activity) {
        try{
            if (tagConfigDto == null){
                return
            }

            if (!VdoKUtils.isConfigAllowed(tagConfigDto)) {

                adUnitItem = tagConfigDto?.adunits?.get(0)
                if (adUnitItem?.adUrl.isNullOrEmpty().not()){

                    PixelApiHelper.logPixel(vdoAppOpenAd.mContext, vdoAppOpenAd.mEnvironment, vdoAppOpenAd.logPixelService, VdoKUtils.getPixelDto(packageName = vdoAppOpenAd.mPackageName, pageUrl = "", tagName = vdoAppOpenAd.mTagName, event = VdoEventNames.PAGE_VIEW))

                    isLoadingAd = true
                    val request = AdRequest.Builder().build()
                    AppOpenAd.load(activity, adUnitItem?.adUrl!!, request, AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,this@VdoAppOpenManager)
                }
            }else{
                PixelApiHelper.logPixel(vdoAppOpenAd.mContext, vdoAppOpenAd.mEnvironment, vdoAppOpenAd.logPixelService, VdoKUtils.getPixelDto(packageName = vdoAppOpenAd.mPackageName, pageUrl = "", tagName = vdoAppOpenAd.mTagName , event = VdoEventNames.BLOCK_APP, reason = tagConfigDto?.reason))
                mOnShowAdCompleteListener?.onShowAdComplete()
            }
        }catch (e:Exception){
            setErrorLog(e, ErrorFilterType.LOAD)
        }
    }

    override fun onAdLoaded(ad: AppOpenAd) {
        Log.d(TAG, "GAM Ad was loaded")
        setShowAdFromThirdParty(false)
        appOpenAd = ad
        isLoadingAd = false
        loadTime = Date().time

        eventAdLoaded()
        showAppOpenAd()
    }

    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
        isLoadingAd = false
        Log.d(TAG, "GAM Ad was failed to load. $loadAdError")
        setShowAdFromThirdParty(false)

        mOnShowAdCompleteListener?.onShowAdComplete()
        vdoAppOpenAd.mListener.onAdFailedToLoad(VdoKUtils.getAdError(loadAdError))
    }

    override fun setShowAdFromThirdParty(flag: Boolean) {

    }

    override fun onVdoAdFailedToShowFullScreen(adError: AdError?) {

    }

    private fun showAppOpenAd(){
        try{
            appOpenAd?.apply {
                fullScreenContentCallback = object : FullScreenContentCallback() {

                    override fun onAdDismissedFullScreenContent() {
                        // Set the reference to null so isAdAvailable() returns false.
                        appOpenAd = null
                        isShowingAd = false
                        Log.d(TAG, "GAM Ad was dismiss full screen")

                        eventAdDismiss()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        Log.d(TAG, "GAM Ad failed to show full screen $adError")
                        appOpenAd = null
                        isShowingAd = false

                        if (isShowAdFromThirdParty){
                            this@VdoAppOpenManager.onVdoAdFailedToShowFullScreen(adError)
                        }else{
                            mOnShowAdCompleteListener?.onShowAdComplete()
                            vdoAppOpenAd.mListener.onAdFailedToShowFullScreenContent(VdoKUtils.getAdError(adError))
                        }
                    }

                    override fun onAdShowedFullScreenContent() {
                        Log.d(TAG, "GAM Ad was show full screen.")
                        eventShowedFullScreen()
                    }
                }
                isShowingAd = true
                show(activity)
            }
        }catch (e:Exception){
            setErrorLog(e, ErrorFilterType.SHOW)
        }
    }

    fun eventAdLoaded(){
        val eventDataDto = VdoKUtils.getEventData(0, adUnitItem?.partner, 0.5, VdoKUtils.TYPE_VIDEO)

        PixelApiHelper.logPixel(vdoAppOpenAd.mContext, vdoAppOpenAd.mEnvironment, vdoAppOpenAd.logPixelService, VdoKUtils.getPixelDto(packageName = vdoAppOpenAd.mPackageName, pageUrl = "",
            tagName = vdoAppOpenAd.mTagName, event = VdoEventNames.PAGE_VIEW_MATCH, eventDataDto= eventDataDto))

        PixelApiHelper.logPixel(vdoAppOpenAd.mContext, vdoAppOpenAd.mEnvironment, vdoAppOpenAd.logPixelService, VdoKUtils.getPixelDto(packageName = vdoAppOpenAd.mPackageName, pageUrl = "",
            tagName = vdoAppOpenAd.mTagName, event = VdoEventNames.AD_MATCH, eventDataDto= eventDataDto))

        vdoAppOpenAd.mListener.onAdLoaded()
    }

    fun eventShowedFullScreen(){
        val eventDataDto = VdoKUtils.getEventData(0, adUnitItem?.partner, 0.5)

        PixelApiHelper.logPixel(vdoAppOpenAd.mContext, vdoAppOpenAd.mEnvironment, vdoAppOpenAd.logPixelService, VdoKUtils.getPixelDto(packageName = vdoAppOpenAd.mPackageName, pageUrl = "",
            tagName = vdoAppOpenAd.mTagName, event = VdoEventNames.VIEWABLE_IMPRESSION, eventDataDto= eventDataDto))

        vdoAppOpenAd.mListener.onAdShowedFullScreenContent()
    }

    fun eventAdDismiss(){
        PixelApiHelper.logPixel(vdoAppOpenAd.mContext, vdoAppOpenAd.mEnvironment, vdoAppOpenAd.logPixelService, VdoKUtils.getPixelDto(packageName = vdoAppOpenAd.mPackageName, pageUrl = "",
            tagName = vdoAppOpenAd.mTagName, event = VdoEventNames.CROSS_CLICKED))

        vdoAppOpenAd.mListener.onAdDismissedFullScreenContent()
        mOnShowAdCompleteListener?.onShowAdComplete()
    }

    private fun setErrorLog(e:Exception?, errorType:ErrorFilterType, errorMessage:String?=null){
        var message:String = if (e != null) {
            Log.getStackTraceString(e)
        }else{
            errorMessage?:""
        }
//        message = "filterType.code + message"
        PixelApiHelper.logError(vdoAppOpenAd.mEnvironment, vdoAppOpenAd.errorLogService, ErrorLogDto(message, vdoAppOpenAd.mTagName))
        PixelApiHelper.logPixel(vdoAppOpenAd.mContext, vdoAppOpenAd.mEnvironment , vdoAppOpenAd.logPixelService, VdoKUtils.getPixelDto(packageName = vdoAppOpenAd.mPackageName, pageUrl = "",
            tagName = vdoAppOpenAd.mTagName , event = VdoEventNames.ERROR, errorCode = errorType.code))
    }

}