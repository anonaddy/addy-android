package host.stjin.anonaddy_shared.network

sealed class NetworkResult<out T> {
    data class Success<out T>(val data: T, val statusCode: Int = 200) : NetworkResult<T>()
    data class Error(val error: String?, val statusCode: Int = 0, val exception: Throwable? = null) : NetworkResult<Nothing>()

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }

    fun errorOrNull(): String? = when (this) {
        is Success -> null
        is Error -> error
    }

    inline fun onSuccess(action: (data: T) -> Unit): NetworkResult<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onError(action: (error: String?, statusCode: Int) -> Unit): NetworkResult<T> {
        if (this is Error) action(error, statusCode)
        return this
    }
}
