package host.stjin.anonaddy_shared

import com.google.gson.Gson
import host.stjin.anonaddy_shared.models.BulkActionResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BulkActionResponseUnitTest {

    private val gson = Gson()

    @Test
    fun testNestedDataBulkActionResponseDeserialization() {
        val json = """
            {
              "data": {
                "message": "2 aliases activated successfully",
                "ids": [
                  "50c9e585-e7f5-41c4-9016-9014c15454bc",
                  "c549db7d-5fac-4b09-9443-9e47f644d29f"
                ]
              }
            }
        """.trimIndent()

        val response = gson.fromJson(json, BulkActionResponse::class.java)
        assertEquals("2 aliases activated successfully", response.message)
        assertEquals(2, response.ids.size)
        assertEquals("50c9e585-e7f5-41c4-9016-9014c15454bc", response.ids[0])
        assertEquals("c549db7d-5fac-4b09-9443-9e47f644d29f", response.ids[1])
    }

    @Test
    fun testFlatBulkActionResponseDeserialization() {
        val json = """
            {
              "message": "labels updated for 2 aliases successfully",
              "ids": [
                "50c9e585-e7f5-41c4-9016-9014c15454bc",
                "c549db7d-5fac-4b09-9443-9e47f644d29f"
              ]
            }
        """.trimIndent()

        val response = gson.fromJson(json, BulkActionResponse::class.java)
        assertEquals("labels updated for 2 aliases successfully", response.message)
        assertEquals(2, response.ids.size)
        assertEquals("50c9e585-e7f5-41c4-9016-9014c15454bc", response.ids[0])
        assertEquals("c549db7d-5fac-4b09-9443-9e47f644d29f", response.ids[1])
    }

    @Test
    fun testEmptyOrMissingFieldsBulkActionResponseDeserialization() {
        val json = "{}"
        val response = gson.fromJson(json, BulkActionResponse::class.java)
        assertEquals("", response.message)
        assertTrue(response.ids.isEmpty())
    }
}
