package host.stjin.anonaddy.widget

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import android.widget.RemoteViewsService.RemoteViewsFactory
import host.stjin.anonaddy.R
import host.stjin.anonaddy.SettingsManager
import host.stjin.anonaddy.models.Aliases
import host.stjin.anonaddy.utils.DateTimeUtils
import host.stjin.anonaddy.utils.GsonTools
import host.stjin.anonaddy.widget.AliasWidgetProvider.AliasWidgetValues.COPY_ACTION
import host.stjin.anonaddy.widget.AliasWidgetProvider.AliasWidgetValues.NAVIGATE
import host.stjin.anonaddy.widget.AliasWidgetProvider.AliasWidgetValues.OPEN_ACTION


class AliasWidgetRemoteViewsFactory(private val mContext: Context) : RemoteViewsFactory {

    private var aliasList: ArrayList<Aliases>? = null

    override fun onCreate() {
        aliasList = arrayListOf()
    }

    override fun onDestroy() {
        aliasList?.clear()
    }

    override fun getCount(): Int {
        return aliasList?.size ?: 0
    }

    override fun getViewAt(position: Int): RemoteViews? {
        // position will always range from 0 to getCount() - 1.
        // construct a remote views item based on our widget item xml file, and set the
        // text based on the position.
        val rv = RemoteViews(mContext.packageName, R.layout.widget_aliases_listview_list_item)
        rv.setTextViewText(R.id.widget_aliases_listview_list_title, aliasList?.get(position)?.email)


        val description: String = if (aliasList?.get(position)?.description.isNullOrEmpty()) {
            mContext.resources.getString(
                R.string.created_at_s,
                DateTimeUtils.turnStringIntoLocalString(aliasList?.get(position)?.created_at)
            )
        } else {
            aliasList?.get(position)?.description.toString()
        }
        rv.setTextViewText(R.id.widget_aliases_listview_list_description, description)


        // Next, set a fill-intent which will be used to fill-in the pending intent template
        // which is set on the collection view in StackWidgetProvider.

        val extras = Bundle()
        extras.putString(NAVIGATE, aliasList!![position].email)
        extras.putString(COPY_ACTION, aliasList!![position].email)
        val copyIntent = Intent()
        copyIntent.putExtras(extras)


        val extras2 = Bundle()
        extras2.putString(NAVIGATE, aliasList!![position].id)
        extras2.putString(OPEN_ACTION, aliasList!![position].id)
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

    override fun getLoadingView(): RemoteViews? {
        return null
    }

    override fun getViewTypeCount(): Int {
        return 1
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    override fun onDataSetChanged() {
        val settingsManager = SettingsManager(true, mContext)
        val aliasesJson = settingsManager.getSettingsString(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_DATA_ALIASES)


        val aliasesList = aliasesJson?.let { GsonTools.jsonToAliasObject(mContext, it) }

        if (aliasesList != null) {
            if (aliasesList.size >= 2) {

                // Sort by emails forwarded
                aliasesList.sortByDescending { it.emails_forwarded }

                // Get the top 15
                aliasList = aliasesList.take(15) as ArrayList<Aliases>?
            }
        }


    }

}