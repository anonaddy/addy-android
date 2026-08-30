package host.stjin.anonaddy.tiles


import androidx.core.content.ContextCompat
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER
import androidx.wear.protolayout.LayoutElementBuilders.Image
import androidx.wear.protolayout.LayoutElementBuilders.Layout
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.LayoutElementBuilders.Row
import androidx.wear.protolayout.LayoutElementBuilders.Spacer
import androidx.wear.protolayout.ModifiersBuilders.Background
import androidx.wear.protolayout.ModifiersBuilders.Clickable
import androidx.wear.protolayout.ModifiersBuilders.Corner
import androidx.wear.protolayout.ModifiersBuilders.Modifiers
import androidx.wear.protolayout.ModifiersBuilders.Padding
import androidx.wear.protolayout.ModifiersBuilders.Semantics
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.ResourceBuilders.Resources
import androidx.wear.protolayout.TimelineBuilders.Timeline
import androidx.wear.protolayout.TimelineBuilders.TimelineEntry
import androidx.wear.protolayout.TypeBuilders
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.tiles.RequestBuilders.ResourcesRequest
import androidx.wear.tiles.RequestBuilders.TileRequest
import androidx.wear.tiles.TileBuilders.Tile
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import host.stjin.anonaddy.BuildConfig
import host.stjin.anonaddy.R
import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy.ui.aliases.AliasesActivity
import host.stjin.anonaddy.ui.aliases.CreateAliasActivity
import host.stjin.anonaddy.ui.aliases.ManageAliasActivity
import host.stjin.anonaddy.utils.ColorUtils
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.models.Aliases
import host.stjin.anonaddy_shared.utils.CacheHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.guava.future

/**
 * Creates a Favorite Aliases Tile, showing your favorite aliases and a button to view more aliases.
 *
 * The main function, [onTileRequest], is triggered when the system calls for a tile and implements
 * ListenableFuture which allows the Tile to be returned asynchronously.
 */
class PinnedAliasesTileService : TileService() {
    // For coroutines, use a custom scope we can cancel when the service is destroyed
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)


    // Updating this version triggers a new call to onResourcesRequest(). This is useful for dynamic
