package com.example.fyp;

import android.annotation.SuppressLint;
import android.os.Build;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.HashMap;


public class MessageHelper {
    JSONObject fullMessage = new JSONObject();
    private final String sender = Build.ID;
    private final String message;


    private final String time;
    public MessageHelper(String message){
        this.message = message;

        long currentDateTime = System.currentTimeMillis();
        @SuppressLint("SimpleDateFormat") SimpleDateFormat simpleDateFormat = new SimpleDateFormat("H:mm:ss:SS");
        this.time = simpleDateFormat.format(currentDateTime);
    }
    public MessageHelper(){
        this.message = "null";
        long currentDateTime = System.currentTimeMillis();
        @SuppressLint("SimpleDateFormat") SimpleDateFormat simpleDateFormat = new SimpleDateFormat("H:mm:ss:SS");
        this.time = simpleDateFormat.format(currentDateTime);
    }

    public String getFullMessage(){
        return String.format("sender=%s message=%s time=%s", this.sender, this.message, this.time);
    }

    public HashMap<String, String> getMap(String input){
        //"sender:AP2A.240905.003 msg:Message time:3:44:31:29"
        HashMap<String, String> messageMap = new HashMap<String, String>();
        if(input != null && !input.isEmpty()){
            String[] keyValuePairs = input.split(" ");
            for(String k : keyValuePairs){
                String[] keyValue = k.split("=");
                messageMap.put(keyValue[0], keyValue[1]);
            }
        } else{
            messageMap.put("sender", "system");
            messageMap.put("message", "ERROR");
            messageMap.put("time", "failure");
        }
        return messageMap;
    }


}
