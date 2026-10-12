package host.stjin.anonaddy_shared

import com.google.gson.Gson
import host.stjin.anonaddy_shared.models.Action
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleActionUnitTest {

    private val gson = Gson()

    @Test
    fun testEmptyAliasDescriptionSerializesValueField() {
        val action = Action(type = "setAliasDescription", value = "")
        val json = gson.toJson(action)
        assertTrue("JSON should contain value field with empty string", json.contains("\"value\":\"\""))
        assertTrue("JSON should contain type field", json.contains("\"type\":\"setAliasDescription\""))
    }

    @Test
    fun testEmptyAliasDescriptionDeserialization() {
        val json = "{\"type\":\"setAliasDescription\",\"value\":\"\"}"
        val action = gson.fromJson(json, Action::class.java)
        assertEquals("setAliasDescription", action.type)
        assertEquals("", action.value)
    }

    @Test
    fun testNormalizeNullAliasDescriptionToEmptyString() {
        val actionWithNull = Action(type = "setAliasDescription", value = null)
        val normalized = if (actionWithNull.type == "setAliasDescription" && actionWithNull.value == null) {
            actionWithNull.copy(value = "")
        } else {
            actionWithNull
        }
        val json = gson.toJson(normalized)
        assertTrue("Normalized action JSON should contain value field with empty string", json.contains("\"value\":\"\""))
    }

    @Test
    fun testWebhookActionSerialization() {
        val webhookValue = host.stjin.anonaddy_shared.models.WebhookActionValue(
            url = "https://example.com/webhook",
            include_body = true,
            include_headers = true
        )
        val action = Action(type = "webhook", value = webhookValue)
        val json = host.stjin.anonaddy_shared.utils.GsonTools.gson.toJson(action)
        assertTrue("JSON should contain type webhook", json.contains("\"type\":\"webhook\""))
        assertTrue("JSON should contain url", json.contains("\"url\":\"https://example.com/webhook\""))
        assertTrue("JSON should contain include_body true", json.contains("\"include_body\":true"))
        assertTrue("JSON should contain include_headers true", json.contains("\"include_headers\":true"))
    }

    @Test
    fun testWebhookActionDeserializationWithGsonTools() {
        val json = "{\"type\":\"webhook\",\"value\":{\"url\":\"https://example.com/webhook\",\"include_body\":true,\"include_headers\":true}}"
        val action = host.stjin.anonaddy_shared.utils.GsonTools.gson.fromJson(json, Action::class.java)
        assertEquals("webhook", action.type)
        val webhookValue = action.getWebhookValue()
        org.junit.Assert.assertNotNull(webhookValue)
        assertEquals("https://example.com/webhook", webhookValue?.url)
        assertEquals(true, webhookValue?.include_body)
        assertEquals(true, webhookValue?.include_headers)
    }

    @Test
    fun testWebhookActionDeserializationWithPlainGson() {
        val json = "{\"type\":\"webhook\",\"value\":{\"url\":\"https://example.com/webhook\",\"include_body\":false,\"include_headers\":true}}"
        val action = gson.fromJson(json, Action::class.java)
        assertEquals("webhook", action.type)
        val webhookValue = action.getWebhookValue()
        org.junit.Assert.assertNotNull(webhookValue)
        assertEquals("https://example.com/webhook", webhookValue?.url)
        assertEquals(false, webhookValue?.include_body)
        assertEquals(true, webhookValue?.include_headers)
    }
}
