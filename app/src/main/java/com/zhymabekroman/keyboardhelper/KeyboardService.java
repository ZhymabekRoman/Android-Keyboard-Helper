package com.zhymabekroman.keyboardhelper;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;
import androidx.annotation.Nullable;
import java.util.List;

public final class KeyboardService extends Service {
  class LocalBinder extends Binder {
    public final KeyboardService service = KeyboardService.this;
  }

  private final IBinder mBinder = new LocalBinder();
  private Handler handler = new Handler();
  private static final int NOTIFICATION_MODE_SILENT = 1;
  private static final int APP_NOTIFICATION_ID = 286;
  private static final String APP_NAME = "Keyboard Service";
  private static final String APP_NOTIFICATION_CHANNEL_ID = "keyboard_service_channel";
  private static final String APP_NOTIFICATION_CHANNEL_NAME = "Keyboard Service App";
  private static final String ACTION_STOP_SERVICE = "com.zhymabekroman.keyboardhelper.exit_service";

  private int lastOrientation = -1;

  private Runnable toastRunnable =
      new Runnable() {
        @Override
        public void run() {
          int currentOrientation = getResources().getConfiguration().orientation;
          if (currentOrientation != lastOrientation) {
            SharedPreferences sharedPref =
                getSharedPreferences("appSettings", Context.MODE_PRIVATE);
            if (getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_PORTRAIT) {
              String portraitKeyboard = sharedPref.getString("portraitKeyboard", "");
              Log.d("KeyboardService", portraitKeyboard);
              if (!setInputMethod(KeyboardService.this, portraitKeyboard)) {
                Toast.makeText(
                        KeyboardService.this, "Failed to set input method", Toast.LENGTH_SHORT)
                    .show();
              }
            } else {
              String landscapeKeyboard = sharedPref.getString("landscapeKeyboard", "");
              Log.d("KeyboardService", landscapeKeyboard);
              if (!setInputMethod(KeyboardService.this, landscapeKeyboard)) {
                Toast.makeText(
                        KeyboardService.this, "Failed to set input method", Toast.LENGTH_SHORT)
                    .show();
              }
            }
            Toast.makeText(KeyboardService.this, "Orientation changed", Toast.LENGTH_SHORT).show();
            lastOrientation = currentOrientation;
          }
          handler.postDelayed(this, 5000);
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
      final Context context,
      final String channelId,
      final int priority,
      final CharSequence title,
      final CharSequence notificationText,
      final CharSequence notificationBigText,
      final PendingIntent contentIntent,
      final PendingIntent deleteIntent,
      final int notificationMode) {
    if (context == null) return null;
    Notification.Builder builder = new Notification.Builder(context);
    builder.setContentTitle(title);
    builder.setContentText(notificationText);
    builder.setStyle(new Notification.BigTextStyle().bigText(notificationBigText));
    builder.setContentIntent(contentIntent);
    builder.setDeleteIntent(deleteIntent);

    builder.setPriority(priority);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) builder.setChannelId(channelId);

    return builder;
  }

  private void runStartForeground() {
    setupNotificationChannel();
    startForeground(APP_NOTIFICATION_ID, buildNotification());
  }

  private void runStopForeground() {
    stopForeground(true);
  }

  private Notification buildNotification() {
    String notificationText = "Background keyboard service as active";

    int priority = NotificationManager.IMPORTANCE_HIGH;

    Notification.Builder builder =
        getNotificationBuilder(
            this,
            APP_NOTIFICATION_CHANNEL_ID,
            priority,
            APP_NAME,
            notificationText,
            null,
            null,
            null,
            NOTIFICATION_MODE_SILENT);

    if (builder == null) return null;

    builder.setShowWhen(false);

    builder.setSmallIcon(R.mipmap.ic_launcher);

    builder.setColor(0xFF607D8B);

    builder.setOngoing(true);

    Intent exitIntent = new Intent(this, KeyboardService.class).setAction(ACTION_STOP_SERVICE);
    builder.addAction(
        R.mipmap.ic_launcher_round,
        "Exit",
        PendingIntent.getService(this, 0, exitIntent, PendingIntent.FLAG_IMMUTABLE));

    return builder.build();
  }

  private void setupNotificationChannel() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

    setupNotificationChannel(
        this,
        APP_NOTIFICATION_CHANNEL_ID,
        APP_NOTIFICATION_CHANNEL_NAME,
        NotificationManager.IMPORTANCE_HIGH);
  }

  public static NotificationManager getNotificationManager(final Context context) {
    if (context == null) return null;
    return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
  }

  public static void setupNotificationChannel(
      final Context context,
      final String channelId,
      final CharSequence channelName,
      final int importance) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

    NotificationManager notificationManager = getNotificationManager(context);
    if (notificationManager == null) return;

    NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
    channel.enableLights(false);
    channel.enableVibration(false);
    channel.setShowBadge(false);
    notificationManager.createNotificationChannel(channel);
  }

  public static boolean isInputMethodEnabled(final Context context, final String imId) {
    if (context == null || imId == null) return false;

    InputMethodManager imm =
        (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
    if (imm == null) return false;

    List<InputMethodInfo> mInputMethodProperties = imm.getEnabledInputMethodList();

    final int N = mInputMethodProperties.size();
    for (int i = 0; i < N; i++) {
      InputMethodInfo imi = mInputMethodProperties.get(i);
      if (imi.getId().equals(imId)) {
        return true;
      }
    }
    return false;
  }

  public static boolean setInputMethod(final Context context, final String imId) {
    if (context == null || imId == null) return false;

    if (!isInputMethodEnabled(context, imId)) return false;

    Settings.Secure.putString(
        context.getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD, imId);
    return true;
  }
}