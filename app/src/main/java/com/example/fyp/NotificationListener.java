package com.example.fyp;

import static android.content.Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP;

import android.app.ActivityManager;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.widget.Toast;

import java.util.List;
import java.util.Objects;

public class NotificationListener extends NotificationListenerService {

    Context context;
    @Override
    public void onCreate(){
        super.onCreate();
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
                    Intent launchIntent = new Intent(context, MainActivity.class);
                    if (launchIntent != null) {
                        try{
                            final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                            List<ActivityManager.RunningAppProcessInfo> procInfos = activityManager.getRunningAppProcesses();
                            if (procInfos != null)
                            {
                                for (final ActivityManager.RunningAppProcessInfo processInfo : procInfos) {
                                    if (processInfo.processName.equals("com.example.fyp")) {
                                        launchIntent.setFlags(FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                                        Log.d("FYP", "Process already running");
                                    }
                                }
                            }
                            launchIntent.setFlags(FLAG_ACTIVITY_NEW_TASK);
                            startActivity(launchIntent);
                            Log.d("FYP", "Intent Launched");
                        } catch (Error ignored){
                            Log.e("FYP", "Failed Try Catch");
                        }

                    } else {
                        Log.e("FYP", "Launch Intent Null");
                    }
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


}

