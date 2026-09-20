package host.stjin.anonaddy_shared.network

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.security.KeyChain
import android.util.Log
import host.stjin.anonaddy_shared.AddyIo.API_BASE_URL
import host.stjin.anonaddy_shared.AddyIoApp
import host.stjin.anonaddy_shared.BuildConfig
import host.stjin.anonaddy_shared.R
import host.stjin.anonaddy_shared.ServiceLocator
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.models.ErrorHelper
import host.stjin.anonaddy_shared.models.LOGIMPORTANCE
import host.stjin.anonaddy_shared.utils.DefaultDispatcherProvider
import host.stjin.anonaddy_shared.utils.DispatcherProvider
import host.stjin.anonaddy_shared.utils.LoggingHelper
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.Socket
import java.security.KeyStore
import java.security.Principal
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509KeyManager
import javax.net.ssl.X509TrustManager
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation {
        cancel()
    }
    enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) {
            continuation.resume(response)
        }
        override fun onFailure(call: Call, e: IOException) {
            continuation.resumeWithException(e)
        }
    })
}

open class BaseNetworkClient(
    protected val context: Context,
    val dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) {
    private val serviceLocator: ServiceLocator by lazy { ServiceLocator().apply { init(context) } }
    val loggingHelper = LoggingHelper(context)
    val gson = host.stjin.anonaddy_shared.utils.GsonTools.gson
    val encryptedSettingsManager = serviceLocator.encryptedSettingsManager

    companion object {
        private val initMutex = Mutex()
        @Volatile
        private var okHttpClient: OkHttpClient? = null
    }

    init {
        API_BASE_URL = encryptedSettingsManager.getSettingsString(SettingsManager.PREFS.BASE_URL) ?: API_BASE_URL
    }

    suspend fun waitForInit() {
        if (BuildConfig.DEBUG) {
            Log.d("AFA", "Waiting for init")
        }
        if (okHttpClient == null) {
            initMutex.withLock {
                if (okHttpClient == null) {
                    okHttpClient = createOkHttpClient()
                }
            }
        }
    }

    suspend fun getClient(): OkHttpClient {
        waitForInit()
        return okHttpClient ?: initMutex.withLock {
            okHttpClient ?: createOkHttpClient().also { okHttpClient = it }
        }
    }

    private suspend fun createOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

        val alias = encryptedSettingsManager.getSettingsString(SettingsManager.PREFS.CERTIFICATE_ALIAS)
        if (alias != null) {
            try {
                val chain = withContext(dispatchers.io) {
                    KeyChain.getCertificateChain(context, alias)
                }
                val privateKey = withContext(dispatchers.io) {
                    KeyChain.getPrivateKey(context, alias)
                }
                if (chain != null && privateKey != null) {
                    withContext(dispatchers.main) {
                        setupCustomSocketFactory(builder, alias, chain, privateKey)
                    }
                }
            } catch (e: Exception) {
                withContext(dispatchers.main) {
                    loggingHelper.addLog(
                        LOGIMPORTANCE.CRITICAL.int,
                        e.message.toString(),
                        "BaseNetworkClient;init",
                        e.stackTrace.contentToString()
                    )
                }
            }
        }
        return builder.build()
    }

    private fun setupCustomSocketFactory(
        builder: OkHttpClient.Builder,
        alias: String,
        chain: Array<X509Certificate>?,
        privateKey: PrivateKey
    ) {
        val expiryDateOfChain = chain?.firstOrNull()?.notAfter
        expiryDateOfChain?.let {
            if (it < Date()) {
                invalidCertificate()
                Handler(Looper.getMainLooper()).postDelayed({
                    serviceLocator.encryptedSettingsManager.clearSettingsAndCloseApp()
                }, 8000)
            }
        }

        val customKeyManager = object : X509KeyManager {
            override fun chooseClientAlias(keyType: Array<String>?, issuers: Array<Principal>?, socket: Socket?): String {
                return alias
            }
            override fun getCertificateChain(alias: String?): Array<X509Certificate>? {
                return if (alias == this.chooseClientAlias(null, null, null)) chain else null
            }
            override fun getPrivateKey(alias: String?): PrivateKey? {
                return if (alias == this.chooseClientAlias(null, null, null)) privateKey else null
            }
            override fun chooseServerAlias(keyType: String?, issuers: Array<out Principal?>?, socket: Socket?): String? = null
            override fun getClientAliases(keyType: String?, issuers: Array<Principal>?): Array<String>? = null
            override fun getServerAliases(keyType: String?, issuers: Array<Principal>?): Array<String>? = null
        }

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(arrayOf(customKeyManager), null, null)

        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(null as KeyStore?)
        val trustManagers = trustManagerFactory.trustManagers
        val x509TrustManager = trustManagers.firstOrNull { it is X509TrustManager } as? X509TrustManager
            ?: return

        builder.sslSocketFactory(sslContext.socketFactory, x509TrustManager)
    }

    private fun invalidCertificate() {
        try {
            loggingHelper.addLog(
                LOGIMPORTANCE.CRITICAL.int,
                context.resources.getString(R.string.certificate_key_invalid),
                "invalidCertificate",
                null
            )
        } catch (e: Exception) {
            Log.e("AFA", e.message.toString())
        }
    }

    fun invalidApiKey() {
        try {
            loggingHelper.addLog(
                LOGIMPORTANCE.CRITICAL.int,
                context.resources.getString(R.string.api_key_invalid),
                "invalidApiKey",
                null
            )
        } catch (e: Exception) {
            Log.e("AFA", e.message.toString())
        }
    }

    fun getHeaders(apiKey: String? = null): Array<Pair<String, Any>> {
        val apiKeyToSend = apiKey ?: encryptedSettingsManager.getSettingsString(SettingsManager.PREFS.API_KEY)
        return arrayOf(
            "Authorization" to "Bearer $apiKeyToSend",
            "Content-Type" to "application/json",
            "X-Requested-With" to "XMLHttpRequest",
            "Accept" to "application/json",
            "User-Agent" to userAgent
        )
    }

    protected val userAgent: String by lazy {
        val app = context.applicationContext as? AddyIoApp
        val ua = if (app != null) {
            "${app.userAgent.userAgentApplicationID} (${app.userAgent.userAgentApplicationBuildType}) / ${app.userAgent.userAgentVersion} (${app.userAgent.userAgentVersionCode})"
        } else {
            "addy.io for Android"
        }
        ua
    }

    suspend fun executeGet(
        url: String,
        parameters: List<Pair<String, Any?>>? = null,
        headers: Array<Pair<String, Any>>? = null
    ): Response {
        val client = getClient()
        val urlBuilder = url.toHttpUrl().newBuilder()
        parameters?.forEach { (key, value) ->
            if (value != null) {
                urlBuilder.addQueryParameter(key, value.toString())
            }
        }
        val requestBuilder = Request.Builder().url(urlBuilder.build()).get()
        val headerList = headers ?: getHeaders()
        headerList.forEach { (k, v) ->
            requestBuilder.header(k, v.toString())
        }
        return client.newCall(requestBuilder.build()).await()
    }

    suspend fun executePost(
        url: String,
        jsonBody: String? = null,
        headers: Array<Pair<String, Any>>? = null
    ): Response {
        val client = getClient()
        val body = (jsonBody ?: "").toRequestBody("application/json; charset=utf-8".toMediaType())
        val requestBuilder = Request.Builder().url(url).post(body)
        val headerList = headers ?: getHeaders()
        headerList.forEach { (k, v) ->
            requestBuilder.header(k, v.toString())
        }
        return client.newCall(requestBuilder.build()).await()
    }

    suspend fun executePatch(
        url: String,
        jsonBody: String? = null,
        headers: Array<Pair<String, Any>>? = null
    ): Response {
        val client = getClient()
        val body = (jsonBody ?: "").toRequestBody("application/json; charset=utf-8".toMediaType())
        val requestBuilder = Request.Builder().url(url).patch(body)
        val headerList = headers ?: getHeaders()
        headerList.forEach { (k, v) ->
            requestBuilder.header(k, v.toString())
        }
        return client.newCall(requestBuilder.build()).await()
    }

    suspend fun executeDelete(
        url: String,
        jsonBody: String? = null,
        headers: Array<Pair<String, Any>>? = null
    ): Response {
        val client = getClient()
        val body = jsonBody?.toRequestBody("application/json; charset=utf-8".toMediaType())
        val requestBuilder = Request.Builder().url(url).delete(body)
        val headerList = headers ?: getHeaders()
        headerList.forEach { (k, v) ->
            requestBuilder.header(k, v.toString())
        }
        return client.newCall(requestBuilder.build()).await()
    }

    fun handleGenericError(
        statusCode: Int,
        bodyString: String,
        methodName: String,
        exception: Throwable? = null
    ): String {
        val exMessage = exception?.message ?: "HTTP $statusCode"
        Log.e("BaseNetworkClient", "$statusCode - $exMessage")
        val errorMessage = ErrorHelper.getErrorMessage(bodyString.toByteArray())
        loggingHelper.addLog(
            LOGIMPORTANCE.CRITICAL.int,
            exMessage,
            methodName,
            errorMessage
        )
        return errorMessage
    }

    protected fun <T> handleResponse(
        response: Response,
        methodName: String,
        parser: (String) -> T
    ): NetworkResult<T> {
        val bodyString = try {
            response.body?.string() ?: ""
        } catch (e: Exception) {
            ""
        }
        val code = response.code
        return when (code) {
            200, 201 -> {
                try {
                    NetworkResult.Success(parser(bodyString), code)
                } catch (e: Exception) {
                    val errorMessage = handleGenericError(code, bodyString, methodName, e)
                    NetworkResult.Error(errorMessage, code)
                }
            }
            204 -> {
                @Suppress("UNCHECKED_CAST")
                NetworkResult.Success(Unit as T, code)
            }
            401 -> {
                invalidApiKey()
                NetworkResult.Error("Unauthorized", code)
            }
            else -> {
                val errorMessage = handleGenericError(code, bodyString, methodName, null)
                NetworkResult.Error(errorMessage, code)
            }
        }
    }

    protected fun handleStatusResponse(
        response: Response,
        methodName: String,
        expectedCode: Int = 200
    ): NetworkResult<String> {
        val bodyString = try {
            response.body?.string() ?: ""
        } catch (e: Exception) {
            ""
        }
        val code = response.code
        return when (code) {
            expectedCode -> NetworkResult.Success(expectedCode.toString(), code)
            401 -> {
                invalidApiKey()
                NetworkResult.Error("Unauthorized", code)
            }
            else -> {
                val errorMessage = handleGenericError(code, bodyString, methodName, null)
                NetworkResult.Error(errorMessage, code)
            }
        }
    }
}
