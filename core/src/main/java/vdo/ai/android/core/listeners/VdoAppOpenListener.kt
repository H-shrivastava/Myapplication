package vdo.ai.android.core.listeners

import vdo.ai.android.core.models.VdoAdError

/**
 *  created by Ashish Saini at 6th Oct 2023
 */
interface VdoAppOpenListener {

    fun onAdLoaded()

    fun onAdFailedToLoad(adError: VdoAdError?)

    fun onAdDismissedFullScreenContent()

    fun onAdShowedFullScreenContent()

    fun onAdFailedToShowFullScreenContent(adError: VdoAdError?)


}