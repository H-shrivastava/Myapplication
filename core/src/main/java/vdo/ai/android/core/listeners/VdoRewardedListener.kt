package vdo.ai.android.core.listeners

/**
 *  created by Ashish Saini at 5th Oct 2023
 *
 **/
interface VdoRewardedListener : VdoInterstitialListener {
    fun onUserEarnedReward(amount: Int, type: String)
}