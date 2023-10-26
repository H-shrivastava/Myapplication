package vdo.ai.android.core

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.admanager.AdManagerInterstitialAd
import com.google.android.gms.ads.admanager.AdManagerInterstitialAdLoadCallback
import vdo.ai.android.core.base.VdoBaseBuilder
import vdo.ai.android.core.listeners.VdoAdErrorListener
import vdo.ai.android.core.listeners.VdoInterstitialListener
import vdo.ai.android.core.manager.VdoManager
import vdo.ai.android.core.models.*
import vdo.ai.android.core.networking.RetrofitHelper
import vdo.ai.android.core.utils.*

/**
*  created by Ashish Saini at 5th Oct 2023
*
**/
open class VdoInterstitialAd(builder : VdoInterstitialAdBuilder) :  AdManagerInterstitialAdLoadCallback(), VdoAdErrorListener {

    protected open val TAG = VdoInterstitialAd::class.java.simpleName
    protected val mActivity: Activity = builder.activity
    protected val mEnvironment : String = builder.mEnvironment
    protected val mListener: VdoInterstitialListener = builder.mListener
    private val mTagName:String= builder.mTagName
    private val mPackageName : String = builder.activity.packageName
    private val tagConfigService = RetrofitHelper.getTagConfigServices(mActivity)
    private val logPixelService = RetrofitHelper.getLogPixelServices(mActivity)
    private val errorLogService = RetrofitHelper.getErrorLogServices(mActivity)
    private var tagConfigDto: GetTagConfigDto?=null
    private var mInterstitialAd: AdManagerInterstitialAd?= null
    private var adUnitItem: AdUnitsItem?= null
    private var isMediationAllowed : Boolean = builder.mIsMediationAllowed
    private var mIsPageViewLogged :Boolean = builder.mIsPageViewLogged
    private var mIsPageViewMatchLogged :Boolean = builder.mIsPageViewMatchLogged
    private var refreshAllowed :Boolean =builder.mRefreshAllowed
    private var isShowAdFromThirdParty :Boolean = false

    init {
        PixelApiHelper.logPixel(mActivity, mEnvironment, logPixelService, VdoKUtils.getPixelDto(packageName = mPackageName, pageUrl = "", tagName = mTagName, event = VdoEventNames.LOADED))
        VdoManager.initializeAdsSdk(mActivity)
        getTagConfig()
    }

    open class VdoInterstitialAdBuilder() : VdoBaseBuilder(){
        lateinit var mListener: VdoInterstitialListener

        open fun withContext(activity: Activity): VdoInterstitialAdBuilder {
            this.activity = activity
            return this
        }

        open fun setEnvironment(environment: String): VdoInterstitialAdBuilder {
            this.mEnvironment = environment
            return this
        }

        open fun setTagName(tagName: String): VdoInterstitialAdBuilder {
            this.mTagName = tagName
            return this
        }

        open fun setAllowRefresh(refresh:Boolean) : VdoInterstitialAdBuilder {
            this.mRefreshAllowed = refresh
            return this
        }

        open fun setMediation(mediationStatus:Boolean): VdoInterstitialAdBuilder {
            this.mIsMediationAllowed = mediationStatus
            return this
        }

        open fun setListener(listener: VdoInterstitialListener): VdoInterstitialAdBuilder{
            this.mListener = listener
            return this
        }

        open fun build(): VdoInterstitialAd {
            return  VdoInterstitialAd(this)
        }
    }


