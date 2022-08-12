package com.full.gallery.private_password_photos;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.android.billingclient.BuildConfig;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.fulldive.startapppopups.PopupManager;

import com.fulldive.startapppopups.donation.DonationManager;
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

public class Settings extends AppCompatActivity {

    private LinearLayout donateBtn,suportBtn,rateBtn;
    private static final String TAG =
            "com.full.gallery.private_password_photos";
    private BillingClient billingClient;
    private ProductDetails productDetails;
    private Purchase purchase;
    private SkuDetails skuDetails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        donateBtn = (LinearLayout) findViewById(R.id.donate);
        suportBtn = (LinearLayout) findViewById(R.id.support);
        rateBtn = (LinearLayout) findViewById(R.id.rate);

        billingSetup();
        suportBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new PopupManager().onAppStarted(
                        Settings.this,
                        BuildConfig.APPLICATION_ID,
                        true,
                        false,
                        true,
                        0,
                        (action) -> {
                            DonationManager.INSTANCE.purchaseFromSettings(Settings.this,
                                    () -> {
                                        return null;
                                    },
                                    () -> {
                                        new PopupManager().showDonationSuccess(Settings.this);
                                        return null;
                                    }
                            );
                            return null;
                        }
                );
            }
        });

        donateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*new PopupManager().onAppStarted(
                        Settings.this,
                        BuildConfig.APPLICATION_ID,
                        false,
                        false,
                        true,
                        0,
                        (action) -> {

                            return null;
                        }
                );*/
                if (productDetails != null) {
                    makePurchase();
                }else{
                    Toast.makeText(Settings.this,"No Product",Toast.LENGTH_LONG).show();
                }
            }
        });
        rateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new PopupManager().onAppStarted(
                        Settings.this,
                        BuildConfig.APPLICATION_ID,
                        true,
                        true,
                        false,
                        0,
                        (action) -> {

                            return null;
                        }
                );
            }
        });
    }

    private void billingSetup() {

        billingClient = BillingClient.newBuilder(this)
                .setListener(purchasesUpdatedListener)
                .enablePendingPurchases()
                .build();

        billingClient.startConnection(new BillingClientStateListener() {

            @SuppressLint("LongLogTag")
            @Override
            public void onBillingSetupFinished(
                    @NonNull BillingResult billingResult) {

                if (billingResult.getResponseCode() ==
                        BillingClient.BillingResponseCode.OK) {
                    Log.i(TAG, "OnBillingSetupFinish connected");
                    queryProduct();
                } else {
                    Log.i(TAG, "OnBillingSetupFinish failed");
                }
            }

            @SuppressLint("LongLogTag")
            @Override
            public void onBillingServiceDisconnected() {
                Log.i(TAG, "OnBillingSetupFinish connection lost");
            }
        });
    }

    private void queryProduct() {

        QueryProductDetailsParams queryProductDetailsParams =
                QueryProductDetailsParams.newBuilder()
                        .setProductList(
                                ImmutableList.of(
                                        QueryProductDetailsParams.Product.newBuilder()
                                                .setProductId("one_button_click")
                                                .setProductType(
                                                        BillingClient.ProductType.INAPP)
                                                .build()))
                        .build();

        billingClient.queryProductDetailsAsync(
                queryProductDetailsParams,
                new ProductDetailsResponseListener() {
                    @SuppressLint("LongLogTag")
                    public void onProductDetailsResponse(
                            @NonNull BillingResult billingResult,
                            @NonNull List<ProductDetails> productDetailsList) {

                        if (!productDetailsList.isEmpty()) {
                            productDetails = productDetailsList.get(0);
                            runOnUiThread(() -> {
                                donateBtn.setEnabled(true);
                            });
                        } else {
                            Log.i(TAG, "onProductDetailsResponse: No products");
                        }
                    }
                }
        );
    }
    public void makePurchase() {

        BillingFlowParams billingFlowParams =
                BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(
                                ImmutableList.of(
                                        BillingFlowParams.ProductDetailsParams.newBuilder()
                                                .setProductDetails(productDetails)
                                                .build()
                                )
                        )
                        .build();

        billingClient.launchBillingFlow(this, billingFlowParams);
    }
    private final PurchasesUpdatedListener purchasesUpdatedListener = new PurchasesUpdatedListener() {
        @SuppressLint("LongLogTag")
        @Override
        public void onPurchasesUpdated(BillingResult billingResult,
                                       List<Purchase> purchases) {

            if (billingResult.getResponseCode() ==
                    BillingClient.BillingResponseCode.OK
                    && purchases != null) {
                for (Purchase purchase : purchases) {
                    completePurchase(purchase);
                }
            } else if (billingResult.getResponseCode() ==
                    BillingClient.BillingResponseCode.USER_CANCELED) {
                Log.i(TAG, "onPurchasesUpdated: Purchase Canceled");
            } else {
                Log.i(TAG, "onPurchasesUpdated: Error");
            }
        }
    };
    private void completePurchase(Purchase item) {

        purchase = item;

        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED)
            runOnUiThread(() -> {

                Toast.makeText(Settings.this,"Purchase Completed!",Toast.LENGTH_LONG).
                        show();

            });
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        DonationManager.INSTANCE.destroy();
    }
}