package vdo.ai.android.core.utils

import android.content.Context
import android.util.Log
import vdo.ai.android.core.models.ErrorLogDto
import vdo.ai.android.core.models.PixelDto
import vdo.ai.android.core.networking.RetrofitService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 *  created by Ashish Saini at 4th Oct 2023
 *
 **/
internal object PixelApiHelper {

    private val TAG = PixelApiHelper::class.java.simpleName

    fun logPixel(context: Context, environment: String, retrofitService: RetrofitService, pixelDto: PixelDto, listener: PixelApiListener? = null) {

        Log.d(TAG, "Build environment type :- $environment")
        if (environment.equals("release", true)) {

            CoroutineScope(Dispatchers.IO).launch {
                // Do the POST request and get response
                try {
                    pixelDto.domainName = context.packageName
                    val response = retrofitService.logPixel(pixelDto)
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            listener?.onSuccess()
                        } else {
                            listener?.onFailure("RETROFIT_ERROR"+ response.code().toString())
                        }
                    }
                }catch (e:Exception){
                    e.printStackTrace()
                    listener?.onFailure("RETROFIT_ERROR : ${e.message}")
                }
            }
        }

    }

    fun logError(environment: String, retrofitService: RetrofitService, errorDto: ErrorLogDto, listener: PixelApiListener? = null) {

        Log.d(TAG, "Build environment type :- $environment")
        if (environment.equals("release", true)) {

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = retrofitService.errorLog(errorDto.message, errorDto.tagName)
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            listener?.onSuccess()
                        } else {
                            listener?.onFailure("RETROFIT_ERROR"+ response.code().toString())
                        }
                    }
                }catch (e:Exception){
                    e.printStackTrace()
                    listener?.onFailure("RETROFIT_ERROR : ${e.message}")
                }
            }
        }
    }

    interface PixelApiListener {
      fun onSuccess()
      fun onFailure(message:String)
    }
}