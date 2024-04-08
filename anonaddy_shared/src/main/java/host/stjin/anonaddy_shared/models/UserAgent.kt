package host.stjin.anonaddy_shared.models

data class UserAgent(
    var userAgentApplicationID: String,
    var userAgentVersion: String,
    var userAgentVersionCode: Int,
    var userAgentApplicationBuildType: String
)