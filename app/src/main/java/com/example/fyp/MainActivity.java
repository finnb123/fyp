package com.example.fyp;

import android.Manifest;
import android.annotation.SuppressLint;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends AppCompatActivity {

    TextView connectionStatus, senderTxt, messageTxt, confidenceTxt, timeTxt, peersTxt;
    Button discoverButton;
    ImageButton sendButton;
    WifiP2pManager manager;
    WifiP2pManager.Channel channel;
    BroadcastReceiver receiver;
    IntentFilter intentFilter;
    List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    String[] deviceNameArray;
    WifiP2pDevice[] deviceArray;
    Socket socket;
    GroupOwnerClass groupOwnerClass;
    ClientClass clientClass;
    boolean isHost;
    InetAddress groupOwnerAddress;

    Double confidence;
    ArrayList<Double> confidenceList = new ArrayList<>();

    // Testing
    double currentTime;
    double tcpTime;
    double groupTime;
    double startTime;


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

        // For Testing
        startTime = System.currentTimeMillis();
        // End Testing

        initialWork();

        buttonListener();

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

    @Override
    protected void onDestroy(){
        super.onDestroy();
        if(groupOwnerClass !=null){
            groupOwnerClass.end();
        }
        if(clientClass!=null){
            clientClass.end();
        }

    }

    private void initialWork() {
        connectionStatus = findViewById(R.id.connection_status);
        senderTxt = findViewById(R.id.senderTxt);
        messageTxt = findViewById(R.id.messageTxt);
        confidenceTxt = findViewById(R.id.confidenceTxt);
        peersTxt = findViewById(R.id.peersTxt);
        timeTxt = findViewById(R.id.timeTxt);
        discoverButton = findViewById(R.id.buttonDiscover);
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

        //Random rng = new Random();
        //confidence = 0.5 +(rng.nextDouble()*0.5);
        confidence = 0.4;
        confidenceList.add(confidence);
    }

    public boolean hasNotificationAccess(Context context){
        return Settings.Secure.getString(
                context.getApplicationContext().getContentResolver(),
                "enabled_notification_listeners"
        ).contains(context.getApplicationContext().getPackageName());
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

                            //testing
                            double newTime = System.currentTimeMillis();
                            groupTime = newTime-startTime;
                            String timeToGF = "Group Formed: " + groupTime;
                            Toast.makeText(MainActivity.this, timeToGF, Toast.LENGTH_LONG ).show();
                            //end testing
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




    private void buttonListener() {
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
                executor.execute(new Runnable() {
                    String finalMsg;
                    @Override
                    public void run() {

                        if(peers.isEmpty()){
                            Log.e("Peers List", "EMPTY");
                        }
                        else if(isHost){
                            if(groupOwnerClass !=null){
                                finalMsg = new MessageHelper("We are now connected... I am the host", confidence, deviceNameArray).getFullMessage();
                                groupOwnerClass.write(finalMsg.getBytes());
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
                isHost = true;
                groupOwnerClass = new GroupOwnerClass();
                groupOwnerClass.start();
            }else if (wifiP2pInfo.groupFormed){

                isHost = false;
                clientClass = new ClientClass(groupOwnerAddress);
                clientClass.start();

            }
        }
    };

    public class GroupOwnerClass extends Thread{
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
        public void end(){
            try {
                if(socket!=null){
                    socket.close();
                    Log.d("GO Class", "Closed socket");
                }
                if(serverSocket!=null){
                    serverSocket.close();
                    Log.d("GO Class", "Closed GO socket");
                }
                Log.d("GO Class", "Ended");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void shooterDetected(){
            Log.e("SHOOTING DETECTED", "SHOOTING DETECTED");
            senderTxt.setText("SHOOTING DETECTED");
            messageTxt.setText("FIND A SAFE LOCATION");
            confidenceTxt.setText("CONTACT EMERGENCY SERVICES");
            peersTxt.setText("CALL 999");
            timeTxt.setText("");
        }


        public boolean consensusDecision(ArrayList<Double> confidenceList){
            int size = confidenceList.size();
            double avg = 0.0;
            for(Double c: confidenceList){
                avg+=c;
            }
            avg = avg/size;

            if(size>1){
                switch(size){
                    case 2: case 3: case 4: case 5:
                        if(avg> 0.9){
                            return true;
                        }
                        break;
                    case 6: case 7: case 8: case 9: case 10:
                        if(avg > 0.85){
                            return true;
                        }
                        break;
                    case 11: case 12: case 13: case 14: case 15:
                        if(avg > 0.8){
                            return true;
                        }
                        break;
                    default:
                        if(avg > 0.7){
                            return true;
                        }
                }
            }
            return false;
        }

        @Override
        public void run(){
            try {
                serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(true);
                serverSocket.bind(new InetSocketAddress(8888));
                socket = serverSocket.accept();
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();

                connectionStatus.setText(R.string.host);

                double newTime = System.currentTimeMillis();
                tcpTime = newTime-startTime;
                String msg = "Group Time: " + groupTime + "\nTCP Time: " + tcpTime;

                String finalMsg = new MessageHelper("We are now connected... I am the host\n" + msg, confidence, deviceNameArray).getFullMessage();
                this.write(finalMsg.getBytes());
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
                                                double newTime = System.currentTimeMillis();
                                                timeTxt.setText(String.format("Time to receive: %s", newTime-startTime));

                                                Double clientConfidence = Double.valueOf(Objects.requireNonNull(messageMap.get("confidence")));
                                                if(!confidenceList.contains(clientConfidence)){
                                                    confidenceList.add(clientConfidence);
                                                    Log.d("Confidence List", "Added value\nNew list is: " + confidenceList.toString());
                                                    if(consensusDecision(confidenceList)){
                                                        shooterDetected();
                                                    }else{
                                                        Log.d("GO Class", "Average under threshold");
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
        public void end(){
            try {
                if(socket!=null){
                    socket.close();
                    Log.d("Client Class", "Closed socket");
                }
                Log.d("Client Class", "Ended");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        @Override
        public void run() {
            try{
                socket.connect(new InetSocketAddress(hostAdd, 8888), 500);
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();

                connectionStatus.setText(R.string.client);

                double newTime = System.currentTimeMillis();
                tcpTime = newTime-startTime;

                String msg = "Group Time: " + groupTime + "\nTCP Time: " + tcpTime;
                String finalMsg = new MessageHelper("We are now connected... I am the Client\n" + msg, confidence, deviceNameArray).getFullMessage();
                this.write(finalMsg.getBytes());
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
                                                double newTime = System.currentTimeMillis();
                                                timeTxt.setText(String.format("Time to Receive: %s",newTime-startTime));


                                            }
                                        });
                                    }
                                }
                            } catch (IOException e) {
                                Log.d("Error", e.toString());
                                end();
                            }
                        }
                    }
                });
        }
    }


}