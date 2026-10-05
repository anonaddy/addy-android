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
}
