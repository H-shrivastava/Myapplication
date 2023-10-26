package vdo.ai.android.core

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import vdo.ai.android.core.base.VdoBaseBuilder
import vdo.ai.android.core.listeners.VdoAdErrorListener
import vdo.ai.android.core.listeners.VdoRewardedListener
import vdo.ai.android.core.manager.VdoManager
import vdo.ai.android.core.models.AdUnitsItem
import vdo.ai.android.core.models.ErrorLogDto
import vdo.ai.android.core.models.GetTagConfigDto
import vdo.ai.android.core.models.VdoAdError
import vdo.ai.android.core.networking.RetrofitHelper
import vdo.ai.android.core.utils.*
import vdo.ai.android.core.utils.ConfigApiHelper
import vdo.ai.android.core.utils.PixelApiHelper
import vdo.ai.android.core.utils.VdoEventNames

/**
 *  created by Ashish Saini at 5th Oct 2023
 *
 **/
open class VdoRewardedVideoAd(builder : VdoRewardedVideoAdBuilder): RewardedAdLoadCallback(), VdoAdErrorListener {

    protected open val TAG = VdoRewardedVideoAd::class.java.simpleName
    protected val mActivity: Activity = builder.activity
    protected val mListener: VdoRewardedListener = builder.mListener
    protected val mEnvironment : String = builder.mEnvironment
    private val mTagName:String= builder.mTagName
    private val mPackageName : String = builder.activity.packageName
    private val tagConfigService = RetrofitHelper.getTagConfigServices(mActivity)
    private val logPixelService = RetrofitHelper.getLogPixelServices(mActivity)
    private val errorLogService = RetrofitHelper.getErrorLogServices(mActivity)
    private var tagConfigDto: GetTagConfigDto?=null
    private var rewardedAd: RewardedAd? = null
    private var adUnitItem: AdUnitsItem?= null
    private var isMediationIsAllowed: Boolean =builder.mIsMediationAllowed
    private var mIsPageViewLogged :Boolean = builder.mIsPageViewLogged
    private var mIsPageViewMatchLogged :Boolean = builder.mIsPageViewMatchLogged
    private var refreshAllowed :Boolean =builder.mRefreshAllowed
    private var isShowAdFromThirdParty :Boolean = false

    init {
        PixelApiHelper.logPixel(mActivity, mEnvironment, logPixelService, VdoKUtils.getPixelDto(packageName = mPackageName, pageUrl = "", tagName = mTagName, event = VdoEventNames.LOADED))
        VdoManager.initializeAdsSdk(mActivity)
        getTagConfig()
    }

    open class VdoRewardedVideoAdBuilder()  : VdoBaseBuilder(){
        lateinit var mListener: VdoRewardedListener

        open fun withContext(activity: Activity): VdoRewardedVideoAdBuilder {
            this.activity = activity
            return this
        }

        open fun setEnvironment(environment: String): VdoRewardedVideoAdBuilder {
            this.mEnvironment = environment
            return this
        }

        open fun setTagName(tagName: String): VdoRewardedVideoAdBuilder {
            this.mTagName = tagName
            return this
        }

        open fun setAllowRefresh(refresh:Boolean) : VdoRewardedVideoAdBuilder {
            this.mRefreshAllowed = refresh
            return this
        }

        open fun setMediation(mediationFlag:Boolean): VdoRewardedVideoAdBuilder {
            this.mIsMediationAllowed=mediationFlag
            return this
        }

        open fun setListener(listener: VdoRewardedListener): VdoRewardedVideoAdBuilder{
            this.mListener = listener
            return this
        }

        open fun build(): VdoRewardedVideoAd {
            return VdoRewardedVideoAd(this)
        }
    }

