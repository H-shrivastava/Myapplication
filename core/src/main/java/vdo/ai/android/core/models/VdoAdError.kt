package vdo.ai.android.core.models

import vdo.ai.android.core.utils.FailureType


/**
 *  created by Ashish Saini at 4th Oct 2023
 *
 **/
data class VdoAdError(val code :Int?=0, val domain:String?="", val message:String?="",
                      val mediatedNetworkErrorCode :Int=0, val mediatedNetworkErrorMessage:String?="",
                      val failureType: FailureType
)


