package com.example.fyp;

import static android.content.Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.List;
import java.util.Objects;

public class NotificationListener extends NotificationListenerService {

    private static final String MAIN_CHANNEL_ID = "fyp_main_notification";
    Context context;
    @Override
    public void onCreate(){
        super.onCreate();
        createNotificationChannel();
        context = getApplicationContext();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn){
        if(sbn != null){
            final String packageName = sbn.getPackageName();
            final Notification noti = sbn.getNotification();
            if(noti.actions != null) {
                final int numActions = noti.actions.length;
                if (Objects.equals(packageName, "com.google.audio.hearing.visualization.accessibility.scribe") && numActions > 0) {
                    Log.d("Notification", "Notification Received");
//                    Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.example.fyp");
                    try{
                        Intent fullScreenIntent = new Intent(context, MainActivity.class);
                        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(context, 0, fullScreenIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT );
//                        Intent openAppIntent = new Intent(context, MainActivity.class);
//                        openAppIntent.setFlags(FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_BROUGHT_TO_FRONT);
//                        PendingIntent pendingOpenAppIntent = PendingIntent.getActivity(this,0, openAppIntent, PendingIntent.FLAG_IMMUTABLE);

                        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, MAIN_CHANNEL_ID)
                                .setSmallIcon(R.mipmap.ic_launcher_round)
                                .setContentTitle("Shooting Detected")
                                .setContentText("Click to start network discovery")
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
//                                .setContentIntent(pendingOpenAppIntent)
                                .setAutoCancel(true)
                                .setCategory(NotificationCompat.CATEGORY_EVENT)
                                .setFullScreenIntent(fullScreenPendingIntent, true);

                        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
                        int NOTIFICATION_ID = 1;
                        notificationManager.notify(NOTIFICATION_ID, builder.build());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }

//                    try {
//
//
////                        final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
////                        List<ActivityManager.RunningAppProcessInfo> procInfos = activityManager.getRunningAppProcesses();
////                        if (procInfos != null) {
////                            for (final ActivityManager.RunningAppProcessInfo processInfo : procInfos) {
////                                if (processInfo.processName.equals("com.example.fyp")) {
////                                    launchIntent.setFlags(FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
////                                    Log.d("FYP", "Process already running");
////                                }
////                            }
////                        }
////                        launchIntent.setFlags(FLAG_ACTIVITY_NEW_TASK);
////                        startActivity(launchIntent);
////                        Log.d("FYP", "Intent Launched");
//                    } catch (Error ignored) {
//                        Log.e("FYP", "Failed Try Catch");
//                    }

                }
            }


//            Bundle extras = sbn.getNotification().extras;
//
//            for (String key : extras.keySet()) {
//                Log.e("Notification", key + " : " + (extras.get(key) != null ? extras.get(key) : "NULL"));
//            }
//            Log.d("notification", String.valueOf(extras));
        }

    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        Log.d("App", "Service Started");
        return START_STICKY;
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is not in the Support Library.
        CharSequence name = getString(R.string.channel_name);
        String description = getString(R.string.channel_description);
        int importance = NotificationManager.IMPORTANCE_HIGH;
        NotificationChannel channel = new NotificationChannel(MAIN_CHANNEL_ID, name, importance);
        channel.setDescription(description);
        // Register the channel with the system. You can't change the importance
        // or other notification behaviors after this.
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }
}