    private fun getTagConfig() {
        ConfigApiHelper.getConfig(tagConfigService, mPackageName,
            mTagName, object : ConfigApiHelper.ConfigApiListener{

                override fun onSuccess(tagConfigDto: GetTagConfigDto) {
                    this@VdoRewardedVideoAd.tagConfigDto = tagConfigDto
                    loadRewardVideoAd()
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
            loadRewardVideoAd()
        }
    }

    private fun loadRewardVideoAd() {
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

                        if (rewardedAd == null) {
                            val adRequest = VdoManager.getAdManagerAdRequest()

                            RewardedAd.load(mActivity, it, adRequest, this@VdoRewardedVideoAd)

                        }else{
                            showRewardedVideoAd()
                        }
                    }
                }else{
                    PixelApiHelper.logPixel(mActivity, mEnvironment, logPixelService, VdoKUtils.getPixelDto(packageName = mPackageName, pageUrl = "", tagName = mTagName , event = VdoEventNames.BLOCK_APP, reason = tagConfigDto?.reason))
                }
            }else{
                Log.d(TAG, "Ads is not showing due to app is in background ")
                reloadRewardVideoAd(VdoKUtils.Ad_FAILED_REFRESH_TIME)
            }
        }catch (e:Exception){
            setErrorLog(e, ErrorFilterType.LOAD)
        }
    }

    override fun onAdLoaded(ad: RewardedAd) {
        Log.d(TAG, "GAM Ad was loaded.")
        rewardedAd = ad
        this@VdoRewardedVideoAd.setShowAdFromThirdParty(false)
        showRewardedVideoAd()
        mListener.onAdLoaded()
    }

    override fun onAdFailedToLoad(adError: LoadAdError) {
        Log.e(TAG, "GAM Ad failed to load $adError")
        rewardedAd = null
        this@VdoRewardedVideoAd.setShowAdFromThirdParty(false)
        reloadRewardVideoAd(VdoKUtils.Ad_FAILED_REFRESH_TIME)
        mListener.onAdFailedToLoad(VdoKUtils.getAdError(adError))
    }

    override fun setShowAdFromThirdParty(flag: Boolean) {
        isShowAdFromThirdParty = flag
    }

    override fun onVdoAdFailedToShowFullScreen(adError: AdError?) {

    }

    private fun showRewardedVideoAd() {
        try {
            if (rewardedAd == null) {
                Log.e(TAG, "GAM rewarded interstitial ad wasn't ready yet.")
                return
            }

            rewardedAd?.apply {
                fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        this@VdoRewardedVideoAd.rewardedAd = null
                        Log.d(TAG, "GAM Ad was dismissed.")
                        eventAdDismiss()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        Log.e(TAG, "GAM Ad failed to show full screen $adError")
                        this@VdoRewardedVideoAd.rewardedAd = null

                        if (isShowAdFromThirdParty){
                            this@VdoRewardedVideoAd.onVdoAdFailedToShowFullScreen(adError)
                        }else{
                            mListener.onAdFailedToShowFullScreenContent(VdoKUtils.getAdError(adError))
                        }
                    }

                    override fun onAdShowedFullScreenContent() {
                        Log.d(TAG, "GAM Ad show full screen")
                        mListener.onAdShowedFullScreenContent()
                    }
                    override fun onAdImpression() {
                        super.onAdImpression()
                        Log.d(TAG, "GAM Ad impression")
                        eventAdImpression()
                        eventAdLoaded(true)
                    }
                }

                show(mActivity) { rewardItem ->
                    Log.d(TAG, "GAM User earned the reward. amount ${rewardItem.amount},  type : ${rewardItem.type}")
                    mListener.onUserEarnedReward(rewardItem.amount, rewardItem.type)
                }
            }
        }catch (e:Exception){
            setErrorLog(e, ErrorFilterType.SHOW)
        }
    }

    protected fun reloadRewardVideoAd(seconds:Long){
        if(refreshAllowed){
            VdoKUtils.getMyHandler().postDelayed(runnable, seconds * 1000)
        }
    }

    private val runnable:Runnable = Runnable {
        loadRewardVideoAd()
    }

    protected fun eventAdLoaded(isImpressionAdListener:Boolean=false){
        val eventDataDto = VdoKUtils.getEventData(0, adUnitItem?.partner, 0.5, VdoKUtils.TYPE_VIDEO)

        if (!mIsPageViewMatchLogged){
            mIsPageViewMatchLogged = true
            PixelApiHelper.logPixel(mActivity, mEnvironment, logPixelService, VdoKUtils.getPixelDto(packageName = mPackageName, pageUrl = "",
                tagName = mTagName, event = VdoEventNames.PAGE_VIEW_MATCH, eventDataDto= eventDataDto))
        }

        PixelApiHelper.logPixel(mActivity, mEnvironment, logPixelService, VdoKUtils.getPixelDto(packageName = mPackageName, pageUrl = "",
            tagName = mTagName, event = VdoEventNames.AD_MATCH, eventDataDto= eventDataDto))

        if (!isImpressionAdListener){
            mListener.onAdLoaded()
        }
    }

    private fun eventAdImpression(){
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
        PixelApiHelper.logError(mEnvironment, errorLogService, ErrorLogDto(message, mTagName))
        PixelApiHelper.logPixel(mActivity, mEnvironment, logPixelService, VdoKUtils.getPixelDto(packageName = mPackageName, pageUrl = "",
            tagName = mTagName , event = VdoEventNames.ERROR, errorCode = errorType.code))
    }

    fun destroy(){
        VdoKUtils.getMyHandler().removeCallbacks(runnable)
    }
}