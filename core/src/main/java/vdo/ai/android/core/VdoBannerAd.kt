package vdo.ai.android.core

import android.app.Activity
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import com.google.ads.interactivemedia.v3.api.AdErrorEvent
import com.google.ads.interactivemedia.v3.api.AdEvent
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.ima.ImaAdsLoader
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.admanager.AdManagerAdView
import vdo.ai.android.core.base.VdoBaseBuilder
import vdo.ai.android.core.databinding.MediumRectanglePlayerLayoutBinding
import vdo.ai.android.core.listeners.VdoBannerAdListener
import vdo.ai.android.core.manager.VdoManager
import vdo.ai.android.core.models.AdUnitsItem
import vdo.ai.android.core.models.ErrorLogDto
import vdo.ai.android.core.models.GetTagConfigDto
import vdo.ai.android.core.models.VdoAdError
import vdo.ai.android.core.networking.RetrofitHelper
import vdo.ai.android.core.utils.*
/**
 *  created by Ashish Saini at 1st Feb 2023
 *
 **/
open class VdoBannerAd protected constructor(builder : VdoBannerAdBuilder) : AdListener() {

    protected open val TAG :String get() = VdoBannerAd::class.java.simpleName
    protected val mActivity : Activity = builder.activity
    private val mPackageName : String = builder.activity.packageName
    protected var mEnvironment : String = builder.mEnvironment
    private val mTagName : String = builder.mTagName
    protected val mAdSize : VdoAdSize = builder.mBannerAdSize
    var mAdContainer : ViewGroup? = builder.mAdContainer
    var mListener : VdoBannerAdListener = builder.mListener
    private val tagConfigService = RetrofitHelper.getTagConfigServices(mActivity)
    private val logPixelService = RetrofitHelper.getLogPixelServices(mActivity)
    private val errorLogService = RetrofitHelper.getErrorLogServices(mActivity)

    private var tagConfigDto: GetTagConfigDto?=null
    private var isReloadBannerAd = false
    private var adUnitItem:AdUnitsItem?= null
    internal var adManagerAdView : AdManagerAdView?= null
    var adDisplayType:AdType = AdType.GOOGLE_AD_MANAGER
    private var isMediationAllowed : Boolean = builder.mIsMediationAllowed
    internal var playerHandler:MediationPlayerHandler?= null
    private var mIsPageViewLogged = false
    private var mIsPageViewMatchLogged = false
    private var mRandomInt:Int?= builder.mRandomInt
    private var randomRefresh :Int?=null
    private var refreshAllowed :Boolean =builder.mRefreshAllowed

    init {
        PixelApiHelper.logPixel(mActivity, mEnvironment, logPixelService, VdoKUtils.getPixelDto(packageName = mPackageName, pageUrl = "", tagName = mTagName, event = VdoEventNames.LOADED))
        VdoManager.initializeAdsSdk(mActivity)
        setCurrentMediationIdentifier()
        getTagConfig()
    }

    open class VdoBannerAdBuilder() :VdoBaseBuilder(){
        var mAdContainer: ViewGroup?=null
        lateinit var mListener: VdoBannerAdListener

        open fun withContext(activity: Activity): VdoBannerAdBuilder{
            this.activity = activity
            return this
        }

        open fun setEnvironment(environment: String): VdoBannerAdBuilder {
            this.mEnvironment = environment
            return this
        }

        open fun setBannerView( adContainer: ViewGroup?): VdoBannerAdBuilder {
            this.mAdContainer = adContainer
            return this
        }

        open fun setTagName(tagName: String): VdoBannerAdBuilder {
            this.mTagName = tagName
            return this
        }

        open fun setAllowRefresh(refresh:Boolean) :VdoBannerAdBuilder{
            this.mRefreshAllowed = refresh
            return this
        }

        open fun setAddSize(adSize: VdoAdSize): VdoBannerAdBuilder {
            this.mBannerAdSize = adSize
            return this
        }

        open fun setListener(listener: VdoBannerAdListener): VdoBannerAdBuilder {
            this.mListener = listener
            return this
        }

        open fun setMediation(mediationFlag:Boolean, random:Int?):VdoBannerAdBuilder{
            this.mIsMediationAllowed=mediationFlag
            this.mRandomInt=random
            return this
        }

        open fun build(): VdoBannerAd? {
            return VdoBannerAd(this)
        }
    }

    private fun getTagConfig() {
        ConfigApiHelper.getConfig(tagConfigService, mPackageName,
            mTagName, object : ConfigApiHelper.ConfigApiListener{

                override fun onSuccess(tagConfigDto: GetTagConfigDto) {
                    this@VdoBannerAd.tagConfigDto = tagConfigDto
                    loadBannerAd()
                }

                override fun onFailure(code: Int, errorMessage: String?) {
                    Log.e(TAG, "onFailure >>>>> $errorMessage")
                    setErrorLog(null, ErrorFilterType.API_FAILURE, errorMessage)
                    val adError = VdoAdError(code,"", errorMessage,0, "", FailureType.API)
                    mListener.onAdFailedToLoad(adError)
                }
            })
    }

