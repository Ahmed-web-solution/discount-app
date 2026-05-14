package com.example.productviewer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Foreground Service that keeps a lightweight TCP connection open to a host:port
 * and periodically sends a small ping to keep the connection alive. The service
 * will auto-reconnect on failures and runs independently of the Activity lifecycle.
 */
public class PingForegroundService extends Service {

    private static final String TAG = "PingForegroundService";
    private static final String CHANNEL_ID = "ping_service_channel";
    private static final int NOTIF_ID = 101;

    private Thread workerThread;
    private volatile boolean running = false;

    private static final String HOST = "wf4km9vcsb.localto.net";
    private static final int PORT = 8458;
    private static final int PING_INTERVAL_MS = 30_000; // 30s

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = buildNotification();
        startForeground(NOTIF_ID, notification);

        startWorker();

        // Keep service running until explicitly stopped
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopWorker();
        stopForeground(true);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // not a bound service
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Background Ping Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Keeps a lightweight connection alive for the app");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        NotificationCompat.Builder b = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Connection active")
                .setContentText("Keeping lightweight data ping alive")
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true);

        return b.build();
    }

    private void startWorker() {
        if (running) {
            Log.d(TAG, "Worker already running");
            return;
        }

        running = true;
        workerThread = new Thread(() -> {
            Socket socket = null;
            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    socket = new Socket();
                    socket.connect(new InetSocketAddress(HOST, PORT), 5000);
                    socket.setSoTimeout(PING_INTERVAL_MS);
                    OutputStream out = socket.getOutputStream();
                    Log.i(TAG, "Connected to " + HOST + ":" + PORT);

                    while (running && !Thread.currentThread().isInterrupted()) {
                        try {
                            out.write('\n');
                            out.flush();
                        } catch (IOException e) {
                            Log.w(TAG, "Ping write failed: " + e.getMessage());
                            break; // break inner loop to reconnect
                        }

                        try { Thread.sleep(PING_INTERVAL_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                    }

                } catch (IOException e) {
                    Log.w(TAG, "Failed to connect: " + e.getMessage());
                } finally {
                    if (socket != null) {
                        try { socket.close(); } catch (IOException ignored) {}
                        socket = null;
                    }
                }

                // Wait a short time before trying to reconnect
                try { Thread.sleep(2000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
            }

            running = false;
            Log.i(TAG, "Worker thread exiting");
        }, "PingWorker");

        workerThread.setDaemon(true);
        workerThread.start();
    }

    private void stopWorker() {
        running = false;
        if (workerThread != null) {
            workerThread.interrupt();
            try { workerThread.join(1500); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            workerThread = null;
        }
    }
}
