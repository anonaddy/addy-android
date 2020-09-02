package host.stjin.anonaddy.widget

import android.content.Intent
import android.widget.RemoteViewsService

class AliasWidgetRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return AliasWidgetRemoteViewsFactory(this.applicationContext)
    }
}