    private fun getTagConfig() {
        ConfigApiHelper.getConfig(tagConfigService, mPackageName,
            mTagName, object : ConfigApiHelper.ConfigApiListener {

                override fun onSuccess(tagConfigDto: GetTagConfigDto) {
                    this@VdoInterstitialAd.tagConfigDto = tagConfigDto
                    loadInterstitialAd()
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
        if (isMediationAllowed){
            destroy()
            loadInterstitialAd()
        }
    }

    private fun loadInterstitialAd() {
        try{
            if (tagConfigDto == null){
                return
            }

            if (VdoKUtils.isAppInForegrounded()){
                if (!VdoKUtils.isConfigAllowed(tagConfigDto)){
                    adUnitItem = tagConfigDto?.adunits?.get(0)
                    adUnitItem?.adUrl?.let {

                        if (!mIsPageViewLogged){
                            mIsPageViewLogged = true
                            PixelApiHelper.logPixel(mActivity, mEnvironment, logPixelService, VdoKUtils.getPixelDto(packageName = mPackageName, pageUrl = "", tagName = mTagName, event = VdoEventNames.PAGE_VIEW))
                        }

                        val adRequest = VdoManager.getAdManagerAdRequest()
                        if (mInterstitialAd == null){
                            AdManagerInterstitialAd.load(mActivity, it, adRequest,this@VdoInterstitialAd)
                        }else{
                            showInterstitial()
                        }
                    }
                }else{
                    PixelApiHelper.logPixel(mActivity, mEnvironment, logPixelService, VdoKUtils.getPixelDto(packageName = mPackageName, pageUrl = "", tagName = mTagName , event = VdoEventNames.BLOCK_APP, reason = tagConfigDto?.reason))
                }
            }else{
                if (!isMediationAllowed){
                    Log.d(TAG, "Ads is not showing due to app is in background ")
                    reloadInterstitialAd(VdoKUtils.Ad_FAILED_REFRESH_TIME)
                }

            }
        }catch (e:Exception){
            setErrorLog(e, ErrorFilterType.LOAD)
        }
    }

    override fun onAdLoaded(interstitialAd: AdManagerInterstitialAd) {
        Log.d(TAG, "GAM Ad was loaded")
        mInterstitialAd = interstitialAd
        setShowAdFromThirdParty(false)
        mListener.onAdLoaded()
        showInterstitial()
    }

    override fun onAdFailedToLoad(adError: LoadAdError) {
        mInterstitialAd = null
        Log.e(TAG, "GAM Ad was failed to load. $adError")

        setShowAdFromThirdParty(false)
        reloadInterstitialAd(VdoKUtils.Ad_FAILED_REFRESH_TIME)
        mListener.onAdFailedToLoad(VdoKUtils.getAdError(adError))
    }

    override fun setShowAdFromThirdParty(flag: Boolean) {
        isShowAdFromThirdParty = flag
    }

    override fun onVdoAdFailedToShowFullScreen(adError: AdError?) {

    }

    private fun showInterstitial() {
        try {
            // Show the ad if it's ready. Otherwise toast and restart the game.
            if (mInterstitialAd == null) {
                Log.e(TAG, "GAM interstitial ad wasn't ready yet.")
                return
            }

            mInterstitialAd?.apply {
                fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Log.d(TAG, "GAM Ad was dismiss full screen")
                        mInterstitialAd = null
                        eventAdDismiss()
                    }

                    override fun onAdImpression() {
                        super.onAdImpression()
                        Log.d(TAG, "GAM Ad impression")
                        eventAdLoaded(true)
                        eventAdImpression()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        mInterstitialAd = null
                        Log.e(TAG, "GAM Ad failed to show full screen $adError")
                        if (isShowAdFromThirdParty){
                            this@VdoInterstitialAd.onVdoAdFailedToShowFullScreen(adError)
                        }else{
                            mListener.onAdFailedToShowFullScreenContent(VdoKUtils.getAdError(adError))
                        }
                    }

                    override fun onAdShowedFullScreenContent() {
                        Log.d(TAG, "GAM Ad show full screen")
                        mListener.onAdShowedFullScreenContent()

                    }
                }
                show(mActivity)
            }
        }catch (e:Exception){
            setErrorLog(e, ErrorFilterType.SHOW)
        }
    }

    protected fun reloadInterstitialAd(seconds:Long){
        if(refreshAllowed){
            VdoKUtils.getMyHandler().postDelayed(runnable, seconds * 1000)
        }
    }

    private val runnable:Runnable = Runnable {
        loadInterstitialAd()
    }


    protected fun eventAdLoaded(isAdLoadedListener:Boolean=false){
        val eventDataDto = VdoKUtils.getEventData(0, adUnitItem?.partner, 0.5, VdoKUtils.TYPE_VIDEO)

        if (!mIsPageViewMatchLogged){
            mIsPageViewMatchLogged = true
            PixelApiHelper.logPixel(mActivity, mEnvironment, logPixelService, VdoKUtils.getPixelDto(packageName = mPackageName, pageUrl = "",
                tagName = mTagName, event = VdoEventNames.PAGE_VIEW_MATCH, eventDataDto= eventDataDto))
        }

        PixelApiHelper.logPixel(mActivity, mEnvironment, logPixelService, VdoKUtils.getPixelDto(packageName = mPackageName, pageUrl = "",
            tagName = mTagName, event = VdoEventNames.AD_MATCH, eventDataDto= eventDataDto))

        if (!isAdLoadedListener){
            mListener.onAdLoaded()
        }
    }

    protected fun eventAdImpression(){
        val eventDataDto = VdoKUtils.getEventData(0, adUnitItem?.partner, 0.5)

        PixelApiHelper.logPixel(mActivity, mEnvironment, logPixelService, VdoKUtils.getPixelDto(packageName = mPackageName, pageUrl = "",
            tagName = mTagName, event = VdoEventNames.VIEWABLE_IMPRESSION, eventDataDto= eventDataDto))

        mListener.onAdImpression()
    }

    protected fun eventAdDismiss(){
        PixelApiHelper.logPixel(mActivity, mEnvironment, logPixelService, VdoKUtils.getPixelDto(packageName = mPackageName, pageUrl = "",
            tagName = mTagName, event = VdoEventNames.CROSS_CLICKED))
        mListener.onAdDismissedFullScreenContent()
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