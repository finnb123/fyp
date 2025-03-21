package com.example.fyp;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class StartOnBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent){
        if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())){
            Intent activityIntent = new Intent(context, MainActivity.class);
            activityIntent.addFlags(FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(activityIntent);
        }
    }
}
