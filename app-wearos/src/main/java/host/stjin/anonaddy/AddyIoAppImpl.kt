package host.stjin.anonaddy

import host.stjin.anonaddy_shared.AddyIoApp

class AddyIoAppImpl : AddyIoApp() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}
