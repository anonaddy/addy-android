package host.stjin.anonaddy.ui

enum class ActivityTargets(val activity: String) {
    ALIASES("aliases"),
    RECIPIENTS("recipients"),
    DOMAINS("domains"),
    USERNAMES("usernames"),
    RULES("rules"),
    FAILED_DELIVERIES("failed_deliveries")
}