package host.stjin.anonaddy.service


import androidx.core.content.ContextCompat
import androidx.wear.tiles.ActionBuilders
import androidx.wear.tiles.ColorBuilders.argb
import androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters
import androidx.wear.tiles.DimensionBuilders.dp
import androidx.wear.tiles.LayoutElementBuilders.*
import androidx.wear.tiles.ModifiersBuilders.*
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
import host.stjin.anonaddy.ui.SplashActivity
import host.stjin.anonaddy.ui.alias.ManageAliasActivity
import host.stjin.anonaddy.utils.ColorUtils
import host.stjin.anonaddy.utils.FavoriteAliasHelper
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.models.Aliases
import host.stjin.anonaddy_shared.utils.GsonTools
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.guava.future

// Updating this version triggers a new call to onResourcesRequest(). This is useful for dynamic
// resources, the contents of which change even though their id stays the same (e.g. a graph).
// In this sample, our resources are all fixed, so we use a constant value.
private const val RESOURCES_VERSION = "1"

// Dimensions
private const val CIRCLE_SIZE = 48f
private val SPACING_TITLE_SUBTITLE = dp(4f)
private val SPACING_SUBTITLE_CONTACTS = dp(12f)
private val SPACING_CONTACTS = dp(8f)
private val ICON_SIZE = dp(24f)

// Resource identifiers for images
private const val ID_IC_EMAIL_AT = "ic_email_at_tinted"
private const val ID_IC_ADD = "ic_add_tinted"
private const val ID_IC_STAR = "ic_star_tinted"

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

    override fun onTileRequest(
        requestParams: TileRequest
    ): ListenableFuture<Tile> = serviceScope.future {

        val aliases = getFavoriteAliases()

        if (!aliases.isNullOrEmpty()) {
            Tile.Builder()
                .setResourcesVersion(RESOURCES_VERSION)
                // Creates a timeline to hold one or more tile entries for a specific time periods.
                .setTimeline(
                    Timeline.Builder()
                        .addTimelineEntry(
                            TimelineEntry.Builder()
                                .setLayout(
                                    Layout.Builder().setRoot(layout(aliases, requestParams.deviceParameters!!)).build()
                                )
                                .build()
                        )
                        .build()
                ).build()
        } else {
            Tile.Builder()
                .setResourcesVersion(RESOURCES_VERSION)
                // Creates a timeline to hold one or more tile entries for a specific time periods.
                .setTimeline(
                    Timeline.Builder()
                        .addTimelineEntry(
                            TimelineEntry.Builder()
                                .setLayout(
                                    Layout.Builder().setRoot(noAliasesLayout(requestParams.deviceParameters!!)).build()
                                )
                                .build()
                        )
                        .build()
                ).build()
        }
    }

    private fun getFavoriteAliases(): List<Aliases>? {
        val favoriteAliases = FavoriteAliasHelper(this@FavoriteAliasesTileService).getFavoriteAliases()
        val aliasesJson = SettingsManager(
            true,
            this@FavoriteAliasesTileService
        ).getSettingsString(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_15_MOST_ACTIVE_ALIASES_DATA)

        val aliasList = aliasesJson?.let { GsonTools.jsonToAliasObject(this@FavoriteAliasesTileService, it) }
        return if (aliasList.isNullOrEmpty()) {
            null
        } else {
            if (favoriteAliases.isNullOrEmpty()) {
                aliasList.take(4)
            } else {
                aliasList.filter { it.id in favoriteAliases }
            }
        }
    }


    /**
     * https://developer.android.com/training/wearables/tiles#resources
     */
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
        aliases: List<Aliases>,
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
        .addContent(Spacer.Builder().setHeight(SPACING_SUBTITLE_CONTACTS).build())
        .addContent(
            Row.Builder()
                // There is always 1 alias
                .addContent(
                    aliasLayout(
                        alias = aliases[0],
                        deviceParameters = deviceParameters,
                        clickable = Clickable.Builder()
                            .setOnClick(getClickAction(aliases[0]))
                            .build()
                    )
                )
                .addContent(Spacer.Builder().setWidth(SPACING_CONTACTS).build())
                .addContent(
                    when {
                        aliases.size > 1 -> {
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
                .addContent(Spacer.Builder().setWidth(SPACING_CONTACTS).build())
                .addContent(
                    when {
                        aliases.size > 2 -> {
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
                .addContent(Spacer.Builder().setWidth(SPACING_CONTACTS).build())
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
                                        .setClassName(SplashActivity::class.java.toString())
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


    //TODO not working
    private fun getClickAction(aliases: Aliases): ActionBuilders.Action {
        return ActionBuilders.LaunchAction.Builder()
            .setAndroidActivity(
                ActionBuilders.AndroidActivity.Builder()
                    .setClassName(ManageAliasActivity::class.java.name)
                    //.addKeyToExtraMapping("alias") { ActionProto.AndroidExtra.getDefaultInstance(). }
                    .setPackageName(this@FavoriteAliasesTileService.packageName)
                    .build()
            ).build()
    }

    private fun noAliasesLayout(
        deviceParameters: DeviceParameters
    ): LayoutElement = Column.Builder()
        .addContent(
            Text.Builder()
                .setText(resources.getString(R.string.tile_favorite_aliases_title))
                .setFontStyle(
                    FontStyles
                        .title3(deviceParameters)
                        .setColor(
                            argb(ContextCompat.getColor(baseContext, R.color.colorSurface))
                        )
                        .build()
                )
                .build()
        )
        .addContent(Spacer.Builder().setHeight(SPACING_TITLE_SUBTITLE).build())
        .addContent(
            Text.Builder()
                .setText(resources.getString(R.string.tile_favorite_aliases_subtitle_no_aliases))
                .setFontStyle(
                    FontStyles
                        .caption1(deviceParameters)
                        .setColor(
                            argb(ContextCompat.getColor(baseContext, R.color.md_theme_outline))
                        )
                        .build()
                )
                .build()
        )
        .build()

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

        // Create an avatar based on the contact's initials
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
                                        .setClassName(SplashActivity::class.java.toString())
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
                                        .setClassName(SplashActivity::class.java.toString())
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
