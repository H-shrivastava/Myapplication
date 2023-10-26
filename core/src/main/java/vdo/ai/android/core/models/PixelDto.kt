package vdo.ai.android.core.models

import com.google.gson.annotations.SerializedName

/**
 *  created by Ashish Saini at 4th Oct 2023
 *
 **/
data class PixelDto(

	@field:SerializedName("domainName")
	var domainName: String? = null,

	@field:SerializedName("page_url")
	var pageUrl: String? = null,

	@field:SerializedName("tagName")
	var tagName: String? = null,

	@field:SerializedName("event")
	var event: String? = null,

	@field:SerializedName("eventData")
	var eventData: Any? = null,

	@field:SerializedName("uid")
	var uid: String? = null
)

data class EventDto(

	@field:SerializedName("offset")
	var offset: Int? = null,

	@field:SerializedName("partner")
	var partner: String? = null,

	@field:SerializedName("cpm")
	var cpm: Double? = null,

	@field:SerializedName("type")
	var type: String? = null


)
