package host.stjin.anonaddy.tiles


import androidx.core.content.ContextCompat
import androidx.wear.tiles.ActionBuilders
import androidx.wear.tiles.ColorBuilders.argb
import androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters
import androidx.wear.tiles.DimensionBuilders.dp
import androidx.wear.tiles.LayoutElementBuilders.Box
import androidx.wear.tiles.LayoutElementBuilders.Column
import androidx.wear.tiles.LayoutElementBuilders.FontStyles
import androidx.wear.tiles.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER
import androidx.wear.tiles.LayoutElementBuilders.Image
import androidx.wear.tiles.LayoutElementBuilders.Layout
import androidx.wear.tiles.LayoutElementBuilders.LayoutElement
import androidx.wear.tiles.LayoutElementBuilders.Row
import androidx.wear.tiles.LayoutElementBuilders.Spacer
import androidx.wear.tiles.LayoutElementBuilders.Text
import androidx.wear.tiles.ModifiersBuilders.Background
import androidx.wear.tiles.ModifiersBuilders.Clickable
import androidx.wear.tiles.ModifiersBuilders.Corner
import androidx.wear.tiles.ModifiersBuilders.Modifiers
import androidx.wear.tiles.ModifiersBuilders.Padding
import androidx.wear.tiles.ModifiersBuilders.Semantics
import androidx.wear.tiles.RequestBuilders.ResourcesRequest
import androidx.wear.tiles.RequestBuilders.TileRequest
import androidx.wear.tiles.ResourceBuilders
import androidx.wear.tiles.ResourceBuilders.Resources
import androidx.wear.tiles.TileBuilders.Tile
import androidx.wear.tiles.TileService
import androidx.wear.tiles.TimelineBuilders.Timeline
import androidx.wear.tiles.TimelineBuilders.TimelineEntry
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import host.stjin.anonaddy.BuildConfig
import host.stjin.anonaddy.R
import host.stjin.anonaddy.ui.alias.AliasActivity
import host.stjin.anonaddy.ui.alias.CreateAliasActivity
import host.stjin.anonaddy.ui.alias.ManageAliasActivity
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
class FavoriteAliasesTileService : TileService() {
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
    private val ID_IC_STAR = "ic_star_tinted"


    override fun onTileRequest(
        requestParams: TileRequest
    ): ListenableFuture<Tile> = serviceScope.future {

        val aliases = CacheHelper.getBackgroundServiceCacheFavoriteAliasesData(this@FavoriteAliasesTileService)
        val encryptedSettingsManager = try {
            SettingsManager(true, this@FavoriteAliasesTileService)
        } catch (e: Exception) {
            null
        }

        Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            // Creates a timeline to hold one or more tile entries for a specific time periods.
            .setTimeline(
                Timeline.Builder()
                    .addTimelineEntry(
                        TimelineEntry.Builder()
                            .setLayout(
                                if (encryptedSettingsManager?.getSettingsString(SettingsManager.PREFS.API_KEY) == null) {
                                    Layout.Builder().setRoot(setupLayout(requestParams.deviceParameters!!)).build()
                                } else {
                                    Layout.Builder().setRoot(layout(aliases, requestParams.deviceParameters!!)).build()
                                }
                            )
                            .build()
                    )
                    .build()
            ).build()

    }


