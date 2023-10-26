package vdo.ai.android.core.base

import android.app.Activity
import vdo.ai.android.core.utils.VdoAdSize

/**
 *  created by Ashish Saini at 4th Oct 2023
 *
 **/
abstract class VdoBaseBuilder() {

    lateinit var activity: Activity
    lateinit var mEnvironment: String
    lateinit var mTagName:String
    lateinit var mBannerAdSize: VdoAdSize

    var mIsMediationAllowed:Boolean = false
    var mIsPageViewLogged : Boolean =false
    var mIsPageViewMatchLogged :Boolean =false
    var mRandomInt:Int?= null
    var mRefreshAllowed : Boolean=true

}