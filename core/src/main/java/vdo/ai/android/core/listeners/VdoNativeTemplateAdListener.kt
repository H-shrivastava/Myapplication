package vdo.ai.android.core.listeners

import com.google.android.gms.ads.nativead.NativeAd
import vdo.ai.android.core.models.VdoAdError

/**
 *  created by Ashish Saini at 6th Oct 2023
 *
 **/
interface VdoNativeTemplateAdListener {

    fun forNativeAd(nativeAd:NativeAd){}

    fun onAdLoaded()

    fun onAdFailedToLoad(adError: VdoAdError?)

    fun onAdImpression()

    fun onAdClosed()

    fun onAdClicked()

}