    package host.stjin.anonaddy.interfaces

    /**
     * An interface for fragments that can have their data refreshed from an external source, like MainActivity.
     */
    interface Refreshable {
        fun onRefreshData()
    }
    