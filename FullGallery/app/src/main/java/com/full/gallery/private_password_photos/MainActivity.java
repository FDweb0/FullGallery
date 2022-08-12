package com.full.gallery.private_password_photos;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.fulldive.startapppopups.PopupManager;
import com.fulldive.startapppopups.donation.DonationManager;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.collect.ImmutableList;

import java.io.File;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static String APP_DIRECTORY = "MyPictureApp/";
    private static String MEDIA_DIRECTORY = APP_DIRECTORY + "PictureApp";

    private final int MY_PERMISSIONS = 100;
    private final int PHOTO_CODE = 200;
    private final int SELECT_PICTURE = 300;

    private ImageView mSetImage;
    private Button mOptionButton;
    private RelativeLayout mRlView;

    private String mPath;

    private Button donateBtn,suportBtn;
    private static final String TAG =
            "com.full.gallery.private_password_photos";
    private BillingClient billingClient;
    private ProductDetails productDetails;
    private Purchase purchase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSetImage = (ImageView) findViewById(R.id.set_picture);
        mOptionButton = (Button) findViewById(R.id.show_options_button);
        mRlView = (RelativeLayout) findViewById(R.id.rl_view);
        donateBtn = (Button) findViewById(R.id.donate);
        suportBtn = (Button) findViewById(R.id.support);

        if(mayRequestStoragePermission()) {
            mOptionButton.setEnabled(true);
        }else {
            mOptionButton.setEnabled(false);
        }
        billingSetup();
        mOptionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showOptions();
            }
        });

        donateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (productDetails != null) {
                    makePurchase();
                }else{
                    Toast.makeText(MainActivity.this,"No Product",Toast.LENGTH_LONG).show();
                }
            }
        });

        suportBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new PopupManager().onAppStarted(
                        MainActivity.this,
                        com.android.billingclient.BuildConfig.APPLICATION_ID,
                        true,
                        false,
                        true,
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

                Toast.makeText(MainActivity.this,"Purchase Completed!",Toast.LENGTH_LONG).
                        show();

            });
    }

    private boolean mayRequestStoragePermission() {

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return true;

        if((checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) &&
                (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED))
            return true;

        if((shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) ||
                (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA))){
            Snackbar.make(mRlView, "Los permisos son necesarios para poder usar la aplicaciÃ³n",
                    Snackbar.LENGTH_INDEFINITE).setAction(android.R.string.ok, new View.OnClickListener() {
                @TargetApi(Build.VERSION_CODES.M)
                @Override
                public void onClick(View v) {
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, MY_PERMISSIONS);
                }
            });
        }else{
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, MY_PERMISSIONS);
        }

        return false;
    }

    private void showOptions() {
        final CharSequence[] option = {"Tomar foto", "Elegir de galeria", "Cancelar"};
        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Eleige una opciÃ³n");
        builder.setItems(option, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(option[which] == "Tomar foto"){
                    openCamera();
                }else if(option[which] == "Elegir de galeria"){
                    Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    intent.setType("image/*");
                    startActivityForResult(intent.createChooser(intent, "Selecciona app de imagen"), SELECT_PICTURE);
                }else {
                    dialog.dismiss();
                }
            }
        });

        builder.show();
    }

    private void openCamera() {
        File file = new File(Environment.getExternalStorageDirectory(), MEDIA_DIRECTORY);
        boolean isDirectoryCreated = file.exists();

        if(!isDirectoryCreated)
            isDirectoryCreated = file.mkdirs();

        if(isDirectoryCreated){
            Long timestamp = System.currentTimeMillis() / 1000;
            String imageName = timestamp.toString() + ".jpg";

            mPath = Environment.getExternalStorageDirectory() + File.separator + MEDIA_DIRECTORY
                    + File.separator + imageName;

            File newFile = new File(mPath);

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(newFile));
            startActivityForResult(intent, PHOTO_CODE);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("file_path", mPath);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mPath = savedInstanceState.getString("file_path");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == RESULT_OK){
            switch (requestCode){
                case PHOTO_CODE:
                    MediaScannerConnection.scanFile(this,
                            new String[]{mPath}, null,
                            new MediaScannerConnection.OnScanCompletedListener() {
                                @Override
                                public void onScanCompleted(String path, Uri uri) {
                                    Log.i("ExternalStorage", "Scanned " + path + ":");
                                    Log.i("ExternalStorage", "-> Uri = " + uri);
                                }
                            });


                    Bitmap bitmap = BitmapFactory.decodeFile(mPath);
                    mSetImage.setImageBitmap(bitmap);
                    break;
                case SELECT_PICTURE:
                    Uri path = data.getData();
                    mSetImage.setImageURI(path);
                    break;

            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == MY_PERMISSIONS){
            if(grantResults.length == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(MainActivity.this, "Permisos aceptados", Toast.LENGTH_SHORT).show();
                mOptionButton.setEnabled(true);
            }
        }else{
            showExplanation();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_settings){
            startActivity(new Intent(MainActivity.this, com.full.gallery.private_password_photos.Settings.class));
        }
        return super.onOptionsItemSelected(item);
    }

    private void showExplanation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Permisos denegados");
        builder.setMessage("Para usar las funciones de la app necesitas aceptar los permisos");
        builder.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            }
        });
        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                finish();
            }
        });

        builder.show();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        DonationManager.INSTANCE.destroy();
    }
}