    /**
     * https://developer.android.com/training/wearables/tiles#resources
     */
    @Deprecated("Deprecated in Java")
    override fun onResourcesRequest(requestParams: ResourcesRequest): ListenableFuture<Resources> =
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
                    ID_IC_STAR,
                    ResourceBuilders.ImageResource.Builder()
                        .setAndroidResourceByResId(
                            ResourceBuilders.AndroidImageResourceByResId.Builder()
                                .setResourceId(R.drawable.ic_star_tinted)
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
            Text.Builder()
                .setText(resources.getString(R.string.tile_favorite_aliases_title))
                .setFontStyle(
                    FontStyles
                        .title3(deviceParameters)
                        .setColor(
                            argb(ContextCompat.getColor(baseContext, R.color.md_theme_primary))
                        )
                        .build()
                )
                .build()
        )
        .addContent(Spacer.Builder().setHeight(SPACING_TITLE_SUBTITLE).build())
        .addContent(
            Text.Builder()
                .setText(resources.getString(R.string.tile_favorite_aliases_subtitle))
                .setFontStyle(
                    FontStyles
                        .caption1(deviceParameters)
                        .setColor(
                            argb(ContextCompat.getColor(baseContext, R.color.md_grey_500))
                        )
                        .build()
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
                            addNoFavoriteLayout()
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
                            addNoFavoriteLayout()
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
                            addNoFavoriteLayout()
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
                        .setContentDescription(getString(R.string.tile_favorite_aliases_label))
                        .build()
                )
                .build()
        )
        .build()

    private fun setupLayout(
        deviceParameters: DeviceParameters
    ): LayoutElement = Column.Builder()
        .addContent(
            Text.Builder()
                .setText(resources.getString(R.string.tile_favorite_aliases_title))
                .setFontStyle(
                    FontStyles
                        .title3(deviceParameters)
                        .setColor(
                            argb(ContextCompat.getColor(baseContext, R.color.md_theme_primary))
                        )
                        .build()
                )
                .build()
        )
        .addContent(Spacer.Builder().setHeight(SPACING_TITLE_SUBTITLE).build())
        .addContent(
            Box.Builder().setModifiers(
                Modifiers.Builder()
                    .setPadding(Padding.Builder().setStart(dp(16f)).setEnd(dp(16f)).build()).build()
            ).addContent(
                Text.Builder()
                    .setText(resources.getString(R.string.tile_favorite_aliases_subtitle_not_logged_in))
                    .setMaxLines(3)
                    .setFontStyle(
                        FontStyles
                            .body1(deviceParameters)
                            .setColor(
                                argb(ContextCompat.getColor(baseContext, R.color.md_grey_500))
                            )
                            .build()
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
                        .setContentDescription(getString(R.string.tile_favorite_aliases_label))
                        .build()
                )
                .build()
        )
        .build()

    private fun addNoFavoriteLayout() = Box.Builder()
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
                        .setContentDescription(getString(R.string.tile_favorite_aliases_favorite))
                        .build()
                )
                .setClickable(
                    Clickable.Builder()
                        .setOnClick(
                            ActionBuilders.LaunchAction.Builder()
                                .setAndroidActivity(
                                    ActionBuilders.AndroidActivity.Builder()
                                        .setClassName(AliasActivity::class.java.name)
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
                .setResourceId(ID_IC_STAR)
                .build()
        )
        .build()


    private fun getClickAction(alias: Aliases): ActionBuilders.Action {
        return ActionBuilders.LaunchAction.Builder()
            .setAndroidActivity(
                ActionBuilders.AndroidActivity.Builder()
                    .setClassName(ManageAliasActivity::class.java.name)
                    .addKeyToExtraMapping("alias", ActionBuilders.stringExtra(alias.id))
                    .setPackageName(this@FavoriteAliasesTileService.packageName)
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
            Text.Builder()
                .setText(alias.local_part.take(2).uppercase())
                .setFontStyle(
                    FontStyles
                        .button(deviceParameters)
                        .setColor(
                            argb(ColorUtils.getMostPopularColor(this@FavoriteAliasesTileService, alias))
                        )
                        .build()
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
                        .setContentDescription(getString(R.string.tile_favorite_aliases_all))
                        .build()
                )
                .setClickable(
                    Clickable.Builder()
                        .setOnClick(
                            ActionBuilders.LaunchAction.Builder()
                                .setAndroidActivity(
                                    ActionBuilders.AndroidActivity.Builder()
                                        .setClassName(AliasActivity::class.java.name)
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
                .setResourceId(ID_IC_EMAIL_AT)
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
                        .setContentDescription(getString(R.string.tile_favorite_aliases_create))
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
                .setResourceId(ID_IC_ADD)
                .build()
        )
        .build()

}
