package host.stjin.anonaddy.ui

import android.app.ActivityManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
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
        setupUI()

        if ((application as AddyIoApp).userResource.subscription_type == "google" || (application as AddyIoApp).userResource.subscription_type == null) {
            binding.activityManageSubscriptionNSV.visibility = View.VISIBLE
            binding.root.findViewById<View>(R.id.fragment_subscription_other_platform).visibility = View.GONE
        } else {
            binding.activityManageSubscriptionNSV.visibility = View.GONE
            binding.root.findViewById<View>(R.id.fragment_subscription_other_platform).visibility = View.VISIBLE
        }

    }

    private var selectedTab = "annually"
    private lateinit var billingClient: BillingClient
    private var products: List<ProductDetails> = emptyList()


    private fun setupBillingClient() {
        billingClient = BillingClient.newBuilder(this)
            .setListener(this)
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    // Here you can query products
                    queryProducts()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
            }
        })
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
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("lite_monthly")
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        params.setProductList(productList).let { queryParams ->
            billingClient.queryProductDetailsAsync(queryParams.build()) { billingResult, productDetailsList ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    products = productDetailsList
                    runOnUiThread { updateProductsDisplay() }
                }
            }
        }
    }

    private fun setupUI() {
        binding.annuallyButton.setOnClickListener {
            binding.annuallyButton.isChecked = true
            changeSubscriptionType("annually")
        }
        binding.monthlyButton.setOnClickListener {
            binding.monthlyButton.isChecked = true
            changeSubscriptionType("monthly")
        }
        binding.restorePurchasesButton.setOnClickListener { restorePurchases() }
        updateProductsDisplay()
    }

    private fun changeSubscriptionType(type: String) {
        selectedTab = type
        updateProductsDisplay()
    }

    private fun updateProductsDisplay() {
        binding.productsContainer.removeAllViews()
        products.filter { it.productId.endsWith(selectedTab) }.forEach { productDetails ->
            val productView = layoutInflater.inflate(R.layout.product_item, binding.productsContainer, false)
            productView.findViewById<TextView>(R.id.product_item_name).text = productDetails.name
            productView.findViewById<TextView>(R.id.product_item_desc).text = productDetails.description
            productView.findViewById<TextView>(R.id.product_item_price).text = productDetails.oneTimePurchaseOfferDetails?.formattedPrice ?: this.resources.getString(R.string.price_not_available)

            val buyButton = productView.findViewById<MaterialButton>(R.id.product_item_button)
            buyButton.text = getString(R.string.subscribe_now)
            buyButton.setOnClickListener {
                launchPurchaseFlow(productDetails)
            }

            binding.productsContainer.addView(productView)
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
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
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
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
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

}
