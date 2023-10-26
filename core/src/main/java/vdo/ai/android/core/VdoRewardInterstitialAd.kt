package vdo.ai.android.core

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import vdo.ai.android.core.base.VdoBaseBuilder
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
 class VdoRewardInterstitialAd(builder : VdoRewardInterstitialAdBuilder) : RewardedInterstitialAdLoadCallback() {

    protected  val TAG = VdoRewardInterstitialAd::class.java.simpleName
    private val mActivity: Activity = builder.activity
    private val mListener: VdoRewardedListener = builder.mListener
    private val mEnvironment : String = builder.mEnvironment
    private val mTagName:String= builder.mTagName
    private val mPackageName : String = builder.activity.packageName
    private val tagConfigService = RetrofitHelper.getTagConfigServices(mActivity)
    private val logPixelService = RetrofitHelper.getLogPixelServices(mActivity)
    private val errorLogService = RetrofitHelper.getErrorLogServices(mActivity)
    private var tagConfigDto: GetTagConfigDto?=null
    private var adUnitItem: AdUnitsItem?= null
    private var rewardedInterstitialAd: RewardedInterstitialAd? = null
    private var isMediationAllowed :Boolean=builder.mIsMediationAllowed
    private var mIsPageViewLogged :Boolean = builder.mIsPageViewLogged
    private var mIsPageViewMatchLogged :Boolean = builder.mIsPageViewMatchLogged
    private var refreshAllowed :Boolean =builder.mRefreshAllowed

    init {
        PixelApiHelper.logPixel(mActivity, mEnvironment, logPixelService, VdoKUtils.getPixelDto(packageName = mPackageName, pageUrl = "", tagName = mTagName, event = VdoEventNames.LOADED))
        VdoManager.initializeAdsSdk(mActivity)
        getTagConfig()
    }

     class VdoRewardInterstitialAdBuilder() : VdoBaseBuilder(){
        lateinit var mListener: VdoRewardedListener

        fun withContext(activity: Activity): VdoRewardInterstitialAdBuilder {
            this.activity = activity
            return this
        }

        fun setEnvironment(environment: String): VdoRewardInterstitialAdBuilder {
            this.mEnvironment = environment
            return this
        }

        fun setTagName(tagName: String): VdoRewardInterstitialAdBuilder {
            this.mTagName = tagName
            return this
        }

        fun setAllowRefresh(refresh:Boolean) : VdoRewardInterstitialAdBuilder {
            this.mRefreshAllowed = refresh
            return this
        }

        fun setMediation(mediationFlag:Boolean): VdoRewardInterstitialAdBuilder {
            this.mIsMediationAllowed=mediationFlag
            return this
        }

        fun setListener(listener: VdoRewardedListener): VdoRewardInterstitialAdBuilder{
            this.mListener = listener
            return this
        }

        fun build(): VdoRewardInterstitialAd {
            return VdoRewardInterstitialAd(this)
        }
    }

    private fun getTagConfig() {
        ConfigApiHelper.getConfig(tagConfigService, mPackageName,
            mTagName, object : ConfigApiHelper.ConfigApiListener{

                override fun onSuccess(tagConfigDto: GetTagConfigDto) {
                    this@VdoRewardInterstitialAd.tagConfigDto = tagConfigDto
                    loadRewardInterstitialAd()
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
            loadRewardInterstitialAd()
        }
    }

    private fun loadRewardInterstitialAd() {
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

                        if (rewardedInterstitialAd == null) {
                            val adRequest = VdoManager.getAdManagerAdRequest()

                            RewardedInterstitialAd.load(mActivity, it, adRequest,this@VdoRewardInterstitialAd)

                        }else{
                            showRewardedInterstitialAd()
                        }
                    }
                }else{
                    PixelApiHelper.logPixel(mActivity, mEnvironment, logPixelService, VdoKUtils.getPixelDto(packageName = mPackageName, pageUrl = "", tagName = mTagName , event = VdoEventNames.BLOCK_APP, reason = tagConfigDto?.reason))
                }
            }else{
                Log.d(TAG, "Ads is not showing due to app is in background ")
                reloadRewardInterstitialAd(VdoKUtils.Ad_FAILED_REFRESH_TIME)
            }
        }catch (e:Exception){
            setErrorLog(e, ErrorFilterType.LOAD)
        }
    }
    override fun onAdFailedToLoad(adError: LoadAdError) {
        //                                super.onAdFailedToLoad(adError)
        Log.e(TAG, "GAM Ad failed to load $adError")
        rewardedInterstitialAd = null
        mListener.onAdFailedToLoad(VdoKUtils.getAdError(adError))
    }

    override fun onAdLoaded(rewardedAd: RewardedInterstitialAd) {
        super.onAdLoaded(rewardedAd)
        Log.d(TAG, "GAM Ad was loaded.")
        rewardedInterstitialAd = rewardedAd
        showRewardedInterstitialAd()
        mListener.onAdLoaded()
    }
    private fun showRewardedInterstitialAd() {
        try {
            if (rewardedInterstitialAd == null) {
                Log.e(TAG, "GAM rewarded interstitial ad wasn't ready yet.")
                return
            }

            rewardedInterstitialAd?.apply {
                fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        rewardedInterstitialAd = null
                        Log.d(TAG, "GAM Ad was dismissed.")

                        PixelApiHelper.logPixel(mActivity, mEnvironment, logPixelService, VdoKUtils.getPixelDto(packageName = mPackageName, pageUrl = "",
                            tagName = mTagName, event = VdoEventNames.CROSS_CLICKED))
                        mListener.onAdDismissedFullScreenContent()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {

                        rewardedInterstitialAd = null
                        Log.e(TAG, "GAM Ad failed to show full screen $adError")
                        mListener.onAdFailedToShowFullScreenContent(VdoKUtils.getAdError(adError))
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

    private fun eventAdLoaded(isImpressionAdListener:Boolean=false){
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


    private fun reloadRewardInterstitialAd(seconds:Long){
        if(refreshAllowed){
            VdoKUtils.getMyHandler().postDelayed(runnable, seconds * 1000)
        }
    }
    private val runnable:Runnable = Runnable {
        loadRewardInterstitialAd()
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