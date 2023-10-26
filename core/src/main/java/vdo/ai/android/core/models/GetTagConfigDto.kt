package vdo.ai.android.core.models

import com.google.gson.annotations.SerializedName

/**
 *  created by Ashish Saini at 4th Oct 2023
 *
 **/
data class GetTagConfigDto(

	@field:SerializedName("reason")
	val reason: String? = null,

	@field:SerializedName("country")
	val country: String? = null,

	@field:SerializedName("r")
	val r: String? = null,

	@field:SerializedName("city")
	val city: String? = null,

	@field:SerializedName("allowed")
	val allowed: String? = null,

	@field:SerializedName("tagType")
	val tagType: String? = null,

	@field:SerializedName("adunits")
	val adunits: List<AdUnitsItem?>? = null,

	@field:SerializedName("gdpr_check")
	val gdprCheck: String? = null,

	@field:SerializedName("state")
	val state: String? = null,

	@field:SerializedName("ip_address")
	val ipAddress: String? = null,

	@field:SerializedName("package_id")
	val packageId: String? = null,

	@field:SerializedName("media_file")
	val mediaFile: String? = null,

	@field:SerializedName("platform")
	val platform: String? = null
)

data class AdUnitsItem(

	@field:SerializedName("partner")
	val partner: String? = null,

	@field:SerializedName("ad_url")
    var adUrl: String? = null,

	@field:SerializedName("app_id")
	val appId: String? = null
)
