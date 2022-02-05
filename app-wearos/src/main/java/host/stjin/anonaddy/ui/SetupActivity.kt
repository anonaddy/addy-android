package host.stjin.anonaddy.ui

import android.app.Activity
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.Wearable
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.ActivitySetupBinding

class SetupActivity : Activity(), DataClient.OnDataChangedListener {

    private lateinit var binding: ActivitySetupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySetupBinding.inflate(layoutInflater)
        setTheme(R.style.AppTheme)
        setContentView(binding.root)
        startAnimation(binding.wearosActivitySetupLogo)
        requestSetup()

        binding.wearosActivitySetupLogo.setOnClickListener {
            startAnimation(binding.wearosActivitySetupLogo)
            requestSetup()
        }
    }

    private fun requestSetup() {
        val nodeClient = Wearable.getNodeClient(this)
        nodeClient.connectedNodes.addOnCompleteListener { nodes ->
            if (nodes.result.any()) {
                nodeClient.localNode.addOnCompleteListener { localNode ->
                    binding.wearosActivitySetupText.text = this.resources.getString(R.string.setup_wearos_check_paired_device)
                    // Send a message to all connected nodes basically broadcasting itself.
                    // Nodes with the app installed will receive this message and open the setup sheet
                    for (node in nodes.result) {
                        Wearable.getMessageClient(this).sendMessage(node.id, "/requestsetup", localNode.result.displayName.toByteArray())
                    }
                }
            } else {
                noNodesFound()
            }
        }.addOnFailureListener {
            noNodesFound()
        }.addOnCanceledListener {
            noNodesFound()
        }
    }

    private fun noNodesFound() {
        binding.wearosActivitySetupText.text = this.resources.getString(R.string.setup_wearos_no_paired_device)

        // No nodes found, let's check again in 5 seconds
        Handler(Looper.getMainLooper()).postDelayed({
           requestSetup()
        }, 5000)
    }

    public override fun onResume() {
        super.onResume()
        Wearable.getDataClient(this).addListener(this)
    }

    override fun onPause() {
        super.onPause()
        Wearable.getDataClient(this).removeListener(this)
    }

    private fun startAnimation(wearOSActivitySetupLogo: ImageView) {
        when (val drawable = wearOSActivitySetupLogo.drawable) {
            is AnimatedVectorDrawableCompat -> {
                drawable.start()
            }
            is AnimatedVectorDrawable -> {
                drawable.start()
            }
        }
    }

    override fun onDataChanged(p0: DataEventBuffer) {
        finish()
    }

}