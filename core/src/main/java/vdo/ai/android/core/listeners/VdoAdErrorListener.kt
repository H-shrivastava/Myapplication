package vdo.ai.android.core.listeners

import com.google.android.gms.ads.AdError

/**
 *  created by Ashish Saini at 5th Oct 2023
 *
 **/
internal interface VdoAdErrorListener {

    fun setShowAdFromThirdParty(flag: Boolean)

    fun onVdoAdFailedToShowFullScreen(adError: AdError?)
}