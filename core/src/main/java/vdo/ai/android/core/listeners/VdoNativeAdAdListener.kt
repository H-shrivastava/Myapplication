package vdo.ai.android.core.listeners

import com.google.android.gms.ads.nativead.NativeAd

/**
 *  created by Ashish Saini at 6th Oct 2023
 *
 **/
interface VdoNativeAdAdListener : VdoNativeTemplateAdListener{
    override fun forNativeAd(nativeAd: NativeAd){}
}