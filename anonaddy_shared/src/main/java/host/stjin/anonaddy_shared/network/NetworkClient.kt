package host.stjin.anonaddy_shared.network

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.security.KeyChain
import android.util.Log
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Response
import host.stjin.anonaddy_shared.AddyIo.API_BASE_URL
import host.stjin.anonaddy_shared.AddyIoApp
import host.stjin.anonaddy_shared.BuildConfig
import host.stjin.anonaddy_shared.R
import host.stjin.anonaddy_shared.ServiceLocator
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.models.ErrorHelper
import host.stjin.anonaddy_shared.models.LOGIMPORTANCE
import host.stjin.anonaddy_shared.utils.LoggingHelper
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.net.Socket
import java.security.Principal
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.Date
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.X509KeyManager

import host.stjin.anonaddy_shared.utils.DefaultDispatcherProvider
import host.stjin.anonaddy_shared.utils.DispatcherProvider

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
        private var isSocketFactoryInitialized = false
    }

    init {
        API_BASE_URL = encryptedSettingsManager.getSettingsString(SettingsManager.PREFS.BASE_URL) ?: API_BASE_URL
    }

    suspend fun waitForInit() {
        if (BuildConfig.DEBUG) {
            val trace = Thread.currentThread().stackTrace
            val callerMethod = if (trace.size > 4) trace[4].methodName else "unknown"
            val callerClass = if (trace.size > 4) trace[4].className else "unknown"
            val currentMethod = if (trace.size > 3) trace[3].methodName else "unknown"
            println("$currentMethod called from $callerClass;$callerMethod")
        }
        if (!isSocketFactoryInitialized) {
            initMutex.withLock {
                if (!isSocketFactoryInitialized) {
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
                                    setupCustomSocketFactory(alias, chain, privateKey)
                                }
                            }
                        } catch (e: Exception) {
                            withContext(dispatchers.main) {
                                loggingHelper.addLog(LOGIMPORTANCE.CRITICAL.int, e.message.toString(), "BaseNetworkClient;init",
                                    e.stackTrace.contentToString()
                                )
                            }
                        }
                    } else {
                        withContext(dispatchers.main) {
                            FuelManager.instance.apply {
                                socketFactory = HttpsURLConnection.getDefaultSSLSocketFactory()
                            }
                        }
                    }
                    isSocketFactoryInitialized = true
                }
            }
        }
    }

    private fun setupCustomSocketFactory(alias: String, chain: Array<X509Certificate>?, privateKey: PrivateKey) {
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

        FuelManager.instance.apply {
            socketFactory = sslContext.socketFactory
        }
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

    private val userAgent: String by lazy {
        val app = context.applicationContext as? AddyIoApp
        val ua = if (app != null) {
            "${app.userAgent.userAgentApplicationID} (${app.userAgent.userAgentApplicationBuildType}) / ${app.userAgent.userAgentVersion} (${app.userAgent.userAgentVersionCode})"
        } else {
            "addy.io for Android"
        }
        ua
    }

    fun getFuelResponse(response: Response): ByteArray? {
        return try {
            response.data
        } catch (e: Exception) {
            null
        }
    }

    fun handleGenericError(
        response: Response,
        result: com.github.kittinunf.result.Result<*, com.github.kittinunf.fuel.core.FuelError>,
        methodName: String
    ): String {
        val ex = result.component2()?.message
        val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
        Log.e("BaseNetworkClient", "${response.statusCode} - $ex")
        val errorMessage = ErrorHelper.getErrorMessage(fuelResponse)
        loggingHelper.addLog(
            LOGIMPORTANCE.CRITICAL.int,
            ex.toString(),
            methodName,
            errorMessage
        )
        return errorMessage
    }

    fun handleGenericErrorByteArray(
        response: Response,
        result: com.github.kittinunf.result.Result<ByteArray, com.github.kittinunf.fuel.core.FuelError>,
        methodName: String
    ): String = handleGenericError(response, result, methodName)

    protected fun <T> handleResponse(
        response: Response,
        result: com.github.kittinunf.result.Result<String, com.github.kittinunf.fuel.core.FuelError>,
        methodName: String,
        parser: (String) -> T
    ): NetworkResult<T> {
        return when (response.statusCode) {
            200, 201 -> {
                try {
                    val data = result.get()
                    NetworkResult.Success(parser(data), response.statusCode)
                } catch (e: Exception) {
                    val errorMessage = handleGenericError(response, result, methodName)
                    NetworkResult.Error(errorMessage, response.statusCode)
                }
            }
            204 -> {
                @Suppress("UNCHECKED_CAST")
                NetworkResult.Success(Unit as T, response.statusCode)
            }
            401 -> {
                invalidApiKey()
                NetworkResult.Error("Unauthorized", response.statusCode)
            }
            else -> {
                val errorMessage = handleGenericError(response, result, methodName)
                NetworkResult.Error(errorMessage, response.statusCode)
            }
        }
    }

    protected fun handleStatusResponse(
        response: Response,
        result: com.github.kittinunf.result.Result<String, com.github.kittinunf.fuel.core.FuelError>,
        methodName: String,
        expectedCode: Int = 200
    ): NetworkResult<String> {
        return when (response.statusCode) {
            expectedCode -> NetworkResult.Success(expectedCode.toString(), response.statusCode)
            401 -> {
                invalidApiKey()
                NetworkResult.Error("Unauthorized", response.statusCode)
            }
            else -> {
                val errorMessage = handleGenericError(response, result, methodName)
                NetworkResult.Error(errorMessage, response.statusCode)
            }
        }
    }
}
