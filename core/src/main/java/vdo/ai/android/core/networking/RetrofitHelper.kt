package vdo.ai.android.core.networking

import android.content.Context
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 *  created by Ashish Saini at 4th Oct 2023
 *
 **/
object RetrofitHelper {

    private const val PIXEL_URL = "https://analytics1.vdo.ai/"
    private const val TAG_CONFIG_URL = "https://targetingv1.vdo.ai/"
    private const val ERROR_URL= "https://a.vdo.ai/"

    private fun getOkHttpClient(context: Context): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(UserAgentInterceptor(context))
            .build()
    }

    @JvmStatic
    fun getTagConfigServices(context: Context) : RetrofitService {

        val retrofit = Retrofit.Builder()
            .client(getOkHttpClient(context))
            .baseUrl(TAG_CONFIG_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(RetrofitService::class.java)
    }

    @JvmStatic
    fun getLogPixelServices(context: Context) : RetrofitService {
        val retrofit = Retrofit.Builder()
            .client(getOkHttpClient(context))
            .baseUrl(PIXEL_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(RetrofitService::class.java)
    }

    @JvmStatic
    fun getErrorLogServices(context: Context) : RetrofitService {
        val retrofit = Retrofit.Builder()
            .client(getOkHttpClient(context))
            .baseUrl(ERROR_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(RetrofitService::class.java)
    }

}