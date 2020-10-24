package host.stjin.anonaddy

object AnonAddy {
    var API_BASE_URL = "https://app.anonaddy.com"

    // The versioncode is a combination of MAJOR MINOR PATCH
    var VERSIONCODE = 0
    var VERSIONSTRING = ""


    //resettableLazy(lazyMgr) properties: the value gets computed only upon first access
    val lazyMgr = resettableManager()
    val API_URL_RECIPIENTS: String by resettableLazy(lazyMgr) { "$API_BASE_URL/api/v1/recipients" }
    val API_URL_ALIAS: String by resettableLazy(lazyMgr) { "$API_BASE_URL/api/v1/aliases" }
    val API_URL_ACTIVE_ALIAS: String by resettableLazy(lazyMgr) { "$API_BASE_URL/api/v1/active-aliases" }
    val API_URL_ALIAS_RECIPIENTS: String by resettableLazy(lazyMgr) { "$API_BASE_URL/api/v1/alias-recipients" }
    val API_URL_DOMAIN_OPTIONS: String by resettableLazy(lazyMgr) { "$API_BASE_URL/api/v1/domain-options" }
    val API_URL_ENCRYPTED_RECIPIENTS: String by resettableLazy(lazyMgr) { "$API_BASE_URL/api/v1/encrypted-recipients" }
    val API_URL_RECIPIENT_RESEND: String by resettableLazy(lazyMgr) { "$API_BASE_URL/api/v1/recipients/email/resend" }
    val API_URL_RECIPIENT_KEYS: String by resettableLazy(lazyMgr) { "$API_BASE_URL/api/v1/recipient-keys" }
    val API_URL_ACCOUNT_DETAILS: String by resettableLazy(lazyMgr) { "$API_BASE_URL/api/v1/account-details" }
    val API_URL_DOMAINS: String by resettableLazy(lazyMgr) { "$API_BASE_URL/api/v1/domains" }
    val API_URL_ACTIVE_DOMAINS: String by resettableLazy(lazyMgr) { "$API_BASE_URL/api/v1/active-domains" }
    val API_URL_CATCH_ALL_DOMAINS: String by resettableLazy(lazyMgr) { "$API_BASE_URL/api/v1/catch-all-domains" }
    val API_URL_USERNAMES: String by resettableLazy(lazyMgr) { "$API_BASE_URL/api/v1/usernames" }
    val API_URL_ACTIVE_USERNAMES: String by resettableLazy(lazyMgr) { "$API_BASE_URL/api/v1/active-usernames" }
    val API_URL_RULES: String by resettableLazy(lazyMgr) { "$API_BASE_URL/api/v1/rules" }
    val API_URL_ACTIVE_RULES: String by resettableLazy(lazyMgr) { "$API_BASE_URL/api/v1/active-rules" }
    val API_URL_REORDER_RULES: String by resettableLazy(lazyMgr) { "$API_BASE_URL/api/v1/reorder-rules" }

    // 0.6.0
    val API_URL_APP_VERSION: String by resettableLazy(lazyMgr) { "$API_BASE_URL/api/v1/app-version" }
}