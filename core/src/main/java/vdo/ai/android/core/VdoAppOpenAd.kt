package vdo.ai.android.core

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import vdo.ai.android.core.base.VdoBaseBuilder
import vdo.ai.android.core.listeners.OnShowAdCompleteListener
import vdo.ai.android.core.listeners.VdoAppOpenListener
import vdo.ai.android.core.manager.VdoAppOpenManager
import vdo.ai.android.core.manager.VdoManager
import vdo.ai.android.core.networking.RetrofitHelper
import vdo.ai.android.core.utils.*
import vdo.ai.android.core.utils.PixelApiHelper
import vdo.ai.android.core.utils.VdoEventNames

/**
 *  created by Ashish Saini at 6th Oct 2023
 *
 **/
class VdoAppOpenAd (builder : VdoAppOpenAdBuilder) : Application.ActivityLifecycleCallbacks {

    protected  val TAG = VdoAppOpenAd::class.java.simpleName
    protected  var mShouldAllowAppOpenMgr:Boolean =true
    private val mApplication:Application = builder.mApplication
    val mPackageName : String = mApplication.packageName
    val mEnvironment : String = builder.mEnvironment
    val mTagName : String = builder.mTagName
    val mContext: Context = mApplication.applicationContext
    val mListener : VdoAppOpenListener = builder.mListener
    internal val tagConfigService = RetrofitHelper.getTagConfigServices(mApplication.applicationContext )
    internal val logPixelService = RetrofitHelper.getLogPixelServices(mApplication.applicationContext)
    internal val errorLogService = RetrofitHelper.getErrorLogServices(mApplication.applicationContext )
    private var appOpenAdMgr: VdoAppOpenManager?=null
    private var currentActivity: Activity? = null

    init {
        initSdk()
    }

     class VdoAppOpenAdBuilder() : VdoBaseBuilder() {
        lateinit var mApplication:Application
        lateinit var mListener: VdoAppOpenListener

        fun withContext(application: Application): VdoAppOpenAdBuilder {
            this.mApplication = application
            return this
        }

        fun setEnvironment(environment: String): VdoAppOpenAdBuilder {
            this.mEnvironment = environment
            return this
        }

       fun setTagName(tagName: String): VdoAppOpenAdBuilder {
            this.mTagName = tagName
            return this
        }

        fun setListener(listener: VdoAppOpenListener): VdoAppOpenAdBuilder {
            this.mListener = listener
            return this
        }

        fun build(): VdoAppOpenAd {
            return VdoAppOpenAd(this)
        }
    }


    protected fun initSdk() {
        PixelApiHelper.logPixel(mContext, mEnvironment, logPixelService, VdoKUtils.getPixelDto(packageName = mPackageName, pageUrl = "", tagName = mTagName, event = VdoEventNames.LOADED))
        mApplication.registerActivityLifecycleCallbacks(this)

        VdoManager.initializeAdsSdk(mApplication)
        if (mShouldAllowAppOpenMgr){
            appOpenAdMgr = VdoAppOpenManager(this@VdoAppOpenAd)
        }
    }

    fun showAdIfAvailable(activity: Activity, onShowAdCompleteListener: OnShowAdCompleteListener) {
        // We wrap the showAdIfAvailable to enforce that other classes only interact with MyApplication
        // class.
        appOpenAdMgr?.showAdIfAvailable(activity, onShowAdCompleteListener)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {

    }

    override fun onActivityStarted(activity: Activity) {
        // An ad activity is started when an ad is showing, which could be AdActivity class from Google
        // SDK or another activity class implemented by a third party mediation partner. Updating the
        // currentActivity only when an ad is not showing will ensure it is not an ad activity, but the
        // one that shows the ad.
        appOpenAdMgr?.let {
            if (!it.isShowingAd) {
                it.activity = activity
                currentActivity = activity
            }
        }

    }

    override fun onActivityResumed(activity: Activity) {

    }

    override fun onActivityPaused(activity: Activity) {

    }

    override fun onActivityStopped(activity: Activity) {

    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {

    }

    override fun onActivityDestroyed(activity: Activity) {

    }



}