package host.stjin.anonaddy_shared

import android.content.Context
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.repositories.*

/**
 * A simple Service Locator to centralize component creation and ensure singletons are used where appropriate.
 * This avoids manual instantiation of repositories and managers in every ViewModel/Activity.
 */
open class ServiceLocator {
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    protected fun getContext(): Context {
        return appContext ?: throw IllegalStateException("ServiceLocator must be initialized with a Context before use.")
    }

    // Settings Managers
    val settingsManager: SettingsManager by lazy { SettingsManager(false, getContext()) }
    val encryptedSettingsManager: SettingsManager by lazy { SettingsManager(true, getContext()) }

    // Repositories
    val userRepository: UserRepository by lazy { UserRepository(getContext()) }
    val aliasRepository: AliasRepository by lazy { AliasRepository(getContext()) }
    val recipientRepository: RecipientRepository by lazy { RecipientRepository(getContext()) }
    val domainRepository: DomainRepository by lazy { DomainRepository(getContext()) }
    val usernameRepository: UsernameRepository by lazy { UsernameRepository(getContext()) }
    val ruleRepository: RuleRepository by lazy { RuleRepository(getContext()) }
    val labelRepository: LabelRepository by lazy { LabelRepository(getContext()) }
    val blocklistRepository: BlocklistRepository by lazy { BlocklistRepository(getContext()) }
    val appMaintenanceRepository: AppMaintenanceRepository by lazy { AppMaintenanceRepository(getContext()) }
    val failedDeliveriesRepository: FailedDeliveriesRepository by lazy { FailedDeliveriesRepository(getContext()) }
}