// resources, the contents of which change even though their id stays the same (e.g. a graph).
// In this sample, our resources are all fixed, so we use a constant value.
    private val RESOURCES_VERSION = "1"

    // Dimensions
    private val CIRCLE_SIZE = 48f
    private val SPACING_TITLE_SUBTITLE = dp(4f)
    private val SPACING_SUBTITLE_ALIASES = dp(12f)
    private val SPACING_BUTTONS = dp(8f)
    private val ICON_SIZE = dp(24f)

    // Resource identifiers for images
    private val ID_IC_EMAIL_AT = "ic_email_at_tinted"
    private val ID_IC_ADD = "ic_add_tinted"
    private val ID_IC_PINNED = "ic_pinned_tinted"


    override fun onTileRequest(
        requestParams: TileRequest
    ): ListenableFuture<Tile> = serviceScope.future {

        val aliases = CacheHelper.getBackgroundServiceCachePinnedAliasesData(this@PinnedAliasesTileService)
        val encryptedSettingsManager = try {
            ServiceLocator.encryptedSettingsManager
        } catch (e: Exception) {
            null
        }

        val deviceParams = requestParams.deviceConfiguration

        Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            // Creates a timeline to hold one or more tile entries for a specific time periods.
            .setTileTimeline(
                Timeline.Builder()
                    .addTimelineEntry(
                        TimelineEntry.Builder()
                            .setLayout(
                                if (encryptedSettingsManager?.getSettingsString(SettingsManager.PREFS.API_KEY) == null) {
                                    Layout.Builder().setRoot(setupLayout(deviceParams)).build()
                                } else {
                                    Layout.Builder().setRoot(layout(aliases, deviceParams)).build()
                                }
                            )
                            .build()
                    )
                    .build()
            ).build()

    }


    override fun onTileResourcesRequest(requestParams: ResourcesRequest): ListenableFuture<Resources> =
        Futures.immediateFuture(
            Resources.Builder()
                .setVersion(RESOURCES_VERSION)
                .addIdToImageMapping(
                    ID_IC_EMAIL_AT,
                    ResourceBuilders.ImageResource.Builder()
                        .setAndroidResourceByResId(
                            ResourceBuilders.AndroidImageResourceByResId.Builder()
                                .setResourceId(R.drawable.ic_email_at_tinted)
                                .build()
                        )
                        .build()
                ).addIdToImageMapping(
                    ID_IC_ADD,
                    ResourceBuilders.ImageResource.Builder()
                        .setAndroidResourceByResId(
                            ResourceBuilders.AndroidImageResourceByResId.Builder()
                                .setResourceId(R.drawable.ic_add_tinted)
                                .build()
                        )
                        .build()
                ).addIdToImageMapping(
                    ID_IC_PINNED,
                    ResourceBuilders.ImageResource.Builder()
                        .setAndroidResourceByResId(
                            ResourceBuilders.AndroidImageResourceByResId.Builder()
                                .setResourceId(R.drawable.ic_pinned_tinted)
                                .build()
                        )
                        .build()
                )
                .build()
        )

    override fun onDestroy() {
        super.onDestroy()
        // Cleans up the coroutine
        serviceJob.cancel()
    }

    private fun layout(
        aliases: List<Aliases>?,
        deviceParameters: DeviceParameters
    ): LayoutElement = Column.Builder()
        .addContent(
            Text.Builder(baseContext, resources.getString(R.string.tile_pinned_aliases_title))
                .setTypography(Typography.TYPOGRAPHY_TITLE3)
                .setColor(
                    argb(ContextCompat.getColor(baseContext, R.color.md_theme_primary))
                )
                .build()
        )
        .addContent(Spacer.Builder().setHeight(SPACING_TITLE_SUBTITLE).build())
        .addContent(
            Text.Builder(baseContext, resources.getString(R.string.tile_pinned_aliases_subtitle))
                .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                .setColor(
                    argb(ContextCompat.getColor(baseContext, R.color.md_grey_500))
                )
                .build()
        )
        .addContent(Spacer.Builder().setHeight(SPACING_SUBTITLE_ALIASES).build())
        .addContent(
            Row.Builder()
                // There is always 1 alias
                .addContent(
                    when {
                        !aliases.isNullOrEmpty() -> {
                            aliasLayout(
                                alias = aliases[0],
                                deviceParameters = deviceParameters,
                                clickable = Clickable.Builder()
                                    .setOnClick(getClickAction(aliases[0]))
                                    .build()
                            )
                        }

                        else -> {
                            addNoPinnedLayout()
                        }
                    }
                )
                .addContent(Spacer.Builder().setWidth(SPACING_BUTTONS).build())
                .addContent(
                    when {
                        !aliases.isNullOrEmpty() && aliases.size > 1 -> {
                            aliasLayout(
                                alias = aliases[1],
                                deviceParameters = deviceParameters,
                                clickable = Clickable.Builder()
                                    .setOnClick(getClickAction(aliases[1]))
                                    .build()
                            )
                        }

                        else -> {
                            addNoPinnedLayout()
                        }
                    }
                )
                .addContent(Spacer.Builder().setWidth(SPACING_BUTTONS).build())
                .addContent(
                    when {
                        !aliases.isNullOrEmpty() && aliases.size > 2 -> {
                            aliasLayout(
                                alias = aliases[2],
                                deviceParameters = deviceParameters,
                                clickable = Clickable.Builder()
                                    .setOnClick(getClickAction(aliases[2]))
                                    .build()
                            )
                        }

                        else -> {
                            addNoPinnedLayout()
                        }
                    }
                )
                .build()
        )
        .addContent(
            Row.Builder()
                .addContent(allAliasesLayout())
                .addContent(Spacer.Builder().setWidth(SPACING_BUTTONS).build())
                .addContent(addAliasesLayout())
                .build()
        )
        .setModifiers(
            Modifiers.Builder()
                .setSemantics(
                    Semantics.Builder()
                        .setContentDescription(getString(R.string.tile_pinned_aliases_label))
                        .build()
                )
                .build()
        )
        .build()

    private fun setupLayout(
        deviceParameters: DeviceParameters
    ): LayoutElement = Column.Builder()
        .addContent(
            Text.Builder(baseContext, resources.getString(R.string.tile_pinned_aliases_title))
                .setTypography(Typography.TYPOGRAPHY_TITLE3)
                .setColor(
                    argb(ContextCompat.getColor(baseContext, R.color.md_theme_primary))
                )
                .build()
        )
        .addContent(Spacer.Builder().setHeight(SPACING_TITLE_SUBTITLE).build())
        .addContent(
            Box.Builder().setModifiers(
                Modifiers.Builder()
                    .setPadding(Padding.Builder().setStart(dp(16f)).setEnd(dp(16f)).build()).build()
            ).addContent(
                Text.Builder(baseContext, resources.getString(R.string.tile_pinned_aliases_subtitle_not_logged_in))
                    .setMaxLines(3)
                    .setTypography(Typography.TYPOGRAPHY_BODY1)
                    .setColor(
                        argb(ContextCompat.getColor(baseContext, R.color.md_grey_500))
                    )
                    .build()
            ).build()


        )
        .addContent(Spacer.Builder().setHeight(SPACING_SUBTITLE_ALIASES).build())
        .addContent(
            Row.Builder()
                .addContent(addAliasesLayout())
                .build()
        )
        .setModifiers(
            Modifiers.Builder()
                .setSemantics(
                    Semantics.Builder()
                        .setContentDescription(getString(R.string.tile_pinned_aliases_label))
                        .build()
                )
                .build()
        )
        .build()

    private fun addNoPinnedLayout() = Box.Builder()
        .setWidth(dp(CIRCLE_SIZE))
        .setHeight(dp(CIRCLE_SIZE))
        .setModifiers(
            Modifiers.Builder()
                .setBackground(
                    Background.Builder()
                        .setColor(
                            argb(ContextCompat.getColor(baseContext, R.color.colorSurface))
                        )
                        .setCorner(
                            Corner.Builder().setRadius(dp(CIRCLE_SIZE / 2)).build()
                        )
                        .build()
                )
                .setSemantics(
                    Semantics.Builder()
                        .setContentDescription(getString(R.string.tile_pinned_aliases_pin))
                        .build()
                )
                .setClickable(
                    Clickable.Builder()
                        .setOnClick(
                            ActionBuilders.LaunchAction.Builder()
                                .setAndroidActivity(
                                    ActionBuilders.AndroidActivity.Builder()
                                        .setClassName(AliasesActivity::class.java.name)
                                        .setPackageName(BuildConfig.APPLICATION_ID)
                                        .build()
                                ).build()
                        ).build()
                )
                .build()
        )
        .addContent(
            Image.Builder()
                .setWidth(ICON_SIZE)
                .setHeight(ICON_SIZE)
                .setResourceId(TypeBuilders.StringProp.Builder(ID_IC_PINNED).build())
                .build()
        )
        .build()


    private fun getClickAction(alias: Aliases): ActionBuilders.Action {
        return ActionBuilders.LaunchAction.Builder()
            .setAndroidActivity(
                ActionBuilders.AndroidActivity.Builder()
                    .setClassName(ManageAliasActivity::class.java.name)
                    .addKeyToExtraMapping("alias", ActionBuilders.stringExtra(alias.id))
                    .setPackageName(this@PinnedAliasesTileService.packageName)
                    .build()
            ).build()
    }

    private fun aliasLayout(
        alias: Aliases,
        deviceParameters: DeviceParameters,
        clickable: Clickable
    ) = Box.Builder().apply {
        val modifiersBuilder = Modifiers.Builder()
            .setClickable(clickable)
            .setSemantics(
                Semantics.Builder()
                    .setContentDescription(alias.local_part)
                    .build()
            )

        setWidth(dp(CIRCLE_SIZE))
        setHeight(dp(CIRCLE_SIZE))
        setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
        modifiersBuilder
            .setBackground(
                Background.Builder()
                    .setColor(
                        argb(ContextCompat.getColor(baseContext, R.color.colorSurface))
                    )
                    .setCorner(
                        Corner.Builder()
                            .setRadius(dp(CIRCLE_SIZE / 2))
                            .build()
                    )
                    .build()
            )
        addContent(
            Text.Builder(baseContext, alias.local_part.take(2).uppercase())
                .setTypography(Typography.TYPOGRAPHY_BUTTON)
                .setColor(
                    argb(ColorUtils.getMostPopularColor(this@PinnedAliasesTileService, alias))
                )
                .build()
        )

        setModifiers(modifiersBuilder.build())
    }
        .build()

    private fun allAliasesLayout() = Box.Builder()
        .setWidth(dp(CIRCLE_SIZE))
        .setHeight(dp(CIRCLE_SIZE))
        .setModifiers(
            Modifiers.Builder()
                .setBackground(
                    Background.Builder()
                        .setColor(
                            argb(ContextCompat.getColor(baseContext, R.color.md_theme_secondaryContainer))
                        )
                        .setCorner(
                            Corner.Builder().setRadius(dp(CIRCLE_SIZE / 2)).build()
                        )
                        .build()
                )
                .setSemantics(
                    Semantics.Builder()
                        .setContentDescription(getString(R.string.tile_pinned_aliases_all))
                        .build()
                )
                .setClickable(
                    Clickable.Builder()
                        .setOnClick(
                            ActionBuilders.LaunchAction.Builder()
                                .setAndroidActivity(
                                    ActionBuilders.AndroidActivity.Builder()
                                        .setClassName(AliasesActivity::class.java.name)
                                        .setPackageName(BuildConfig.APPLICATION_ID)
                                        .build()
                                ).build()
                        ).build()
                )
                .build()
        )
        .addContent(
            Image.Builder()
                .setWidth(ICON_SIZE)
                .setHeight(ICON_SIZE)
                .setResourceId(TypeBuilders.StringProp.Builder(ID_IC_EMAIL_AT).build())
                .build()
        )
        .build()

    private fun addAliasesLayout() = Box.Builder()
        .setWidth(dp(CIRCLE_SIZE))
        .setHeight(dp(CIRCLE_SIZE))
        .setModifiers(
            Modifiers.Builder()
                .setBackground(
                    Background.Builder()
                        .setColor(
                            argb(ContextCompat.getColor(baseContext, R.color.md_theme_secondaryContainer))
                        )
                        .setCorner(
                            Corner.Builder().setRadius(dp(CIRCLE_SIZE / 2)).build()
                        )
                        .build()
                )
                .setSemantics(
                    Semantics.Builder()
                        .setContentDescription(getString(R.string.tile_pinned_aliases_create))
                        .build()
                )
                .setClickable(
                    Clickable.Builder()
                        .setOnClick(
                            ActionBuilders.LaunchAction.Builder()
                                .setAndroidActivity(
                                    ActionBuilders.AndroidActivity.Builder()
                                        .setClassName(CreateAliasActivity::class.java.name)
                                        .setPackageName(BuildConfig.APPLICATION_ID)
                                        .build()
                                ).build()
                        ).build()
                )
                .build()
        )
        .addContent(
            Image.Builder()
                .setWidth(ICON_SIZE)
                .setHeight(ICON_SIZE)
                .setResourceId(TypeBuilders.StringProp.Builder(ID_IC_ADD).build())
                .build()
        )
        .build()

}
