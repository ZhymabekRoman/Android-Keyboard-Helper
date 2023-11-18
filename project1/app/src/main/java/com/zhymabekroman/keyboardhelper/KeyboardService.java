package com.zhymabekroman.keyboardhelper;
import java.util.List;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.Nullable;


public final class KeyboardService extends Service {
    class LocalBinder extends Binder {
        public final KeyboardService service = KeyboardService.this;
    }

    private final IBinder mBinder = new LocalBinder();
    private Handler handler = new Handler();
    private static final int NOTIFICATION_MODE_SILENT=1;
    private static final int APP_NOTIFICATION_ID=286;
    private static final String APP_NAME="Keyboard Service";
    private static final String APP_NOTIFICATION_CHANNEL_ID="keyboard_service_channel";
    private static final String APP_NOTIFICATION_CHANNEL_NAME="Keyboard Service App";
    private static final String ACTION_STOP_SERVICE="com.zhymabekroman.keyboardhelper.exit_service";

    private int lastOrientation = -1; // Initial value

    private Runnable toastRunnable = new Runnable() {
        @Override
        public void run() {
            int currentOrientation = getResources().getConfiguration().orientation;
            if (currentOrientation == lastOrientation) {
                // Chill out bro
            } else {
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
                    // Screen is in portrait mode
                    Settings.Secure.putString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD, "ru.yandex.androidkeyboard/com.android.inputmethod.latin.LatinIME");
                } else {
                    // Screen is in landscape
                    Settings.Secure.putString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD, "io.github.visnkmr.nokeyboard/.IMEService");
                }
                Toast.makeText(KeyboardService.this, "Orientation changed", Toast.LENGTH_SHORT).show();
                lastOrientation = currentOrientation;
            }
            handler.postDelayed(this, 5000); // Check orientation every 5 seconds
        }
    };

    @Override
    public void onCreate() {
        runStartForeground();
        handler.postDelayed(toastRunnable, 0);
    }

    public void onDestroy() {
        runStopForeground();
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Nullable
    public static Notification.Builder getNotificationBuilder(
            final Context context, final String channelId, final int priority, final CharSequence title,
            final CharSequence notificationText, final CharSequence notificationBigText,
            final PendingIntent contentIntent, final PendingIntent deleteIntent, final int notificationMode) {
        if (context == null) return null;
        Notification.Builder builder = new Notification.Builder(context);
        builder.setContentTitle(title);
        builder.setContentText(notificationText);
        builder.setStyle(new Notification.BigTextStyle().bigText(notificationBigText));
        builder.setContentIntent(contentIntent);
        builder.setDeleteIntent(deleteIntent);

        builder.setPriority(priority);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            builder.setChannelId(channelId);

        // builder = setNotificationDefaults(builder, notificationMode);

        return builder;
    }

    /** Make service run in foreground mode. */
    private void runStartForeground() {
        setupNotificationChannel();
        startForeground(APP_NOTIFICATION_ID, buildNotification());
    }

    /** Make service leave foreground mode. */
    private void runStopForeground() {
        stopForeground(true);
    }

    private Notification buildNotification() {
        String notificationText = "Background keyboard service as active";

        int priority = NotificationManager.IMPORTANCE_HIGH;

        Notification.Builder builder = getNotificationBuilder(this,
                APP_NOTIFICATION_CHANNEL_ID, priority,
                APP_NAME, notificationText, null,
                null, null, NOTIFICATION_MODE_SILENT);

        if (builder == null)  return null;

        // No need to show a timestamp:
        builder.setShowWhen(false);

        // Set notification icon
        builder.setSmallIcon(R.mipmap.ic_launcher);

        // Set background color for small notification icon
        builder.setColor(0xFF607D8B);

        // TermuxSessions are always ongoing
        builder.setOngoing(true);

        // Set Exit button action
        Intent exitIntent = new Intent(this, KeyboardService.class).setAction(ACTION_STOP_SERVICE);
        builder.addAction(R.mipmap.ic_launcher_round, "Exit", PendingIntent.getService(this, 0, exitIntent, PendingIntent.FLAG_IMMUTABLE));

        return builder.build();
    }

    private void setupNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        setupNotificationChannel(this, APP_NOTIFICATION_CHANNEL_ID,
                APP_NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
    }

    public static NotificationManager getNotificationManager(final Context context) {
        if (context == null) return null;
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }
    public static void setupNotificationChannel(final Context context, final String channelId, final CharSequence channelName, final int importance) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);

        NotificationManager notificationManager = getNotificationManager(context);
        if (notificationManager != null)
            notificationManager.createNotificationChannel(channel);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Run again in case service is already started and onCreate() is not called
        runStartForeground();

        String action = null;
        if (intent != null) {
            action = intent.getAction();
        }

        if (action != null) {
            switch (action) {
                case ACTION_STOP_SERVICE:
                    runStopForeground();
                    System.exit(0);
                    break;
                default:
                    break;
            }
        }

        // If this service really do get killed, there is no point restarting it automatically - let the user do on next
        // start of {@link Term):
        return Service.START_NOT_STICKY;
    }
}
