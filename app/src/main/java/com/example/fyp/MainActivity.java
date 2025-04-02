package com.example.fyp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;


import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends AppCompatActivity {

    TextView connectionStatus, messageTextView, senderTxt, messageTxt, confidenceTxt, timeTxt, peersTxt;
    Button discoverButton;
    //ListView listView;
    EditText typeMsg;
    ImageButton sendButton;

    WifiP2pManager manager;
    WifiP2pManager.Channel channel;
    BroadcastReceiver receiver;
    IntentFilter intentFilter;
    List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    String[] deviceNameArray;
    WifiP2pDevice[] deviceArray;
    Socket socket;
    ServerClass serverClass;
    ClientClass clientClass;
    boolean isHost;
    InetAddress groupOwnerAddress;

    Double confidence;
    ArrayList<Double> confidenceList = new ArrayList<>();


    private static final String MAIN_CHANNEL_ID = "fyp_main_notification";
    private static final int PERMISSION_REQUEST_CODE = 2;
    final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.NEARBY_WIFI_DEVICES,
            Manifest.permission.POST_NOTIFICATIONS
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        initialWork();

        exqListener();

        discoverPeers();
    }

    @Override
    protected void onResume() {
        super.onResume();
        receiver = new WiFiDirectBroadcastReceiver(manager,channel,this);
        registerReceiver(receiver, intentFilter);
    }

    @Override
    protected void onPause(){
        super.onPause();
        unregisterReceiver(receiver);
    }

    private void initialWork() {
        connectionStatus = findViewById(R.id.connection_status);
        senderTxt = findViewById(R.id.senderTxt);
        messageTxt = findViewById(R.id.messageTxt);
        confidenceTxt = findViewById(R.id.confidenceTxt);
        peersTxt = findViewById(R.id.peersTxt);
        timeTxt = findViewById(R.id.timeTxt);
        discoverButton = findViewById(R.id.buttonDiscover);
        //listView = findViewById(R.id.listView);
        //typeMsg = findViewById(R.id.editTextTypeMsg);
        sendButton = findViewById(R.id.sendButton);
        NotificationListener listener = new NotificationListener();
        requestRuntimePermission();

        if(!hasNotificationAccess(this)){
            openPermissions();
        }
        listener.onListenerConnected();
        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);
        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);

        Random rng = new Random();
        confidence = 0.5 +(rng.nextDouble()*0.5);
        confidenceList.add(confidence);
    }


    private void discoverPeers(){
        if (ActivityCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(MainActivity.this,
               Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
            requestRuntimePermission();
        }
        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d("Discovery", "Discovery Started");
                connectionStatus.setText(R.string.discovery_started);
            }
            @Override
            public void onFailure(int i) {
                Log.d("Discovery", "Discovery Failed: " + i);
                connectionStatus.setText(R.string.discovery_failed);
            }
        });
    }

    private void createGroup(){
        WifiP2pConfig config = new WifiP2pConfig();
        if(deviceArray.length > 0){
            if(deviceArray[0] != null) {
//            for (WifiP2pDevice device : deviceArray){
                List<WifiP2pDevice> deviceList = Arrays.asList(deviceArray);
                WifiP2pDevice device = deviceList.stream()
                        .filter(d -> d.deviceName.startsWith("Android"))
                        .findFirst()
                        .orElse(null);
                //WifiP2pDevice device = deviceArray[0];
                if(device!= null){
                    Log.d("Create Group", device.toString());
                    config.deviceAddress = device.deviceAddress;
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    manager.connect(channel, config, new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                            //Log.d("Peer Group", String.format("Group formed with: %s", device.deviceAddress));
                            //connectionStatus.setText("Group Formed");
                            Log.d("Peer Group", "Create Group Success");
                        }

                        @Override
                        public void onFailure(int i) {
                            //Log.d("Peer Group", String.format("Error with: %s\nError is: %d", device.deviceAddress, i));
                            Log.d("Peer Group", "Issue connecting: " + i);
                        }
                    });
                }
            }
        }
        //}

    }


    private void exqListener() {
        discoverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                discoverPeers();
            }
        });


        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                Random rng = new Random();
                double confidence = rng.nextDouble();
                executor.execute(new Runnable() {
                    String finalMsg;
                    @Override
                    public void run() {

                        if(peers.isEmpty()){
                            messageTextView.setText(R.string.peer_list_empty);
                        }
                        else if(isHost){
                            if(serverClass!=null){
                                finalMsg = new MessageHelper("We are now connected... I am the host", confidence, deviceNameArray).getFullMessage();
                                serverClass.write(finalMsg.getBytes());
                            }
                        }else{
                            if(clientClass!=null){
                                finalMsg = new MessageHelper("We are now connected... I am a client", confidence, deviceNameArray).getFullMessage();
                                clientClass.write(finalMsg.getBytes());
                            }
                        }
                    }
                });
            }
        });

    }

    public void openPermissions(){
        try{
            Intent settingsIntent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
            startActivity(settingsIntent);
        } catch (ActivityNotFoundException e){
            Log.d("error", e.toString());
        }
    }
    private void requestRuntimePermission(){
        int i = 0;
        ArrayList<String> perms = new ArrayList<String>();
        for(String perm: PERMISSIONS){
            if(ActivityCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED){
                perms.add(perm);
            }
        }

        String[] not_allowed = new String[perms.size()];
        perms.toArray(not_allowed);
        if(not_allowed.length >= 1){
            Log.d("Permissions", Arrays.toString(not_allowed));
            ActivityCompat.requestPermissions(this, not_allowed, PERMISSION_REQUEST_CODE );
        }
    }


    WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {
            List<WifiP2pDevice> refreshedPeers = new ArrayList<>(peerList.getDeviceList());
            if(!refreshedPeers.equals(peers)){
                peers.clear();
                peers.addAll(refreshedPeers);
                deviceNameArray = new String[peerList.getDeviceList().size()];
                deviceArray = new WifiP2pDevice[peerList.getDeviceList().size()];

                int index = 0;
                for(WifiP2pDevice device: peerList.getDeviceList()){
                    deviceNameArray[index] = device.deviceName;
                    deviceArray[index] = device;
                    index++;
                }
                Log.d("Peer List Listener", Arrays.toString(deviceArray));
                Log.d("Peer List Listener", Arrays.toString(deviceNameArray ));
                createGroup();
            }




        }
    };

    WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
            Log.d("On Connection Info", "Firing");
            groupOwnerAddress = wifiP2pInfo.groupOwnerAddress;
            if(wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner){
                connectionStatus.setText(R.string.host);
                isHost = true;
                serverClass = new ServerClass();
                serverClass.start();
            }else if (wifiP2pInfo.groupFormed){
                connectionStatus.setText(R.string.client);
                isHost = false;
                clientClass = new ClientClass(groupOwnerAddress);
                clientClass.start();
            }
        }
    };

    public class ServerClass extends Thread{
        ServerSocket serverSocket;
        private InputStream inputStream;
        private OutputStream outputStream;

        public void write(byte[] bytes){
            try {
                if(outputStream != null){
                    outputStream.write(bytes);
                }else{
                    Log.e("Output Stream", "Output Stream is Null");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
//        public void end(){
//            try {
//                socket.close();
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        }

        @Override
        public void run(){
            try {
                serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(true);
                serverSocket.bind(new InetSocketAddress(8888));

                socket = serverSocket.accept();
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());

                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        byte[] buffer = new byte[1024];
                        int bytes;

                        while (socket != null) {
                            try {
                                if(inputStream!=null){
                                    bytes = inputStream.read(buffer);
                                    if (bytes > 0) {
                                        int finalBytes = bytes;
                                        long currentTime = System.currentTimeMillis();
                                        handler.post(new Runnable() {
                                            @SuppressLint("SetTextI18n")
                                            @Override
                                            public void run() {
                                                String tempMSG = new String(buffer, 0, finalBytes);
                                                Log.e("TEMP MESSAGE", tempMSG);
                                                MessageHelper helper = new MessageHelper();
                                                HashMap<String, String> messageMap = helper.getMap(tempMSG);
                                                senderTxt.setText(String.format("Sender: %s",messageMap.get("sender")));
                                                messageTxt.setText(String.format("Message: %s", messageMap.get("message")));
                                                confidenceTxt.setText(String.format("Confidence: %s", messageMap.get("confidence")));
                                                peersTxt.setText(String.format("Peers: %s", messageMap.get("peers")));
                                                timeTxt.setText(String.format("Time Sent: %s",messageMap.get("time")));

                                                Double clientConfidence = Double.valueOf(Objects.requireNonNull(messageMap.get("confidence")));
                                                if(!confidenceList.contains(clientConfidence)){
                                                    confidenceList.add(clientConfidence);
                                                    Log.d("Confidence List", "Added value\nNew list is: " + confidenceList.toString());
                                                    if(confidenceList.size()>1){
                                                        if(confAvg(confidenceList) > 0.4){
                                                            Log.e("SHOOTING DETECTED", "SHOOTING DETECTED");
                                                            senderTxt.setText("SHOOTING DETECTED");
                                                            messageTxt.setText("FIND A SAFE LOCATION");
                                                            confidenceTxt.setText("CONTACT EMERGENCY SERVICES");
                                                            peersTxt.setText("CALL 999");
                                                            timeTxt.setText("");
                                                        }
                                                    }
                                                }else{
                                                    Log.d("Confidence List", "Value already added");
                                                }
                                            }
                                        });

                                    }
                                }
                            } catch (IOException e) {
//                                throw new RuntimeException(e);
                                Log.e("error", e.toString());
                            }
                        }

                    }
                });

        }
    }

    public double confAvg(ArrayList<Double> confidenceList){
        Double i = 0.0, total = 0.0;
        for(Double c: confidenceList){
            total += c;
            i += 1;
        }
        return total/i;
    }

    public class ClientClass extends Thread{
        String hostAdd;
        private InputStream inputStream;
        private OutputStream outputStream;

        public ClientClass(InetAddress hostAddress){
            hostAdd = hostAddress.getHostAddress();
            socket = new Socket();
        }

        public void write(byte[] bytes){
            try {
                if(outputStream != null){
                    outputStream.write(bytes);
                }else{
                    Log.e("Output Stream", "Output Stream is Null");
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
//        public void end(){
//            try {
//                socket.close();
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        }
        @Override
        public void run() {
            try{
                socket.connect(new InetSocketAddress(hostAdd, 8888), 500);
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
            } catch (IOException e){
                Log.e("Client", e.toString());
            }

            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());


                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        byte[] buffer = new byte[1024];
                        int bytes;

                        while (socket != null) {
                            try {
                                if(inputStream!=null){
                                    bytes = inputStream.read(buffer);
                                    if (bytes > 0) {
                                        int finalBytes = bytes;
                                        long currentTime = System.currentTimeMillis();
                                        handler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                String tempMSG = new String(buffer, 0, finalBytes);
                                                Log.e("TEMP MESSAGE", tempMSG);
                                                MessageHelper helper = new MessageHelper();
                                                HashMap<String, String> messageMap = helper.getMap(tempMSG);
                                                senderTxt.setText(String.format("Sender: %s",messageMap.get("sender")));
                                                messageTxt.setText(String.format("Message: %s", messageMap.get("message")));
                                                confidenceTxt.setText(String.format("Confidence: %s", messageMap.get("confidence")));
                                                peersTxt.setText(String.format("Peers: %s", messageMap.get("peers")));
                                                timeTxt.setText(String.format("Time Sent: %s",messageMap.get("time")));


                                                String difference = Long.toString(currentTime - Long.parseLong(Objects.requireNonNull(messageMap.get("time"))));
                                                Log.e("Time From Sent to Received", difference);
                                            }
                                        });
                                    }
                                }
                            } catch (IOException e) {
                                Log.d("Error", e.toString());
                            }
                        }
                    }
                });




        }
    }

    public boolean hasNotificationAccess(Context context){
        return Settings.Secure.getString(
                context.getApplicationContext().getContentResolver(),
                "enabled_notification_listeners"
        ).contains(context.getApplicationContext().getPackageName());
    }




}