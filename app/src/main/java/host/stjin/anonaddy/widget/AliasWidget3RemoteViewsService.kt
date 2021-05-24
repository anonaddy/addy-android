package host.stjin.anonaddy.widget

import android.content.Intent
import android.widget.RemoteViewsService

class AliasWidget3RemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return AliasWidget3RemoteViewsFactory(this)
    }
}