package host.stjin.anonaddy_shared

import host.stjin.anonaddy_shared.models.ServiceStatus
import host.stjin.anonaddy_shared.models.StatusMonitor
import host.stjin.anonaddy_shared.models.StatusUpdate
import host.stjin.anonaddy_shared.utils.GsonTools
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceStatusUnitTest {

    @Test
    fun testIsDefaultInstance() {
        assertTrue(AddyIo.isDefaultInstance("https://app.addy.io"))
        assertTrue(AddyIo.isDefaultInstance("https://app.addy.io/"))
        assertTrue(AddyIo.isDefaultInstance("http://app.addy.io"))
        assertTrue(AddyIo.isDefaultInstance("https://app.addy.io/api/v1"))
        assertFalse(AddyIo.isDefaultInstance("https://example.com"))
        assertFalse(AddyIo.isDefaultInstance("https://anonaddy.me"))
    }

    @Test
    fun testParseOperationalStatus() {
        val json = """
            {
              "title": "addy.io Service Status",
              "timezone": "Europe/London",
              "pinnedUpdate": null,
              "monitors": {
                "ungrouped": [
                  {"label": "Landing Page", "url": "https://addy.io", "status": "up"},
                  {"label": "Web Application", "url": "https://app.addy.io/login", "status": "up"}
                ]
              },
              "updatesPerDay": {
                "1792537200": [
                  {
                    "id": 2405964,
                    "title": "Patched PHP version installed.",
                    "text": "The patched PHP version has been installed and the web app is back online.",
                    "pinned": false,
                    "severity": "resolved",
                    "time": 1792593180
                  }
                ]
              },
              "summarizedStatus": "up"
            }
        """.trimIndent()

        val status = GsonTools.gson.fromJson(json, ServiceStatus::class.java)
        assertEquals("addy.io Service Status", status.title)
        assertEquals("Europe/London", status.timezone)
        assertNull(status.pinnedUpdate)
        assertTrue(status.isOperational)
        assertEquals("up", status.summarizedStatus)
        assertEquals("up", status.webApplicationStatus)

        val latest = status.latestUpdate
        assertNotNull(latest)
        assertEquals(2405964, latest?.id)
        assertEquals("Patched PHP version installed.", latest?.title)
        assertEquals("resolved", latest?.severity)
    }

    @Test
    fun testParseIncidentDowntimeStatus() {
        val json = """
            {
              "title": "addy.io Service Status",
              "timezone": "Europe/London",
              "pinnedUpdate": {
                "id": 2403991,
                "title": "Unplanned downtime due to vulnerability.",
                "text": "We are temporarily offline.",
                "pinned": true,
                "severity": "warning",
                "time": 1792580820
              },
              "monitors": {
                "ungrouped": [
                  {"label": "Landing Page", "url": "https://addy.io", "status": "down"},
                  {"label": "Web Application", "url": "https://app.addy.io/login", "status": "down"}
                ]
              },
              "updatesPerDay": {},
              "summarizedStatus": "down"
            }
        """.trimIndent()

        val status = GsonTools.gson.fromJson(json, ServiceStatus::class.java)
        assertFalse(status.isOperational)
        assertEquals("down", status.summarizedStatus)
        assertEquals("down", status.webApplicationStatus)

        val latest = status.latestUpdate
        assertNotNull(latest)
        assertEquals("Unplanned downtime due to vulnerability.", latest?.title)
        assertEquals(true, latest?.pinned)
        assertEquals("warning", latest?.severity)
    }

    @Test
    fun testLatestUpdateFallbackFromUpdatesPerDay() {
        val update1 = StatusUpdate(id = 1, title = "Earlier update", time = 1000L)
        val update2 = StatusUpdate(id = 2, title = "Latest update", time = 2000L)
        val status = ServiceStatus(
            summarizedStatus = "warning",
            updatesPerDay = mapOf(
                "day1" to listOf(update1),
                "day2" to listOf(update2)
            )
        )
        assertFalse(status.isOperational)
        assertEquals("Latest update", status.latestUpdate?.title)
    }
}
