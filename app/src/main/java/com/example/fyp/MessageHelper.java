package com.example.fyp;

import android.os.Build;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;


public class MessageHelper {
    JSONObject fullMessage = new JSONObject();
    private final String sender = Build.DEVICE + " " + Build.ID;
    private final String message;
    private final double confidence;
    private final String time;

    private final String[] peers;

    public MessageHelper(String message,double confidence, String[] peers){
        this.message = message;
        this.confidence = confidence;
        this.peers = peers;

        long currentDateTime = System.currentTimeMillis();
        this.time = Long.toString(currentDateTime);
    }
    public MessageHelper(){
        this.message = "null";
        this.confidence = 0;
        this.peers = null;

        long currentDateTime = System.currentTimeMillis();
        this.time = Long.toString(currentDateTime);
    }

    public String getFullMessage(){
        return String.format(Locale.getDefault(), "sender=%s;message=%s;confidence=%g;peers=%s;time=%s", this.sender, this.message, this.confidence, Arrays.toString(this.peers), this.time);
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
            messageMap.put("confidence", "ERROR");
            messageMap.put("peers", "ERROR");
            messageMap.put("time", "failure");
        }
        return messageMap;
    }


}
