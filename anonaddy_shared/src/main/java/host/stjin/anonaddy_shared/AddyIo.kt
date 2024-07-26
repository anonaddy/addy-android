package host.stjin.anonaddy_shared

import host.stjin.anonaddy_shared.utils.resettableLazy
import host.stjin.anonaddy_shared.utils.resettableManager

object AddyIo {
    var API_BASE_URL = "https://app.addy.io"

    // The versioncode is a combination of MAJOR MINOR PATCH
    //TODO Update on every release

    // 1.2.2
    var MINIMUMVERSIONCODEMAJOR = 1
    var MINIMUMVERSIONCODEMINOR = 2
    var MINIMUMVERSIONCODEPATCH = 2

    var VERSIONMAJOR = 0
    var VERSIONMINOR = 0
    var VERSIONPATCH = 0
    var VERSIONSTRING = ""


    //resettableLazy(lazyMgr) properties: the value gets computed only upon first access
    val lazyMgr = resettableManager()
    val API_URL_RECIPIENTS: String by resettableLazy(lazyMgr) { "$API_BASE_URL/api/v1/recipients" }

    //0.10.1
    val API_URL_ALLOWED_RECIPIENTS: String by resettableLazy(lazyMgr) { "$API_BASE_URL/api/v1/allowed-recipients" }

    val API_URL_ALIAS: String by resettableLazy(lazyMgr) { "$API_BASE_URL/api/v1/aliases" }
    val API_URL_ACTIVE_ALIAS: String by resettableLazy(lazyMgr) { "$API_BASE_URL/api/v1/active-aliases" }
    val API_URL_ALIAS_RECIPIENTS: String by resettableLazy(lazyMgr) { "$API_BASE_URL/api/v1/alias-recipients" }
    val API_URL_DOMAIN_OPTIONS: String by resettableLazy(lazyMgr) { "$API_BASE_URL/api/v1/domain-options" }
    val API_URL_ENCRYPTED_RECIPIENTS: String by resettableLazy(lazyMgr) { "$API_BASE_URL/api/v1/encrypted-recipients" }
    val API_URL_INLINE_ENCRYPTED_RECIPIENTS: String by resettableLazy(lazyMgr) { "$API_BASE_URL/api/v1/inline-encrypted-recipients" }
    val API_URL_PROTECTED_HEADERS_RECIPIENTS: String by resettableLazy(lazyMgr) { "$API_BASE_URL/api/v1/protected-headers-recipients" }
    val API_URL_RECIPIENT_RESEND: String by resettableLazy(lazyMgr) { "$API_BASE_URL/api/v1/recipients/email/resend" }
    val API_URL_RECIPIENT_KEYS: String by resettableLazy(lazyMgr) { "$API_BASE_URL/api/v1/recipient-keys" }
    val API_URL_ACCOUNT_DETAILS: String by resettableLazy(lazyMgr) { "$API_BASE_URL/api/v1/account-details" }
    val API_URL_DOMAINS: String by resettableLazy(lazyMgr) { "$API_BASE_URL/api/v1/domains" }
    val API_URL_ACTIVE_DOMAINS: String by resettableLazy(lazyMgr) { "$API_BASE_URL/api/v1/active-domains" }
    val API_URL_CATCH_ALL_DOMAINS: String by resettableLazy(lazyMgr) { "$API_BASE_URL/api/v1/catch-all-domains" }
    val API_URL_USERNAMES: String by resettableLazy(lazyMgr) { "$API_BASE_URL/api/v1/usernames" }
    val API_URL_ACTIVE_USERNAMES: String by resettableLazy(lazyMgr) { "$API_BASE_URL/api/v1/active-usernames" }
    val API_URL_CATCH_ALL_USERNAMES: String by resettableLazy(lazyMgr) { "$API_BASE_URL/api/v1/catch-all-usernames" }
    val API_URL_CAN_LOGIN_USERNAMES: String by resettableLazy(lazyMgr) { "$API_BASE_URL/api/v1/loginable-usernames" }
    val API_URL_RULES: String by resettableLazy(lazyMgr) { "$API_BASE_URL/api/v1/rules" }
    val API_URL_ACTIVE_RULES: String by resettableLazy(lazyMgr) { "$API_BASE_URL/api/v1/active-rules" }
    val API_URL_REORDER_RULES: String by resettableLazy(lazyMgr) { "$API_BASE_URL/api/v1/reorder-rules" }
    val API_URL_API_TOKEN_DETAILS: String by resettableLazy(lazyMgr) { "$API_BASE_URL/api/v1/api-token-details" }

    // 0.8.1
    val API_URL_FAILED_DELIVERIES: String by resettableLazy(lazyMgr) { "$API_BASE_URL/api/v1/failed-deliveries" }

    // 0.6.0
    val API_URL_APP_VERSION: String by resettableLazy(lazyMgr) { "$API_BASE_URL/api/v1/app-version" }

    // 1.0.0
    val API_URL_CHART_DATA: String by resettableLazy(lazyMgr) { "$API_BASE_URL/api/v1/chart-data" }

    // Gitlab built-in updater
    const val GITLAB_TAGS_RSS_FEED: String = "https://gitlab.com/Stjin/anonaddy-android/-/tags?feed_token=QQ9pQKWGBdsYzCrqkdBN&format=atom"
}