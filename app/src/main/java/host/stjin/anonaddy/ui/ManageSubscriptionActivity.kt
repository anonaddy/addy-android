package host.stjin.anonaddy.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.google.android.material.button.MaterialButton
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.ActivityManageSubscriptionBinding
import host.stjin.anonaddy.utils.InsetUtil
import host.stjin.anonaddy.utils.MaterialDialogHelper
import host.stjin.anonaddy_shared.AddyIoApp
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.models.LOGIMPORTANCE
import host.stjin.anonaddy_shared.utils.LoggingHelper
import kotlinx.coroutines.launch


class ManageSubscriptionActivity : BaseActivity(), PurchasesUpdatedListener {

    private lateinit var settingsManager: SettingsManager
    private lateinit var encryptedSettingsManager: SettingsManager

    private var currentSubscriptionSku: String? = null

    private lateinit var binding: ActivityManageSubscriptionBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageSubscriptionBinding.inflate(layoutInflater)
        InsetUtil.applyBottomInset(binding.activityManageSubscriptionNSV)
        val view = binding.root
        setContentView(view)

        settingsManager = SettingsManager(false, this)
        encryptedSettingsManager = SettingsManager(true, this)
        setupToolbar(
            R.string.manage_subscription,
            binding.activityManageSubscriptionNSV,
            binding.appsettingsToolbar,
            R.drawable.ic_credit_card
        )

        setupBillingClient()
        updateUi()

