package hr.kotwave.gameslibrary.network

import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.statement.bodyAsText


suspend fun <T> safeApiCall(call: suspend () -> T): ApiResponse<T> {
    return try {
        ApiResponse.Success(call())
    } catch (e: Exception) {
        when (e) {
            is ClientRequestException -> ApiResponse.Error(e.response.bodyAsText())
            is ServerResponseException -> ApiResponse.Error(e.response.bodyAsText())
            else -> ApiResponse.Error(message = e.localizedMessage ?: "An unknown error occurred")
        }
    }
}