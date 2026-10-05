package host.stjin.anonaddy.widget

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import android.widget.RemoteViewsService.RemoteViewsFactory
import host.stjin.anonaddy.R
import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy.widget.AliasWidget2Provider.AliasWidget2Values.COPY_ACTION
import host.stjin.anonaddy.widget.AliasWidget2Provider.AliasWidget2Values.NAVIGATE
import host.stjin.anonaddy.widget.AliasWidget2Provider.AliasWidget2Values.OPEN_ACTION
import host.stjin.anonaddy.widget.AliasWidget2Provider.AliasWidget2Values.OPEN_APP
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.models.Aliases
import host.stjin.anonaddy_shared.utils.CacheHelper
import host.stjin.anonaddy_shared.utils.DateTimeUtils


class AliasWidget2RemoteViewsFactory(private val context: Context) : RemoteViewsFactory {

    private var aliasList: ArrayList<Aliases>? = null

    override fun onCreate() {
        aliasList = arrayListOf()
    }

    override fun onDestroy() {
        aliasList?.clear()
    }

    override fun getCount(): Int {
        return (aliasList?.size ?: 0) + 1
    }

    override fun getViewAt(position: Int): RemoteViews {
        // position will always range from 0 to getCount() - 1.

        // Check if view is last view
        if (position == count - 1) {

            val extras = Bundle()
            extras.putString(OPEN_APP, null)
            val openIntent = Intent()
            openIntent.putExtras(extras)

            val rv = RemoteViews(context.packageName, R.layout.widget_2_aliases_listview_list_more)
            rv.setOnClickFillInIntent(R.id.widget_aliases_listview_more, openIntent)

            return rv
        } else {
            val rv = RemoteViews(context.packageName, R.layout.widget_2_aliases_listview_list_item)
            val currentAlias = aliasList?.getOrNull(position) ?: return rv

            val encryptedSettingsManager = ServiceLocator.encryptedSettingsManager
            if (encryptedSettingsManager.getSettingsBool(SettingsManager.PREFS.PRIVACY_MODE)) {
                // If privacy mode, hide email
                rv.setTextViewText(R.id.widget_aliases_listview_list_title, context.resources.getString(R.string.alias_hidden))
            } else {
                rv.setTextViewText(R.id.widget_aliases_listview_list_title, currentAlias.email)
            }


            val description: String = if (currentAlias.description.isNullOrEmpty()) {
                context.resources.getString(
                    R.string.created_at_s,
                    DateTimeUtils.convertStringToLocalTimeZoneString(currentAlias.created_at)
                )
            } else {
                currentAlias.description.toString()
            }
            rv.setTextViewText(R.id.widget_aliases_listview_list_description, description)


            // Next, set a fill-intent which will be used to fill-in the pending intent template
            // which is set on the collection view in StackWidgetProvider.

            val extras = Bundle()
            extras.putString(NAVIGATE, currentAlias.email)
            extras.putString(COPY_ACTION, currentAlias.email)
            val copyIntent = Intent()
            copyIntent.putExtras(extras)


            val extras2 = Bundle()
            extras2.putString(NAVIGATE, currentAlias.id)
            extras2.putString(OPEN_ACTION, currentAlias.id)
            val openIntent = Intent()
            openIntent.putExtras(extras2)


            rv.setOnClickFillInIntent(R.id.widget_aliases_listview_list, openIntent)
            rv.setOnClickFillInIntent(R.id.widget_aliases_listview_list_copy, copyIntent)

            // You can do heaving lifting in here, synchronously. For example, if you need to
            // process an image, fetch something from the network, etc., it is ok to do it here,
            // synchronously. A loading view will show up in lieu of the actual contents in the
            // interim.
            // Return the remote views object.
            return rv

        }
    }

    override fun getLoadingView(): RemoteViews? {
        return null
    }

    override fun getViewTypeCount(): Int {
        return 2
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    override fun onDataSetChanged() {
        val aliasesList = CacheHelper.getBackgroundServiceCacheMostActiveAliasesData(context)
        if (aliasesList != null) {
            aliasList = aliasesList
        }
    }

}