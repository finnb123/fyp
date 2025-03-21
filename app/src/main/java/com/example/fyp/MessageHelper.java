package com.example.fyp;

import android.annotation.SuppressLint;
import android.os.Build;
import android.provider.Settings;

import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;


public class MessageHelper {
    JSONObject fullMessage = new JSONObject();
    private final String sender = Build.DEVICE + " " + Build.ID;
    private final String message;
    private final boolean event;
    private final double confidence;
    private final String time;

    private final String[] peers;

    public MessageHelper(String message, boolean event, double confidence, String[] peers){
        this.message = message;
        this.event = event;
        this.confidence = confidence;
        this.peers = peers;

        long currentDateTime = System.currentTimeMillis();
        this.time = Long.toString(currentDateTime);
//        @SuppressLint("SimpleDateFormat") SimpleDateFormat simpleDateFormat = new SimpleDateFormat("H:mm:ss:SS");
//        this.time = simpleDateFormat.format(currentDateTime);
    }
    public MessageHelper(){
        this.message = "null";
        this.event = false;
        this.confidence = 0;
        this.peers = null;

        long currentDateTime = System.currentTimeMillis();
        this.time = Long.toString(currentDateTime);
    }

    public String getFullMessage(){
        return String.format(Locale.getDefault(), "sender=%s;message=%s;event=%b;confidence=%g;peers=%s;time=%s", this.sender, this.message, this.event, this.confidence, Arrays.toString(this.peers), this.time);
    }

    public HashMap<String, String> getMap(String input){
        HashMap<String, String> messageMap = new HashMap<String, String>();
        if(input != null && !input.isEmpty()){
            String[] keyValuePairs = input.split(";");
            for(String k : keyValuePairs){
                String[] keyValue = k.split("=");
                messageMap.put(keyValue[0], keyValue[1]);
            }
        } else{
            messageMap.put("sender", "system");
            messageMap.put("message", "ERROR");
            messageMap.put("event", "ERROR");
            messageMap.put("confidence", "ERROR");
            messageMap.put("peers", "ERROR");
            messageMap.put("time", "failure");
        }
        return messageMap;
    }


}
