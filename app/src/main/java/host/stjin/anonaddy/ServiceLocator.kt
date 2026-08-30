package host.stjin.anonaddy

import host.stjin.anonaddy.service.AliasWatcher
import host.stjin.anonaddy_shared.ServiceLocator as BaseServiceLocator

/**
 * A simple Service Locator to centralize component creation and ensure singletons are used where appropriate.
 * This avoids manual instantiation of repositories and managers in every ViewModel/Activity.
 */
object ServiceLocator : BaseServiceLocator() {
    // Services
    val aliasWatcher: AliasWatcher by lazy { AliasWatcher(getContext()) }
}
