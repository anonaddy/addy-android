package host.stjin.anonaddy.ui.intent

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import host.stjin.anonaddy.R
import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy.ui.base.BaseActivity
import host.stjin.anonaddy_shared.AddyIoApp
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.models.Aliases
import host.stjin.anonaddy_shared.network.NetworkResult
import kotlinx.coroutines.launch

class CreateAliasAssistantActivity : BaseActivity() {

    override val requiresAuthentication: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkForDarkModeAndSetFlags()

        val apiKey = ServiceLocator.encryptedSettingsManager.getSettingsString(SettingsManager.PREFS.API_KEY)
        if (apiKey.isNullOrEmpty()) {
            Toast.makeText(this, getString(R.string.assistant_app_not_setup), Toast.LENGTH_LONG).show()
            finish()
            return
        }

        requireAuthentication(shouldFinishOnError = true) {
            handleAssistantRequest()
        }
    }

    private fun handleAssistantRequest() {
        val intent = intent
        val extras = intent.extras

        // Extract parameters from intent extras (standard and assistant formats) or Uri data
        val uri = intent.data
        val description = extras?.getString("description")?.takeIf { it.isNotBlank() }
            ?: extras?.getString("alias_description")?.takeIf { it.isNotBlank() }
            ?: extras?.getString("account.name")?.takeIf { it.isNotBlank() }
            ?: extras?.getString("name")?.takeIf { it.isNotBlank() }
            ?: uri?.getQueryParameter("description")?.takeIf { it.isNotBlank() }
            ?: ""

        val domain = extras?.getString("domain")?.takeIf { it.isNotBlank() }
            ?: extras?.getString("alias_domain")?.takeIf { it.isNotBlank() }
            ?: uri?.getQueryParameter("domain")?.takeIf { it.isNotBlank() }

        val format = extras?.getString("format")?.takeIf { it.isNotBlank() }
            ?: extras?.getString("alias_format")?.takeIf { it.isNotBlank() }
            ?: uri?.getQueryParameter("format")?.takeIf { it.isNotBlank() }

        val localPart = extras?.getString("local_part")?.takeIf { it.isNotBlank() }
            ?: extras?.getString("alias_local_part")?.takeIf { it.isNotBlank() }
            ?: uri?.getQueryParameter("local_part")?.takeIf { it.isNotBlank() }
            ?: ""

        Toast.makeText(this, getString(R.string.assistant_creating_alias), Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            createAlias(
                customDomain = domain,
                customDescription = description,
                customFormat = format,
                customLocalPart = localPart
            )
        }
    }

    private suspend fun createAlias(
        customDomain: String?,
        customDescription: String,
        customFormat: String?,
        customLocalPart: String
    ) {
        val userResource = (application as? AddyIoApp)?.userResourceOrNull ?: run {
            when (val userResult = ServiceLocator.userRepository.getUserResource()) {
                is NetworkResult.Success -> userResult.data
                else -> null
            }
        }

        val domain = customDomain?.takeIf { it.isNotBlank() }
            ?: userResource?.default_alias_domain
            ?: ""

        val isCustom = customLocalPart.isNotBlank()
        val format = if (isCustom) {
            "custom"
        } else {
            customFormat?.takeIf { it.isNotBlank() }
                ?: userResource?.default_alias_format?.takeIf { it != "custom" }
                ?: "random_characters"
        }

        if (domain.isBlank()) {
            Toast.makeText(this, getString(R.string.assistant_error_creating_alias), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val aliasRepository = ServiceLocator.aliasRepository
        val result = aliasRepository.addAlias(
            domain = domain,
            description = customDescription,
            format = format,
            aliasLocalPart = customLocalPart,
            recipients = null,
            labels = null
        )

        when (result) {
            is NetworkResult.Success -> {
                val alias = result.data
                onAliasCreated(alias)
            }
            else -> {
                Toast.makeText(this, getString(R.string.assistant_error_creating_alias), Toast.LENGTH_SHORT).show()
                setResult(RESULT_CANCELED)
                finish()
            }
        }
    }

    private suspend fun onAliasCreated(alias: Aliases) {
        // Copy to clipboard
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Alias", alias.email)
        clipboard.setPrimaryClip(clip)

        // Show confirmation toast
        Toast.makeText(
            this,
            getString(R.string.assistant_alias_created_copied, alias.email),
            Toast.LENGTH_LONG
        ).show()

        // Index the newly created alias before finishing
        try {
            ServiceLocator.aliasSearchManager.indexAlias(alias)
        } catch (_: Exception) {}

        // Return result
        val resultIntent = Intent().apply {
            putExtra("alias_email", alias.email)
            putExtra("alias_id", alias.id)
            data = Uri.parse("addy://alias/${alias.id}")
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }
}
