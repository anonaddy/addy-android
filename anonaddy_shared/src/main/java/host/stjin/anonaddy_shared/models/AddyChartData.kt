package host.stjin.anonaddy_shared.models

data class AddyChartData(
    val forwardsData: List<Int>,
    val labels: List<String>,
    val outboundMessageTotals: List<Int>,
    val repliesData: List<Int>,
    val sendsData: List<Int>
)