    internal class MediationPlayerHandler(val bannerAd: VdoBannerAd) : Player.Listener {

        private val TAG :String= "MediationAdView"
        private lateinit var binding: MediumRectanglePlayerLayoutBinding

        private var mExoPlayer: ExoPlayer?= null
        private var mImaAdsLoader: ImaAdsLoader?= null
        var isVideoAdLoaded = false
        var isViewabledImpressionListenerCalled = false
        var imaAdErrorOrAdBreakFetch = false

        val mVastTagUrl : String by lazy {
            if (bannerAd.mEnvironment.equals("release", true)){
                "https://a.vdo.ai/core/${bannerAd.mTagName}/vmap"
            }else{
                "https://a.vdo.ai/core/test/vmap.xml"
            }
        }

        init {
            binding = MediumRectanglePlayerLayoutBinding.inflate(LayoutInflater.from(bannerAd.mAdContainer!!.context))
            mImaAdsLoader = initializeImaAdsLoader()
            mExoPlayer = initializeExoPlayer()
            setPlayer()
        }

        private fun initializeImaAdsLoader(): ImaAdsLoader {
            return ImaAdsLoader.Builder(bannerAd.mAdContainer!!.context)
                .setAdEventListener(buildAdEventListener())
                .setAdErrorListener(buildAdErrorListener())
                .build()
        }

        private fun buildAdEventListener(): AdEvent.AdEventListener {

            val imaAdEventListener = AdEvent.AdEventListener { adEvent ->
                val eventType = adEvent.type

                if (eventType == AdEvent.AdEventType.AD_PROGRESS) {
                    return@AdEventListener
                }
                Log.d(TAG, "IMA event: $eventType")

                if(eventType == AdEvent.AdEventType.LOADED){
                    if (!isVideoAdLoaded){
                        isVideoAdLoaded = true
                        Log.d(TAG, "view added when AdEvent.AdEventType.LOADED called  ")
                        bannerAd.mAdContainer?.addView(binding.root)

                        bannerAd.eventAdLoaded()
                        if (bannerAd.isMediationAllowed){
                            bannerAd.mListener.onMediationSuccess()
                        }
                    }else{
                        val eventDataDto = VdoKUtils.getEventData(0, bannerAd.adUnitItem?.partner, 0.5, VdoKUtils.TYPE_BANNER)
                        PixelApiHelper.logPixel(bannerAd.mActivity, bannerAd.mEnvironment, bannerAd.logPixelService, VdoKUtils.getPixelDto(packageName = bannerAd.mPackageName, pageUrl = "",
                            tagName = bannerAd.mTagName, event = VdoEventNames.AD_MATCH, eventDataDto= eventDataDto))
                    }
                    viewableImpression()
                } else if (eventType == AdEvent.AdEventType.AD_BREAK_FETCH_ERROR){
                    imaAdErrorOrAdBreakFetch = true
                }else if(eventType == AdEvent.AdEventType.ALL_ADS_COMPLETED){
                    releasePlayer()
                }
            }
            return imaAdEventListener
        }

        private fun buildAdErrorListener(): AdErrorEvent.AdErrorListener {

            val imaAdErrorListener= AdErrorEvent.AdErrorListener { adErrorEvent ->
                Log.d(TAG, "IMA error event: $adErrorEvent")
                imaAdErrorOrAdBreakFetch = true
                releasePlayer()
            }
            return imaAdErrorListener
        }

        private fun initializeExoPlayer(): ExoPlayer {
            val maxWidth = 300.toPx(bannerAd.mActivity) //resources.getDimension(R.dimen.floating_video_max_window_width).toInt()
            val maxHeight = 250.toPx(bannerAd.mActivity) //resources.getDimension(R.dimen.floating_video_max_window_height).toInt()

            val trackSelector = DefaultTrackSelector(bannerAd.mActivity,
                DefaultTrackSelector.Parameters.Builder(bannerAd.mActivity)
                    .setMaxVideoSize(maxWidth, maxHeight)
                    .build())

            val dataSourceFactory: DataSource.Factory = DefaultDataSource.Factory(bannerAd.mActivity)
            val mediaSourceFactory: MediaSource.Factory = DefaultMediaSourceFactory(dataSourceFactory)
                .setLocalAdInsertionComponents({
                        unusedAdTagUri: MediaItem.AdsConfiguration? -> mImaAdsLoader
                }, binding.player)

            return ExoPlayer.Builder(bannerAd.mActivity)
                .setTrackSelector(trackSelector)
                .setMediaSourceFactory(mediaSourceFactory)
                .build()
        }

