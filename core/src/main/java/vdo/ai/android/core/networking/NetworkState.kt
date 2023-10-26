package vdo.ai.android.core.networking

import retrofit2.Response

/**
 *  created by Ashish Saini at 4th Oct 2023
 *
 **/
sealed class NetworkState<out T> {
    data class Success<out T>(val data: T): NetworkState<T>()
    data class Error<T>(val response: Response<T>): NetworkState<T>()
}

fun <T> Response<T>.parseResponse(): NetworkState<T> {
    return if (this.isSuccessful &&  this.code() == 200 &&  this.body() != null) {
        val responseBody = this.body()
        NetworkState.Success(responseBody!!)
    } else {
        NetworkState.Error(this)
    }
}