package host.stjin.anonaddy.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.*
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class ManageSubscriptionActivity : BaseActivity(), BillingClientStateListener, PurchasesUpdatedListener, PurchasesResponseListener {

    private lateinit var settingsManager: SettingsManager
    private lateinit var encryptedSettingsManager: SettingsManager

    private var currentSubscriptionSku: String? = null
    private var currentSubscriptionPurchaseToken: String? = null
    private var hasNewSubscription = false

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


        if ((application as AddyIoApp).userResource.disabled == true) {
            binding.activityManageSubscriptionNSV.visibility = View.GONE
            binding.root.findViewById<View>(R.id.fragment_subscription_other_platform).visibility = View.GONE
            binding.root.findViewById<View>(R.id.fragment_subscription_account_disabled).visibility = View.VISIBLE
        } else {
            binding.root.findViewById<View>(R.id.fragment_subscription_account_disabled).visibility = View.GONE
            if ((application as AddyIoApp).userResource.subscription_type == "google" || (application as AddyIoApp).userResource.subscription_type == null) {
                binding.activityManageSubscriptionNSV.visibility = View.VISIBLE
                binding.root.findViewById<View>(R.id.fragment_subscription_other_platform).visibility = View.GONE
            } else {
                binding.activityManageSubscriptionNSV.visibility = View.GONE
                binding.root.findViewById<View>(R.id.fragment_subscription_other_platform).visibility = View.VISIBLE
            }
        }



    }

    private var selectedTab = "pro"
    private lateinit var billingClient: BillingClient
    private var products = mutableListOf<ProductDetails>()


    private fun setupBillingClient() {
        billingClient = BillingClient.newBuilder(this)
            .setListener(this)
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .build()

        billingClient.startConnection(this)
    }

    private fun getPurchasedItem() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
        billingClient.queryPurchasesAsync(params.build(), this)
    }

    private suspend fun queryProducts() {
        products.clear()
        binding.productsContainer.removeAllViews()

        val params = QueryProductDetailsParams.newBuilder().setProductList(
            listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(selectedTab)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            )
        )
            .build()

        val productDetailsResult = withContext(Dispatchers.IO) {
            billingClient.queryProductDetails(params)
        }

        when (productDetailsResult.billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (productDetailsResult.productDetailsList?.isNotEmpty() == true) {
                    products.addAll(productDetailsResult.productDetailsList!!)
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
                Uri.parse("https://addy.io/privacy?ref=appstore")
            )
            startActivity(browserIntent)
        }
        binding.termsOfServiceButton.setOnClickListener {
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://addy.io/terms?ref=appstore")
            )
            startActivity(browserIntent)
        }
        binding.fragmentSubscriptionAccountDisabled.termsOfServiceButton.setOnClickListener {
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://addy.io/terms?ref=appstore")
            )
            startActivity(browserIntent)
        }
        binding.fragmentSubscriptionAccountDisabled.contactUsButton.setOnClickListener {
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://addy.io/contact?ref=appstore")
            )
            startActivity(browserIntent)
        }
        binding.restorePurchasesButton.setOnClickListener { restorePurchases() }
        binding.manageSubscriptionButton.setOnClickListener {
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/account/subscriptions?package=host.stjin.anonaddy")
            )
            startActivity(browserIntent)
        }
        binding.fragmentSubscriptionAccountDisabled.manageSubscriptionButton.setOnClickListener {
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
            listOf(
                this.resources.getString(R.string.why_subscribe_reason_1_pro),
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
                this.resources.getString(R.string.why_subscribe_reason_14)
            )

        } else if (binding.liteButton.isChecked) {
            listOf(
                this.resources.getString(R.string.why_subscribe_reason_1_lite),
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
                this.resources.getString(R.string.why_subscribe_reason_14)
            )
        } else {
            listOf(
                this.resources.getString(R.string.why_subscribe_reason_9),
                this.resources.getString(R.string.why_subscribe_reason_10),
                this.resources.getString(R.string.why_subscribe_reason_11),
                this.resources.getString(R.string.why_subscribe_reason_12),
                this.resources.getString(R.string.why_subscribe_reason_13),
                this.resources.getString(R.string.why_subscribe_reason_14)
            )
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
        setupFeaturesView()
        lifecycleScope.launch {
            queryProducts()
        }
    }

    private fun updateProductsDisplay() {
        products.forEach { productDetails ->
            productDetails.subscriptionOfferDetails?.forEach { subscriptionOfferDetails ->
                val productView = layoutInflater.inflate(R.layout.product_item, binding.productsContainer, false)
                productView.findViewById<TextView>(R.id.product_item_name).text = productDetails.name
                productView.findViewById<TextView>(R.id.product_item_desc).text = productDetails.description

                if (subscriptionOfferDetails.pricingPhases.pricingPhaseList.first().billingPeriod == "P1M") {
                    productView.findViewById<TextView>(R.id.product_item_price).text = this.resources.getString(
                        R.string.price_format_month,
                        subscriptionOfferDetails.pricingPhases.pricingPhaseList.first().formattedPrice
                    )
                } else if (subscriptionOfferDetails.pricingPhases.pricingPhaseList.first().billingPeriod == "P1Y") {
                    productView.findViewById<TextView>(R.id.product_item_price).text = this.resources.getString(
                        R.string.price_format_year,
                        subscriptionOfferDetails.pricingPhases.pricingPhaseList.first().formattedPrice
                    )
                }

                val buyButton = productView.findViewById<MaterialButton>(R.id.product_item_button)

                buyButton.text = getString(R.string.subscribe_now)
                buyButton.setOnClickListener {
                    launchPurchaseFlow(productDetails, subscriptionOfferDetails.offerToken)
                }

                binding.productsContainer.addView(productView)
            }

        }


        if (products.isNotEmpty()) {
            binding.activityManageSubscriptionProductsAndOverview.visibility = View.VISIBLE
            binding.activityManageSubscriptionProgressbar.visibility = View.GONE
        }
    }

    private fun launchPurchaseFlow(productDetails: ProductDetails, offerToken: String) {
        if (currentSubscriptionSku != null && currentSubscriptionSku != productDetails.productId) {

            // If the user wants to go to lite, The user already paid for the more expensive tier, so they keep access until the next billing date.
            // Else The user receives access immediately while keeping the same billing period.
            val replacementMode =
                if (productDetails.productId == "lite") BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.DEFERRED else BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.CHARGE_PRORATED_PRICE

            val billingParams = BillingFlowParams.newBuilder().setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .setOfferToken(offerToken)
                        .build()
                )
            ).setSubscriptionUpdateParams(
                BillingFlowParams.SubscriptionUpdateParams.newBuilder()
                    .setOldPurchaseToken(currentSubscriptionPurchaseToken!!)
                    .setSubscriptionReplacementMode(
                        replacementMode
                    )
                    .build()
            ).build()

            billingClient.launchBillingFlow(
                this,
                billingParams
            )
        } else {
            val productDetailsParamsList = listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .setOfferToken(offerToken)
                    .build()
            )
            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build()
            billingClient.launchBillingFlow(this, billingFlowParams)
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
                lifecycleScope.launch {
                    getPurchasedItem()
                }
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
            // Only handle not acknowledged purchases
            if (!purchase.isAcknowledged) {
                // First notify the server, when success. Acknowledge the purchase
                lifecycleScope.launch {
                    notifyInstanceAboutSubscription(purchase) { succeeded ->
                        if (succeeded) {
                            val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.purchaseToken)
                                .build()
                            billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                                    hasNewSubscription = true
                                    finish()
                                }
                            }

                        }
                    }
                }
            }
        }
    }

    override fun finish() {
        val resultIntent = Intent()
        resultIntent.putExtra("hasNewSubscription", hasNewSubscription)
        setResult(RESULT_OK, resultIntent)
        super.finish()
    }

    private suspend fun notifyInstanceAboutSubscription(purchase: Purchase, callback: (Boolean) -> Unit) {
        binding.root.findViewById<View>(R.id.fragment_subscription_notify_server).visibility = View.VISIBLE
        binding.activityManageSubscriptionNSV.visibility = View.GONE


        val networkHelper = NetworkHelper(this)
        networkHelper.notifyServerForSubscriptionChange({ userResource, _ ->
            if (userResource != null) {
                (application as AddyIoApp).userResource = userResource
                callback(true)
            } else {
                binding.root.findViewById<View>(R.id.fragment_subscription_notify_server).visibility = View.GONE
                binding.activityManageSubscriptionNSV.visibility = View.VISIBLE

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


                callback(false)
            }
        }, purchase.purchaseToken, purchase.products.first())
    }

    private fun restorePurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
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

    override fun onBillingServiceDisconnected() {
        billingClient.startConnection(this)
    }

    override fun onBillingSetupFinished(p0: BillingResult) {
        if (p0.responseCode == BillingClient.BillingResponseCode.OK) {
            // Here you can query products
            lifecycleScope.launch {
                queryProducts()
                getPurchasedItem()
            }
        }
    }

    override fun onQueryPurchasesResponse(p0: BillingResult, p1: MutableList<Purchase>) {
        if (p0.responseCode == BillingClient.BillingResponseCode.OK) {
            for (purchase in p1) {
                // Handle each purchase
                currentSubscriptionSku = purchase.products.first()
                currentSubscriptionPurchaseToken = purchase.purchaseToken
                //Log.d("Subscription", "Current subscribed productId: ${purchase.products.joinToString(" ,")}")
                //Log.d("Subscription", "Current subscribed productId: $currentSubscriptionSku")
                //Log.d("Subscription", "Current subscribed purchaseToken: $currentSubscriptionPurchaseToken")
            }
            lifecycleScope.launch {
                queryProducts()
            }
        } else {
            // Handle error or no purchase found
            //Log.e("Billing", "Error or no subscriptions found: " + p0.debugMessage)
        }
    }


}