        private fun setPlayer() {
            binding.player.player = mExoPlayer
            mExoPlayer?.addListener(this)

            binding.stopBtn.setOnClickListener {
                releasePlayer()
            }

            binding.player.setControllerVisibilityListener(
                StyledPlayerView.ControllerVisibilityListener { visibility ->
                    binding.stopBtn.visibility = visibility
                }
            )
        }

        fun showVideoAds(videoUrl:String, vastTagUrl:String) {

            binding.player.player = mExoPlayer
            mImaAdsLoader?.setPlayer(mExoPlayer)

            // Create the MediaItem to play, specifying the content URI and ad tag URI.
            val contentUri = Uri.parse(videoUrl)
            val adTagUri = Uri.parse(vastTagUrl)
            val mediaItem = MediaItem.Builder()
                .setUri(contentUri)
                .setAdsConfiguration(
                    MediaItem.AdsConfiguration
                        .Builder(adTagUri).build())
                .build()

            mExoPlayer?.let {
                it.stop()
                it.setMediaItem(mediaItem)
                it.prepare()
                it.volume = 0f
                it.playWhenReady = true
            }
        }

        fun releasePlayer(isDestroy:Boolean= false) {
            try {
                mImaAdsLoader?.setPlayer(null)
                if (this::binding.isInitialized){
                    binding.player.player = null
                }
                if (mExoPlayer!= null){
                    mExoPlayer?.release()
                    mExoPlayer = null
                }

                if (!isDestroy){
                    if (imaAdErrorOrAdBreakFetch){
                        bannerAd.loadBannerAd()
                    }else if (isVideoAdLoaded){
                        Log.d(TAG, "ALL_ADS_COMPLETED called player remove from container")
                        bannerAd.mAdContainer?.removeAllViews()
                    }
                }
            }catch (e:Exception){
                bannerAd.setErrorLog(e, ErrorFilterType.RELEASE_PLAYER_FAILURE)
                bannerAd.mAdContainer?.removeAllViews()
                bannerAd.loadBannerAd()
            }
        }

        fun viewableImpression(){
            val mainHandler = Handler(Looper.getMainLooper())
            mainHandler.postDelayed({
                Log.d(TAG,"ViewableImpression after ad loaded 2 seconds")
                bannerAd.eventViewableImpression()
                if (!isViewabledImpressionListenerCalled){
                    isViewabledImpressionListenerCalled = true
                    bannerAd.mListener.onAdImpression()
                }
            }, 2000)
        }
    }

    @Synchronized
    private fun setCurrentMediationIdentifier(){
        if(isMediationAllowed){
//            Log.d("BannerAdView", "old current value = ${Z1KUtils.currentMediationIdentifyer}")
            VdoKUtils.currentMediationIdentifyer = mRandomInt
//            Log.d("BannerAdView", "latest current value = ${Z1KUtils.currentMediationIdentifyer}")
        }
    }

    @Synchronized
    private fun killMediationRequest(){
        if (isMediationAllowed && mRandomInt != null){

            if (VdoKUtils.currentMediationIdentifyer != mRandomInt){
                Log.d("BannerAdView", "destroying........$mRandomInt.\n")
                VdoKUtils.destroyBanner(this@VdoBannerAd)
//                mAdContainer = null
//                mListener.onMediationDestroy()
                return
            }else{
                Log.d("BannerAdView", "current and random is equal.........\n")
            }

//            if (Z1KUtils.currentMediationIdentifyer != mRandomInt){
//                Z1KUtils.previousMap?.put(mRandomInt!!, Z1KUtils.currentMediationIdentifyer!!)
//                Log.d("BannerAdView", "updated previousMap .........\n")
//            }else{
//                Log.d("BannerAdView", "current and random is equal.........\n")
//            }
//
//            Z1KUtils.previousMap?.apply {
//                if (isNullOrEmpty().not() && contains(mRandomInt)){
//                    Log.e("BannerAdView", "onMediationRemovedFromStack().......$mRandomInt")
//                    mAdContainer = null
//                    mListener.onMediationDestroy()
//                    return
//                }
//            }
        }
    }

    fun ifShouldAllowLoadAd(){
        if (isMediationAllowed){
            removeHandler()
            loadBannerAd()
        }
    }

