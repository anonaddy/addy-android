package host.stjin.anonaddy.ui.alias

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.material.*
import app.futured.donut.DonutSection
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.ActivityManageAliasBinding
import host.stjin.anonaddy.utils.FavoriteAliasHelper
import host.stjin.anonaddy.utils.ResizeAnimation
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.models.Aliases
import host.stjin.anonaddy_shared.ui.theme.AppTheme
import kotlinx.coroutines.launch

class ManageAliasActivity : ComponentActivity() {

    private lateinit var binding: ActivityManageAliasBinding
    private var alias: Aliases? = null
    lateinit var networkHelper: NetworkHelper


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityManageAliasBinding.inflate(layoutInflater)

        setContentView(binding.root)

        networkHelper = NetworkHelper(this)


        val alias: Aliases? = intent.getParcelableExtra("alias")
        if (alias == null) {
            finish()
            return
        }
        this.alias = alias
        setPage()

    }


    var isChangingActivationStatus = false
    private fun setPage() {
        if (alias != null) {

            // WearOS needs a focus point
            binding.activityManageAliasScrollview.requestFocus()
/*
            binding.activityManageAliasComposeview0.setContent {
                AppTheme {
                    CurvedRow() {
                        CurvedText(
                            text = alias!!.email,
                        )
                    }
                }
            }*/

            /**
             * CHART
             */

            // Update chart
            setChart(
                alias!!.emails_forwarded.toFloat(),
                alias!!.emails_replied.toFloat(),
                alias!!.emails_blocked.toFloat(),
                alias!!.emails_sent.toFloat()
            )


            val favoriteAliasHelper = FavoriteAliasHelper(this)
            val favoriteAliases = favoriteAliasHelper.getFavoriteAliases()
            val isAliasFavorite = favoriteAliases?.contains(this@ManageAliasActivity.alias!!.id) == true
            binding.activityManageAliasComposeview1.setContent {
                AppTheme {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ToggleChip(
                            label = {
                                Text(
                                    if (this@ManageAliasActivity.alias!!.active) resources.getString(R.string.activated) else resources.getString(
                                        R.string.deactivated
                                    ), maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                            },
                            checked = alias!!.active,
                            toggleIcon = {
                                ToggleChipDefaults.SwitchIcon(checked = alias!!.active)
                            },
                            secondaryLabel = {
                                Text(
                                    if (isChangingActivationStatus) {
                                        resources.getString(
                                            R.string.changing_status
                                        )
                                    } else resources.getString(
                                        R.string.alias_status_desc
                                    ), maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                            },
                            onCheckedChange = {
                                if (!isChangingActivationStatus) {
                                    alias!!.active = it
                                    if (alias!!.active) {
                                        lifecycleScope.launch {
                                            isChangingActivationStatus = true
                                            setPage()
                                            activateAlias()
                                        }
                                    } else {
                                        lifecycleScope.launch {
                                            isChangingActivationStatus = true
                                            setPage()
                                            deactivateAlias()
                                        }
                                    }
                                }
                            },
                            enabled = true
                        )

                        ToggleChip(
                            label = {
                                Text(
                                    resources.getString(R.string.favorite), maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                            },
                            checked = isAliasFavorite,
                            onCheckedChange = {
                                if (it) {
                                    if (!favoriteAliasHelper.addAliasAsFavorite(this@ManageAliasActivity.alias!!.id)) {
                                        Toast.makeText(
                                            this@ManageAliasActivity,
                                            resources.getString(R.string.max_favorites_reached),
                                            Toast.LENGTH_SHORT
                                        )
                                            .show()
                                    }
                                } else {
                                    favoriteAliasHelper.removeAliasAsFavorite(this@ManageAliasActivity.alias!!.id)
                                }
                                setPage()
                            },
                            toggleIcon = {
                            },
                            appIcon = {
                                Icon(
                                    painter = if (isAliasFavorite) painterResource(id = R.drawable.ic_starred) else painterResource(
                                        id = R.drawable.ic_star
                                    ),
                                    contentDescription = resources.getString(R.string.alias_status_desc),
                                    modifier = Modifier
                                        .size(20.dp)
                                        .wrapContentSize(align = Alignment.Center),
                                )
                            },
                            enabled = true
                        )

                        Chip(
                            onClick = { /* Do something */ },
                            enabled = true,
                            label = { Text(text = resources.getString(R.string.set_watchface)) },
                            icon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_clock),
                                    contentDescription = resources.getString(R.string.set_watchface),
                                    modifier = Modifier
                                        .size(24.dp)
                                        .wrapContentSize(align = Alignment.Center),
                                )
                            }
                        )
                    }
                }
            }
        }
    }


    private suspend fun deactivateAlias() {
        networkHelper.deactivateSpecificAlias({ result ->
            if (result == "204") {
                this.alias!!.active = false
            } else {
                Toast.makeText(this, this.resources.getString(R.string.error_edit_active) + "\n" + result, Toast.LENGTH_SHORT).show()
            }
            isChangingActivationStatus = false
            setPage()
        }, this.alias!!.id)
    }


    private suspend fun activateAlias() {
        networkHelper.activateSpecificAlias({ alias, result ->
            if (alias != null) {
                this.alias!!.active = true
            } else {
                Toast.makeText(this, this.resources.getString(R.string.error_edit_active) + "\n" + result, Toast.LENGTH_SHORT).show()
            }
            isChangingActivationStatus = false
            setPage()
        }, this.alias!!.id)
    }

    private fun setChart(forwarded: Float, replied: Float, blocked: Float, sent: Float) {

        binding.activityManageAliasChart.animate().alpha(1.0f)

        ResizeAnimation(
            binding.activityManageAliasChart,
            8f,
            binding.activityManageAliasChart.height.toFloat(),
            8f,
            binding.activityManageAliasChart.width.toFloat(),
            800
        ).start()


        val color1 = if (this.alias!!.active) R.color.portalOrange else R.color.md_grey_500
        val color2 = if (this.alias!!.active) R.color.portalBlue else R.color.md_grey_600
        val color3 = if (this.alias!!.active) R.color.easternBlue else R.color.md_grey_700
        val color4 = if (this.alias!!.active) R.color.softRed else R.color.md_grey_800

        val listOfDonutSection: ArrayList<DonutSection> = arrayListOf()
        var donutCap = 0f
        // DONUT
        val section1 = DonutSection(
            name = forwarded.toInt().toString(),
            color = ContextCompat.getColor(this, color1),
            amount = forwarded
        )
        // Always show section 1
        listOfDonutSection.add(section1)
        donutCap += forwarded

        if (replied > 0) {
            val section2 = DonutSection(
                name = replied.toInt().toString(),
                color = ContextCompat.getColor(this, color2),
                amount = replied
            )
            listOfDonutSection.add(section2)
            donutCap += replied
        }

        if (sent > 0) {
            val section3 = DonutSection(
                name = sent.toInt().toString(),
                color = ContextCompat.getColor(this, color3),
                amount = sent
            )
            listOfDonutSection.add(section3)
            donutCap += sent
        }

        if (blocked > 0) {
            val section4 = DonutSection(
                name = blocked.toInt().toString(),
                color = ContextCompat.getColor(this, color4),
                amount = blocked
            )
            listOfDonutSection.add(section4)
            donutCap += blocked
        }
        binding.activityManageAliasChart.cap = donutCap

        // Sort the list by amount so that the biggest number will fill the whole ring
        binding.activityManageAliasChart.submitData(listOfDonutSection.sortedBy { it.amount })
        // DONUT


        binding.activityManageAliasForwardedCount.text = this.resources.getString(R.string.d_forwarded, forwarded.toInt())
        binding.activityManageAliasRepliesBlockedCount.text = this.resources.getString(R.string.d_blocked, blocked.toInt())
        binding.activityManageAliasSentCount.text = this.resources.getString(R.string.d_sent, sent.toInt())
        binding.activityManageAliasRepliedCount.text = this.resources.getString(R.string.d_replied, replied.toInt())
    }

}