package vdo.ai.android.core.listeners

import vdo.ai.android.core.models.VdoAdError


/**
 *  created by Ashish Saini at 4th Oct 2023
 *
 **/
interface VdoBannerAdListener {

    fun onAdImpression()

    fun onAdLoaded()

    fun onAdFailedToLoad(adError: VdoAdError?)

    fun onAdClicked()

    fun onAdOpened()

    fun onAdClosed()

    fun onMediationSuccess(){}

//    fun onMediationDestroy(){}

}