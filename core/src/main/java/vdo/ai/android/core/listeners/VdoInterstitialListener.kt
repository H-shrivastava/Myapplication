package vdo.ai.android.core.listeners

import vdo.ai.android.core.models.VdoAdError

/**
 *  created by Ashish Saini at 5th Oct 2023
 *
 **/
interface VdoInterstitialListener {

    fun onAdLoaded()

    fun onAdImpression()

    fun onAdFailedToLoad(adError: VdoAdError?)

    fun onAdClicked()

    fun onAdDismissedFullScreenContent()

    fun onAdShowedFullScreenContent()

    fun onAdFailedToShowFullScreenContent(adError: VdoAdError?)


}
