package host.stjin.anonaddy.widget

import android.content.Intent
import android.widget.RemoteViewsService

class AliasWidget2RemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return AliasWidget2RemoteViewsFactory(this)
    }
}