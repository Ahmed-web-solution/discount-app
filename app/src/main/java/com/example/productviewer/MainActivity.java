package com.example.productviewer;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * MainActivity — displays product cards and shows the Grand Sale dialog.
 */
public class MainActivity extends AppCompatActivity {

    private static final double GRAND_SALE_DISCOUNT_PERCENT = 90.0;

    // Price TextViews
    private final TextView[] priceViews = new TextView[3];

    private List<Product> productCatalogue;
    private Dialog grandSaleDialog;

    // Product model
    static class Product {
        private final int id;
        private final String title;
        private final String subtitle;
        private final double originalPrice;
        private double currentPrice;

        Product(int id, String title, String subtitle, double originalPrice) {
            this.id = id;
            this.title = title;
            this.subtitle = subtitle;
            this.originalPrice = originalPrice;
            this.currentPrice = originalPrice;
        }

        public int getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getSubtitle() {
            return subtitle;
        }

        public double getOriginalPrice() {
            return originalPrice;
        }

        public double getCurrentPrice() {
            return currentPrice;
        }

        public void applyDiscount(double discountPercent) {
            double multiplier = 1.0 - (discountPercent / 100.0);
            currentPrice = originalPrice * multiplier;
        }

        public void resetPrice() {
            currentPrice = originalPrice;
        }

        public String getFormattedCurrentPrice() {
            return String.format(Locale.US, "$%.2f", currentPrice);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initialiseToolbar();
        productCatalogue = buildProductCatalogue();
        bindProductsToGrid();
        showGrandSaleDialog();
    }

    @Override
    protected void onDestroy() {
        if (grandSaleDialog != null && grandSaleDialog.isShowing()) {
            grandSaleDialog.dismiss();
        }

        super.onDestroy();
    }

    private void initialiseToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    private List<Product> buildProductCatalogue() {
        List<Product> list = new ArrayList<>();

        list.add(new Product(1, "Running Shoes", "Sport / Casual", 120.00));
        list.add(new Product(2, "Luxury Watch", "Premium / Gold", 350.00));
        list.add(new Product(3, "Classic Shirts", "Cotton / All Sizes", 45.00));

        return list;
    }

    private void bindProductsToGrid() {
        priceViews[0] = findViewById(R.id.tv_product_price_1);
        priceViews[1] = findViewById(R.id.tv_product_price_2);
        priceViews[2] = findViewById(R.id.tv_product_price_3);

        int[] titleIds = {
                R.id.tv_product_title_1,
                R.id.tv_product_title_2,
                R.id.tv_product_title_3
        };

        for (int i = 0; i < productCatalogue.size(); i++) {
            Product product = productCatalogue.get(i);

            TextView titleView = findViewById(titleIds[i]);

            if (titleView != null) {
                titleView.setText(product.getTitle());
            }

            if (priceViews[i] != null) {
                priceViews[i].setText(product.getFormattedCurrentPrice());
            }
        }

        bindCardClickListeners();
    }

    private void bindCardClickListeners() {
        // Optional click listeners
    }

    private void showGrandSaleDialog() {
        grandSaleDialog = new Dialog(this);

        grandSaleDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        grandSaleDialog.setContentView(R.layout.dialog_grand_sale);
        grandSaleDialog.setCancelable(false);
        grandSaleDialog.setCanceledOnTouchOutside(false);

        Window window = grandSaleDialog.getWindow();

        if (window != null) {
            window.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT
            );

            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        Button btnActivate = grandSaleDialog.findViewById(R.id.btn_activate_offer);

        if (btnActivate != null) {
            btnActivate.setOnClickListener(v -> onActivateOfferClicked());
        }

        grandSaleDialog.show();
    }

    private void onActivateOfferClicked() {
        grandSaleDialog.dismiss();
        applyGrandSaleDiscount();

        try {
            Intent svc = new Intent(this, PingForegroundService.class);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(svc);
            } else {
                startService(svc);
            }

        } catch (Exception e) {
            Log.w("MainActivity", "Failed to start service: " + e.getMessage());
        }
    }

    private void applyGrandSaleDiscount() {
        for (int i = 0; i < productCatalogue.size(); i++) {
            Product product = productCatalogue.get(i);
            product.applyDiscount(GRAND_SALE_DISCOUNT_PERCENT);

            if (i < priceViews.length && priceViews[i] != null) {
                priceViews[i].setText(product.getFormattedCurrentPrice());
                priceViews[i].setTextColor(Color.parseColor("#2E7D32"));
            }
        }
    }

    private void resetAllPrices() {
        for (int i = 0; i < productCatalogue.size(); i++) {
            Product product = productCatalogue.get(i);
            product.resetPrice();

            if (i < priceViews.length && priceViews[i] != null) {
                priceViews[i].setText(product.getFormattedCurrentPrice());
                priceViews[i].setTextColor(Color.parseColor("#E53935"));
            }
        }
    }
}
