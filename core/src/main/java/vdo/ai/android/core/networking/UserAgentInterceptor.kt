package vdo.ai.android.core.networking

import android.content.Context
import java.io.IOException
import okhttp3.Interceptor
import okhttp3.Interceptor.Chain
import okhttp3.Request
import okhttp3.Response
import vdo.ai.android.core.utils.VdoKUtils

/**
 *  created by Ashish Saini at 4th Oct 2023
 *
 **/
/* This interceptor adds a custom User-Agent. */
class UserAgentInterceptor(private val context: Context) : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Chain): Response {
//        val unsafeHeader: Headers = Headers.Builder()
//            .addUnsafeNonAscii("User-Agent", Z1KUtils.getUserAgent(context))
//            .build()
        val originalRequest: Request = chain.request()
        val requestWithUserAgent = originalRequest.newBuilder()
            .header("User-Agent", VdoKUtils.getUserAgent(context))
            .build()
        return chain.proceed(requestWithUserAgent)
    }

    private fun getSafeValue(value: String, defaultValue: String): String {
        for (i in value.indices) {
            val c = value[i]
            if ((c == '\t' || c in '\u0020'..'\u007e').not()) {
                return defaultValue
            }
        }
        return value
    }

}