    private fun loadBannerAd() {

        try {
            if (tagConfigDto == null || mAdContainer ==null)
                return

            killMediationRequest()

            adDisplayType = AdType.GOOGLE_AD_MANAGER

            if (VdoKUtils.isAppInForegrounded() && mAdContainer != null){

                if (!VdoKUtils.isConfigAllowed(tagConfigDto)){

                    adUnitItem = tagConfigDto?.adunits?.get(0)
                    adUnitItem?.adUrl?.let {

                        eventPageView()
                        if (mAdSize == VdoAdSize.MEDIUM_RECTANGLE &&  (playerHandler == null || !playerHandler!!.imaAdErrorOrAdBreakFetch)){
                            playerHandler = MediationPlayerHandler(this).apply {
                                showVideoAds(tagConfigDto?.mediaFile?:"", this.mVastTagUrl)
                            }
                        }else {
                            playerHandler = null
                            mAdContainer?.removeAllViews()
                            adManagerAdView = AdManagerAdView(mActivity)
                            adManagerAdView?.apply {
                                setAdSize(mAdSize.adSize)
                                adUnitId = it

                                val adRequest = VdoManager.getAdManagerAdRequest()
                                loadAd(adRequest)
                                mAdContainer?.addView(adManagerAdView)
                                adListener = this@VdoBannerAd

                            }
                        }
                    }
                }else{
                    PixelApiHelper.logPixel(mActivity, mEnvironment, logPixelService, VdoKUtils.getPixelDto(packageName = mPackageName, pageUrl = "", tagName = mTagName , event = VdoEventNames.BLOCK_APP, reason = tagConfigDto?.reason))
                }
            }else{
                if (!isMediationAllowed){
                    Log.d(TAG, "Ads is not showing due to app is in background ")
                    reloadBannerAd(VdoKUtils.Ad_FAILED_REFRESH_TIME)
                }
            }
        }catch (e:Exception){
            setErrorLog(e, ErrorFilterType.LOAD)
        }
    }

    override fun onAdLoaded() {
        Log.d(TAG, "GAM Ad was loaded")
        mListener.onAdLoaded()
    }

    override fun onAdImpression() {
        Log.d(TAG, "GAM Ad was impression")
        eventAdLoaded(true)
        eventAdImpression()
    }

    override fun onAdClicked() {
        Log.d(TAG, "GAM Ad was clicked.")
        mListener.onAdClicked()
    }

    override fun onAdClosed() {
        Log.d(TAG, "GAM Ad was closed.")
        mListener.onAdClosed()
    }

    override fun onAdFailedToLoad(adError: LoadAdError) {
        Log.e(TAG, "GAM Ad was failed to load. $adError")
        reloadBannerAd(VdoKUtils.Ad_FAILED_REFRESH_TIME)
        mListener.onAdFailedToLoad(VdoKUtils.getAdError(adError))
    }

    override fun onAdOpened() {
        Log.d(TAG, "GAM Ad was opened")
        mListener.onAdOpened()
    }

    protected fun reloadBannerAd(seconds:Long){
        if(refreshAllowed){
            isReloadBannerAd = true
            VdoKUtils.getMyHandler().postDelayed(runnable, seconds * 1000)
        }
    }

    internal val runnable:Runnable = Runnable {
        loadBannerAd()
    }

    fun removeHandler(){
        VdoKUtils.getMyHandler().removeCallbacks(runnable)
    }

//    open fun onResume(activity:Activity){
//        adManagerAdView?.resume()
//    }
//
//    open fun onPause(activity: Activity){
//        adManagerAdView?.pause()
//    }

    private fun eventPageView(){
        if(!mIsPageViewLogged){
            mIsPageViewLogged = true
            PixelApiHelper.logPixel(mActivity, mEnvironment, logPixelService, VdoKUtils.getPixelDto(packageName = mPackageName, pageUrl = "", tagName = mTagName, event = VdoEventNames.PAGE_VIEW))
        }
    }

    private fun eventViewableImpression(){
        val eventDataDto = VdoKUtils.getEventData(0, adUnitItem?.partner, 0.5)
        PixelApiHelper.logPixel(mActivity, mEnvironment, logPixelService, VdoKUtils.getPixelDto(packageName = mPackageName, pageUrl = "",
            tagName = mTagName, event = VdoEventNames.VIEWABLE_IMPRESSION, eventDataDto= eventDataDto))
    }

    protected fun eventAdLoaded(isImpressionAdListener:Boolean=false){

        val eventDataDto = VdoKUtils.getEventData(0, adUnitItem?.partner, 0.5, VdoKUtils.TYPE_BANNER)
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

    protected fun eventAdImpression(){
        eventViewableImpression()
        mListener.onAdImpression()

        if (adDisplayType == AdType.GOOGLE_AD_MANAGER || adDisplayType == AdType.APPLOVIN){
            val refreshTime = if (isMediationAllowed) VdoKUtils.AD_REFRESH_MEDIATION else VdoKUtils.AD_REFRESH
            reloadBannerAd(refreshTime)
        }
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

}