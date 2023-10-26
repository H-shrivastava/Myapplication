package vdo.ai.android.core.utils

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vdo.ai.android.core.models.GetTagConfigDto
import vdo.ai.android.core.networking.NetworkState
import vdo.ai.android.core.networking.RetrofitService
import vdo.ai.android.core.networking.parseResponse

/**
 *  created by Ashish Saini at 4th Oct 2023
 *
 **/
internal object ConfigApiHelper {

    fun getConfig(retrofitService: RetrofitService, packageName: String, tagName:String, listener: ConfigApiListener? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = retrofitService.configApi(packageName, tagName, VdoKUtils.PLATFORM)
                withContext(Dispatchers.Main) {
                    when (val result = response.parseResponse()) {
                        is NetworkState.Success -> {
                            listener?.onSuccess(result.data)
                        }
                        is NetworkState.Error -> {

                            VdoKUtils.getMyHandler().post {
                                listener?.onFailure(result.response?.code()?:0,result.response?.message()?:"")
                            }
                        }
                    }
                }
            } catch(e:Exception){
                e.printStackTrace()
                var msg = e.message
                if (msg.isNullOrEmpty()) {
                    msg = Log.getStackTraceString(e)
                }
                VdoKUtils.getMyHandler().post {
                    listener?.onFailure(0, msg)
                }
            }
        }
    }

    interface ConfigApiListener {
        fun onSuccess(tagConfigDto: GetTagConfigDto)
        fun onFailure(code:Int, errorMessage:String?)
    }
}