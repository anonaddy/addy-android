package host.stjin.anonaddy_shared

import host.stjin.anonaddy_shared.network.NetworkResponse
import host.stjin.anonaddy_shared.network.NetworkResult
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class NetworkResponseUnitTest {

    @Test
    fun testNetworkResponseFailure() {
        val ioException = IOException("Connection timed out")
        val failure = NetworkResponse.Failure(ioException)

        assertEquals(ioException, failure.exception)
        assertEquals("Connection timed out", failure.exception.message)
    }

    @Test
    fun testNetworkResponseSuccess() {
        val request = Request.Builder().url("https://app.addy.io/api/v1/aliases").build()
        val response = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("{\"data\": []}".toResponseBody("application/json".toMediaType()))
            .build()

        val success = NetworkResponse.Success(response)
        assertEquals(200, success.code)
        assertEquals("{\"data\": []}", success.body.string())
    }

    @Test
    fun testNetworkResultErrorWithException() {
        val exception = IOException("No internet connection")
        val errorResult: NetworkResult<String> = NetworkResult.Error("No internet connection", 0, exception)

        assertNull(errorResult.getOrNull())
        assertEquals("No internet connection", errorResult.errorOrNull())
        assertTrue(errorResult is NetworkResult.Error)
        assertEquals(exception, (errorResult as NetworkResult.Error).exception)
    }
}
