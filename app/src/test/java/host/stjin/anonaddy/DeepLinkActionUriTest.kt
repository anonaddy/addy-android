package host.stjin.anonaddy

import host.stjin.anonaddy.utils.DeepLinkActionHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DeepLinkActionUriTest {

    @Test
    fun testBlockEmailUri() {
        val uriString =
            "https://app.addy.io/aliases/ddb5c91d-d56b-407f-9f80-059d876087b9/actions?action=block_email&email=news%40shop.example&signature=298378bfc7737b0188c79ecc03a019e56989e461e8da3807169eec14e9bece56"

        val (aliasId, blockAction) = DeepLinkActionHelper.parseAction(uriString)

        assertEquals("ddb5c91d-d56b-407f-9f80-059d876087b9", aliasId)
        assertNotNull(blockAction)
        assertEquals("email", blockAction?.type)
        assertEquals("news@shop.example", blockAction?.value)
    }

    @Test
    fun testBlockDomainUriFromEmail() {
        val uriString =
            "https://app.addy.io/aliases/ddb5c91d-d56b-407f-9f80-059d876087b9/actions?action=block_domain&email=news%40shop.example&signature=59b02a84f73d635c4235820aa3e33f10a3acad3e7f887c1178e89d43b81d667e"

        val (aliasId, blockAction) = DeepLinkActionHelper.parseAction(uriString)

        assertEquals("ddb5c91d-d56b-407f-9f80-059d876087b9", aliasId)
        assertNotNull(blockAction)
        assertEquals("domain", blockAction?.type)
        assertEquals("shop.example", blockAction?.value)
    }

    @Test
    fun testBlockDomainUriWithExplicitDomain() {
        val uriString =
            "https://app.addy.io/aliases/ddb5c91d-d56b-407f-9f80-059d876087b9/actions?action=block_domain&domain=shop.example&signature=12345"

        val (aliasId, blockAction) = DeepLinkActionHelper.parseAction(uriString)

        assertEquals("ddb5c91d-d56b-407f-9f80-059d876087b9", aliasId)
        assertNotNull(blockAction)
        assertEquals("domain", blockAction?.type)
        assertEquals("shop.example", blockAction?.value)
    }

    @Test
    fun testDeactivateUri() {
        val uriString =
            "https://app.addy.io/deactivate/ddb5c91d-d56b-407f-9f80-059d876087b9?signature=12345"

        val (aliasId, blockAction) = DeepLinkActionHelper.parseAction(uriString)

        assertEquals("ddb5c91d-d56b-407f-9f80-059d876087b9", aliasId)
        assertNull(blockAction)
    }

    @Test
    fun testGenericAliasesUri() {
        val uriString = "https://app.addy.io/aliases/ddb5c91d-d56b-407f-9f80-059d876087b9"

        val (aliasId, blockAction) = DeepLinkActionHelper.parseAction(uriString)

        assertEquals("ddb5c91d-d56b-407f-9f80-059d876087b9", aliasId)
        assertNull(blockAction)
    }
}
