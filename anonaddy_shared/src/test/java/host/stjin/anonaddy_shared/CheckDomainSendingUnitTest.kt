package host.stjin.anonaddy_shared

import com.google.gson.Gson
import host.stjin.anonaddy_shared.models.CheckDomainSendingResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckDomainSendingUnitTest {

    private val gson = Gson()

    @Test
    fun testCheckDomainSendingResponseDeserialization() {
        val json = """
            {
              "success": true,
              "message": "Records successfully verified.",
              "data": {
                "id": "domain-123",
                "user_id": "user-456",
                "domain": "example.com",
                "description": "My custom domain",
                "from_name": "Test",
                "aliases_count": 5,
                "default_recipient": null,
                "active": true,
                "catch_all": false,
                "auto_create_regex": null,
                "domain_verified_at": "2024-01-01T00:00:00.000000Z",
                "domain_mx_validated_at": "2024-01-01T00:00:00.000000Z",
                "domain_sending_verified_at": "2024-01-02T12:00:00.000000Z",
                "created_at": "2024-01-01T00:00:00.000000Z",
                "updated_at": "2024-01-02T12:00:00.000000Z"
              }
            }
        """.trimIndent()

        val response = gson.fromJson(json, CheckDomainSendingResponse::class.java)
        assertTrue(response.success == true)
        assertEquals("Records successfully verified.", response.message)
        assertNotNull(response.data)
        assertEquals("domain-123", response.data.id)
        assertEquals("example.com", response.data.domain)
        assertEquals("2024-01-02T12:00:00.000000Z", response.data.domain_sending_verified_at)
    }
}
