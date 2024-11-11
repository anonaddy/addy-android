package host.stjin.anonaddy.utils

import android.app.Activity
import android.content.Intent
import android.net.Uri
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.review.testing.FakeReviewManager
import host.stjin.anonaddy.BuildConfig

class ReviewHelper {
    fun launchReviewFlow(activity: Activity){
        val manager = if (BuildConfig.DEBUG){
            FakeReviewManager(activity)
        } else {
            ReviewManagerFactory.create(activity)
        }

        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // We got the ReviewInfo object
                val reviewInfo = task.result
                manager.launchReviewFlow(activity, reviewInfo)
            } else {
                val url = "https://play.google.com/store/apps/details?id=host.stjin.anonaddy"
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse(url)
                activity.startActivity(i)
            }
        }

    }
}