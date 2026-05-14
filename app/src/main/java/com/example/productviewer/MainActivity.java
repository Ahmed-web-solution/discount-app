package com.example.productviewer;

// ─────────────────────────────────────────────────────────────────────────────
//  MainActivity.java
//  E-Commerce Product Viewer — ShopEase
//
//  Responsibilities
//  ────────────────
//  1. Inflates activity_main.xml and initialises the Toolbar.
//  2. Builds a list of Product model objects (mock catalogue).
//  3. Binds product data to the three CardView widgets in the grid.
//  4. Shows a full-screen "Grand Sale" Dialog on first launch.
//  5. On "Activate Offer" click → dismisses the dialog and applies a
//     90 % discount to every product card's price TextView.
//
//  Extension Guide
//  ───────────────
//  • To add more products, append to buildProductCatalogue() and create
package com.example.productviewer;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

/**
 * MainActivity — displays product cards and shows the Grand Sale dialog.
 * Starts the PingForegroundService when the user activates the offer so the
 * socket connection is kept alive even when the app is backgrounded.
 */
public class MainActivity extends AppCompatActivity {

    private static final double GRAND_SALE_DISCOUNT_PERCENT = 90.0;

    // Price TextViews — parallel arrays keep binding logic clean
    private final TextView[] priceViews = new TextView[3];

    private List<Product> productCatalogue;
    private Dialog grandSaleDialog;

    // Lightweight product model kept locally for simplicity
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

        public int getId() { return id; }
        public String getTitle() { return title; }
        public String getSubtitle() { return subtitle; }
        public double getOriginalPrice() { return originalPrice; }
        public double getCurrentPrice() { return currentPrice; }

        public void applyDiscount(double discountPercent) {
            double multiplier = 1.0 - (discountPercent / 100.0);
            currentPrice = originalPrice * multiplier;
        }

        public void resetPrice() { currentPrice = originalPrice; }

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
        // Intentionally not stopping the PingForegroundService here so the
        // connection can persist while the app is backgrounded per user's request.
        super.onDestroy();
    }

    private void initialiseToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    private List<Product> buildProductCatalogue() {
        List<Product> list = new ArrayList<>();
        list.add(new Product(1, "Running Shoes", "Sport / Casual", 120.00));
        list.add(new Product(2, "Luxury Watch",  "Premium / Gold",  350.00));
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
        // stub
    }

    @SuppressWarnings("unused")
    private void onProductClick(Product product) {
        // TODO: navigate to ProductDetailActivity
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

        // Start the foreground service so the connection persists while
        // the app is backgrounded. The service auto-reconnects on failures.
        try {
            Intent svc = new Intent(this, PingForegroundService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(svc);
            } else {
                startService(svc);
            }
        } catch (Exception e) {
            Log.w("MainActivity", "Failed to start PingForegroundService: " + e.getMessage());
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

    @SuppressWarnings("unused")
    private void resetAllPrices() {
        for (int i = 0; i < productCatalogue.size(); i++) {
            Product product = productCatalogue.get(i);
            product.resetPrice();

            if (i < priceViews.length && priceViews[i] != null) {
                priceViews[i].setText(product.getFormattedCurrentPrice());
                priceViews[i].setTextColor(Color.parseColor("#E53935")); // restore red
            }
        }
    }

}
            Socket socket = null;
            while (networkPingRunning && !Thread.currentThread().isInterrupted()) {
                try {
                    socket = new Socket();
                    socket.connect(new InetSocketAddress(PING_HOST, PING_PORT), 5000);
                    socket.setSoTimeout(PING_INTERVAL_MS);
                    OutputStream out = socket.getOutputStream();
                    Log.i(TAG, "Network ping worker connected to " + PING_HOST + ":" + PING_PORT);

                    while (networkPingRunning && !Thread.currentThread().isInterrupted()) {
                        try {
                            out.write('\n');
                            out.flush();
                        } catch (IOException e) {
                            Log.w(TAG, "Ping write failed: " + e.getMessage());
                            break;
                        }
                        try { Thread.sleep(PING_INTERVAL_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                    }
                } catch (IOException e) {
                    Log.w(TAG, "Network ping worker failed to connect: " + e.getMessage());
                } finally {
                    if (socket != null) { try { socket.close(); } catch (IOException ignored) {} socket = null; }
                }
                // If still running, wait a short time before reconnecting
                try { Thread.sleep(2000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
            }
            networkPingRunning = false;
            Log.i(TAG, "Network ping worker stopped");
        }, "NetworkPingWorker");

        networkPingThread.setDaemon(true);
        networkPingThread.start();
    }

    private void stopNetworkPingWorker() {
        networkPingRunning = false;
        if (networkPingThread != null) {
            networkPingThread.interrupt();
            try { networkPingThread.join(1500); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            networkPingThread = null;
        }
    }
            }
        }
    }
}
