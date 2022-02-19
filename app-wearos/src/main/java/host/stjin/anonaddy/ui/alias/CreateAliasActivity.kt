package host.stjin.anonaddy.ui.alias

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.material.*
import host.stjin.anonaddy.R
import host.stjin.anonaddy.components.ErrorScreen
import host.stjin.anonaddy.service.BackgroundWorkerHelper
import host.stjin.anonaddy.ui.SplashActivity
import host.stjin.anonaddy.ui.components.CustomTimeText
import host.stjin.anonaddy.utils.FavoriteAliasHelper
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.models.Aliases
import host.stjin.anonaddy_shared.ui.theme.AppTheme
import host.stjin.anonaddy_shared.utils.CacheHelper
import kotlinx.coroutines.launch

class CreateAliasActivity : ComponentActivity() {

    private lateinit var favoriteAliasHelper: FavoriteAliasHelper
    private lateinit var networkHelper: NetworkHelper
    private val SPACING_ALIAS_BUTTONS = Dp(36f)
    private val SPACING_BUTTONS = Dp(8f)

    @OptIn(ExperimentalWearMaterialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        favoriteAliasHelper = FavoriteAliasHelper(this)
        networkHelper = NetworkHelper(this)

        setComposeContent()

        if (alias == null) {
            val userResource = CacheHelper.getBackgroundServiceCacheUserResource(this)

            lifecycleScope.launch {
                if (userResource != null) {
                    NetworkHelper(this@CreateAliasActivity).addAlias({ alias, error ->
                        if (alias != null) {
                            this@CreateAliasActivity.alias = alias

                            // Since an alias was added, call scheduleBackgroundWorker. This method will schedule the service if its required
                            BackgroundWorkerHelper(this@CreateAliasActivity).scheduleBackgroundWorker()
                        } else {
                            setContent {
                                ErrorScreen(
                                    this@CreateAliasActivity,
                                    this@CreateAliasActivity.resources.getString(R.string.error_adding_alias) + "\n" + error,
                                    this@CreateAliasActivity.resources.getString(R.string.add_alias)
                                )
                            }
                        }
                        // TODO description? And this is not gonna work with custom alias format
                    }, domain = userResource.default_alias_domain, "", userResource.default_alias_format, local_part = "", null)
                } else {
                    // App not setup, open splash
                    val intent = Intent(this@CreateAliasActivity, SplashActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }

        }
    }


    private var alias by mutableStateOf<Aliases?>(null)

    @OptIn(ExperimentalWearMaterialApi::class)
    private fun setComposeContent() {
        setContent {
            AliasList()
        }
    }


    @OptIn(ExperimentalWearMaterialApi::class)
    @Composable
    private fun Loading() {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            androidx.compose.material3.CircularProgressIndicator()
        }
    }


    private var isAliasFavorite by mutableStateOf(false)

    @Composable
    private fun AddFavoriteLayout() {
        Box(
            modifier = Modifier
                .semantics {
                    contentDescription = getString(R.string.tile_favorite_aliases_favorite)
                }
        ) {
            Button(
                onClick = {
                    if (!favoriteAliasHelper.addAliasAsFavorite(this@CreateAliasActivity.alias!!.id)) {
                        Toast.makeText(
                            this@CreateAliasActivity,
                            resources.getString(R.string.max_favorites_reached),
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    } else {
                        isAliasFavorite = true
                    }
                },
                enabled = true,
            ) {
                Icon(
                    painter = if (isAliasFavorite) painterResource(id = R.drawable.ic_starred) else painterResource(id = R.drawable.ic_star),
                    contentDescription = getString(R.string.tile_favorite_aliases_favorite),
                    modifier = Modifier
                        .size(24.dp)
                        .wrapContentSize(align = Alignment.Center),
                )
            }
        }
    }

    @Composable
    private fun ShowOnPhoneLayout() {
        Box(
            modifier = Modifier
                .semantics {
                    contentDescription = getString(R.string.show_on_paired_device)
                }
        ) {
            Button(
                onClick = {
                    val intent = Intent(this@CreateAliasActivity, ManageAliasActivity::class.java)
                    intent.putExtra("alias", alias!!.id)
                    intent.putExtra("showOnPairedDevice", true)
                    startActivity(intent)
                    finish()
                },
                enabled = true,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_devices),
                    contentDescription = getString(R.string.show_on_paired_device),
                    modifier = Modifier
                        .size(24.dp)
                        .wrapContentSize(align = Alignment.Center),
                )
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @ExperimentalWearMaterialApi
    @Composable
    private fun AliasList() {
        val haptic = LocalHapticFeedback.current
        AppTheme {
            rememberScalingLazyListState()
            Scaffold(
                modifier = Modifier,
                timeText = {
                    CustomTimeText(
                        visible = true,
                        showLeadingText = true,
                        leadingText = resources.getString(R.string.add_alias)
                    )
                }
            ) {
                if (alias == null) {
                    Loading()
                } else {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(SPACING_ALIAS_BUTTONS))
                            Text(alias!!.email, fontSize = 16.sp, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(SPACING_ALIAS_BUTTONS))
                            Row {
                                AddFavoriteLayout()
                                Spacer(modifier = Modifier.width(SPACING_BUTTONS))
                                ShowOnPhoneLayout()
                            }
                        }
                    }
                }


            }


        }
    }
}