        if ((application as AddyIoApp).userResource.subscription_type == "google" || (application as AddyIoApp).userResource.subscription_type == null) {
            binding.activityManageSubscriptionNSV.visibility = View.VISIBLE
            binding.root.findViewById<View>(R.id.fragment_subscription_other_platform).visibility = View.GONE
        } else {
            binding.activityManageSubscriptionNSV.visibility = View.GONE
            binding.root.findViewById<View>(R.id.fragment_subscription_other_platform).visibility = View.VISIBLE
        }

    }

    private var selectedTab = "pro"
    private lateinit var billingClient: BillingClient
    private var products: List<ProductDetails> = emptyList()


    private fun setupBillingClient() {
        billingClient = BillingClient.newBuilder(this)
            .setListener(this)
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingResponseCode.OK) {
                    // Here you can query products
                    queryProducts()
                    getPurchasedItem()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
            }
        })
    }

    private fun getPurchasedItem(){
        // After initializing and successfully connecting the BillingClient:
        billingClient.queryPurchasesAsync(BillingClient.ProductType.SUBS) { billingResult, purchases ->
            if (billingResult.responseCode == BillingResponseCode.OK) {
                for (purchase in purchases) {
                    // Handle each purchase
                    currentSubscriptionSku = purchase.skus[0] // Again, assuming one SKU per purchase for simplicity
                    Log.d("Subscription", "Current subscribed productId: $currentSubscriptionSku")

                    updateProductsDisplay()
                }
            } else {
                // Handle error or no purchase found
                Log.e("Billing", "Error or no subscriptions found: " + billingResult.debugMessage)
            }
        }
    }

    private fun queryProducts() {
        val params = QueryProductDetailsParams.newBuilder()
        val productList = mutableListOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("pro_yearly")
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("pro_monthly")
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("lite_yearly")
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
        )

        params.setProductList(productList).let { queryParams ->
            billingClient.queryProductDetailsAsync(queryParams.build()) { billingResult, productDetailsList ->
                if (billingResult.responseCode == BillingResponseCode.OK) {
                    products = productDetailsList
                    runOnUiThread { updateProductsDisplay() }
                }
            }
        }
    }

    private fun updateUi() {
        binding.proButton.setOnClickListener {
            binding.proButton.isChecked = true
            changeSubscriptionPlan("pro")
        }
        binding.liteButton.setOnClickListener {
            binding.liteButton.isChecked = true
            changeSubscriptionPlan("lite")
        }
        binding.privacyPolicyButton.setOnClickListener {
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://addy.io/privacy/?ref=appstore")
            )
            startActivity(browserIntent)        }
        binding.termsOfServiceButton.setOnClickListener {
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://addy.io/terms?ref=appstore")
            )
            startActivity(browserIntent)        }
        binding.restorePurchasesButton.setOnClickListener { restorePurchases() }
        binding.manageSubscriptionButton.setOnClickListener {
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/account/subscriptions?package=host.stjin.anonaddy")
            )
            startActivity(browserIntent)
        }
        updateProductsDisplay()
        setupFeaturesView()
    }


    private lateinit var featuresContainer: LinearLayout
    private fun setupFeaturesView() {
        featuresContainer = findViewById(R.id.activity_manage_subscription_features_LL)

        val items = if (binding.proButton.isChecked) {
            listOf(this.resources.getString(R.string.why_subscribe_reason_1_pro),
                this.resources.getString(R.string.why_subscribe_reason_8_pro),
                this.resources.getString(R.string.why_subscribe_reason_2_pro),
                this.resources.getString(R.string.why_subscribe_reason_3_pro),
                this.resources.getString(R.string.why_subscribe_reason_4_pro),
                this.resources.getString(R.string.why_subscribe_reason_5_pro),
                this.resources.getString(R.string.why_subscribe_reason_6_pro),
                this.resources.getString(R.string.why_subscribe_reason_13),
                this.resources.getString(R.string.why_subscribe_reason_12),
                this.resources.getString(R.string.why_subscribe_reason_9),
                this.resources.getString(R.string.why_subscribe_reason_10),
                this.resources.getString(R.string.why_subscribe_reason_11),
                this.resources.getString(R.string.why_subscribe_reason_7_pro),
                this.resources.getString(R.string.why_subscribe_reason_14))

        } else if (binding.liteButton.isChecked) {
            listOf(this.resources.getString(R.string.why_subscribe_reason_1_lite),
                this.resources.getString(R.string.why_subscribe_reason_8_lite),
                this.resources.getString(R.string.why_subscribe_reason_2_lite),
                this.resources.getString(R.string.why_subscribe_reason_3_lite),
                this.resources.getString(R.string.why_subscribe_reason_4_lite),
                this.resources.getString(R.string.why_subscribe_reason_5_lite),
                this.resources.getString(R.string.why_subscribe_reason_6_lite),
                this.resources.getString(R.string.why_subscribe_reason_13),
                this.resources.getString(R.string.why_subscribe_reason_12),
                this.resources.getString(R.string.why_subscribe_reason_9),
                this.resources.getString(R.string.why_subscribe_reason_10),
                this.resources.getString(R.string.why_subscribe_reason_11),
                this.resources.getString(R.string.why_subscribe_reason_7_lite),
                this.resources.getString(R.string.why_subscribe_reason_14))
        } else {
            listOf(this.resources.getString(R.string.why_subscribe_reason_9),
                this.resources.getString(R.string.why_subscribe_reason_10),
                this.resources.getString(R.string.why_subscribe_reason_11),
                this.resources.getString(R.string.why_subscribe_reason_12),
                this.resources.getString(R.string.why_subscribe_reason_13),
                this.resources.getString(R.string.why_subscribe_reason_14))
        }

        setupFeaturesView(items)
    }

    private fun setupFeaturesView(items: List<String>) {
        featuresContainer.removeAllViews()

        for (item in items) {
            val itemLayout = layoutInflater.inflate(R.layout.features_item, featuresContainer, false)
            val textView: TextView = itemLayout.findViewById(R.id.itemText)
            textView.text = item

            featuresContainer.addView(itemLayout)
        }
    }

    private fun changeSubscriptionPlan(type: String) {
        selectedTab = type
        updateProductsDisplay()
        setupFeaturesView()
    }

    private fun updateProductsDisplay() {
        //binding.productsContainer.removeAllViews()
        products.filter { it.productId.startsWith(selectedTab) }.forEach { productDetails ->
            val productView = layoutInflater.inflate(R.layout.product_item, binding.productsContainer, false)
            productView.findViewById<TextView>(R.id.product_item_name).text = productDetails.name
            productView.findViewById<TextView>(R.id.product_item_desc).text = productDetails.description
            productView.findViewById<TextView>(R.id.product_item_price).text = productDetails.oneTimePurchaseOfferDetails?.formattedPrice ?: this.resources.getString(R.string.price_not_available)

            val buyButton = productView.findViewById<MaterialButton>(R.id.product_item_button)

            if (currentSubscriptionSku == productDetails.productId) {
                buyButton.text = getString(R.string.active)
                buyButton.isEnabled = false
                buyButton.alpha = 0.8f
                buyButton.setOnClickListener(null)
            } else {
                buyButton.text = getString(R.string.subscribe_now)
                buyButton.setOnClickListener {
                    launchPurchaseFlow(productDetails)
                }

            }

            binding.productsContainer.addView(productView)
        }


        if (products.isNotEmpty()) {
            binding.activityManageSubscriptionProductsAndOverview.visibility = View.VISIBLE
            binding.activityManageSubscriptionProgressbar.visibility = View.GONE
        }
    }

    private fun launchPurchaseFlow(productDetails: ProductDetails) {
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
        )
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()
        billingClient.launchBillingFlow(this, billingFlowParams)
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            // Handle an error caused by a user cancelling the purchase flow.
        } else {
            LoggingHelper(this).addLog(
                LOGIMPORTANCE.CRITICAL.int,
                billingResult.debugMessage,
                "onPurchasesUpdated",
                billingResult.responseCode.toString()
            )
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingResponseCode.OK) {
                        lifecycleScope.launch {
                            notifyInstanceAboutSubscription(purchase)
                        }
                    }
                }
            }
        }
    }

    private suspend fun notifyInstanceAboutSubscription(purchase: Purchase) {
        val networkHelper = NetworkHelper(this)

        networkHelper.notifyServerForSubscriptionChange({ userResource, result ->
                if (userResource != null) {
                    (application as AddyIoApp).userResource = userResource
                    finish()
                } else {
                    MaterialDialogHelper.showMaterialDialog(
                        context = this,
                        title = this.resources.getString(R.string.subscription_processing_failed),
                        message = this.resources.getString(
                            R.string.subscription_processing_failed_desc,
                            (application as AddyIoApp).userResource.id,
                            purchase.purchaseToken,
                            purchase.products.first()
                        ),
                        icon = R.drawable.ic_credit_card,
                        positiveButtonText = this.resources.getString(R.string.dismiss)
                    ).setCancelable(false).show()
                }
            }, purchase.purchaseToken, purchase.products.first())



    }

    private fun restorePurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingResponseCode.OK) {
                purchasesList.forEach { purchase ->
                    handlePurchase(purchase)
                }
            }
        }
    }

    override fun onDestroy() {
        if (::billingClient.isInitialized) {
            billingClient.endConnection()
        }
        super.onDestroy()
    }

}
