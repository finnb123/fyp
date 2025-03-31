package com.example.fyp;

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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends AppCompatActivity {

    TextView connectionStatus, messageTextView, senderTxt, messageTxt, confidenceTxt, timeTxt, peersTxt;
    TextView eventTxt;
    Button aSwitch, discoverButton;
    ListView listView;
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

    private static final String MAIN_CHANNEL_ID = "fyp_main_notification";

    private static final String PERMISSION_COARSE_LOCATION = android.Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final String PERMISSION_FINE_LOCATION = android.Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String PERMISSION_NEARBY_DEVICES = android.Manifest.permission.NEARBY_WIFI_DEVICES;
    private static final int PERMISSION_REQUEST_CODE = 2;

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
        listView = findViewById(R.id.listView);
        typeMsg = findViewById(R.id.editTextTypeMsg);
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
    }


    private void discoverPeers(){
        if (ActivityCompat.checkSelfPermission(MainActivity.this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(MainActivity.this,
                android.Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
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
        for (WifiP2pDevice device : deviceArray){
            config.deviceAddress = device.deviceAddress;
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, android.Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            manager.connect(channel, config, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d("Peer Group", String.format("Connected to: %s", device.deviceAddress));
                }
                @Override
                public void onFailure(int i) {
                    Log.d("Peer Group", String.format("Error with: %s\nError is: %d", device.deviceAddress, i));
                }
            });
        }
    }


    private void exqListener() {
//        aSwitch.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
//                startActivityForResult(intent, 1);
//            }
//        });

        discoverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                discoverPeers();
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                final WifiP2pDevice device = deviceArray[i];
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;
                manager.connect(channel, config, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        connectionStatus.setText(String.format("Connected: %s", device.deviceAddress));
                        long currentDateTime = System.currentTimeMillis();
                        @SuppressLint("SimpleDateFormat") SimpleDateFormat simpleDateFormat = new SimpleDateFormat("H:mm:ss:SS");
                        String currentTime = simpleDateFormat.format(currentDateTime);
                        Toast.makeText(MainActivity.this, currentTime, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int i) {
                        connectionStatus.setText(R.string.not_connected);
                        Log.e("List View", String.valueOf(i));
                    }
                });
            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                String finalMsg;
                finalMsg = new MessageHelper(typeMsg.getText().toString(), 1.00, deviceNameArray).getFullMessage();
                Log.d("JSON STRING MESSAGE", finalMsg);

                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        if(peers.isEmpty()){
                            messageTextView.setText(R.string.peer_list_empty);
                        }
                        else if(isHost){
                            serverClass.write(finalMsg.getBytes());
                        }else{
                            clientClass.write(finalMsg.getBytes());
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
        if (ActivityCompat.checkSelfPermission(this, PERMISSION_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ){
            //Toast.makeText(this,"Coarse permission granted", Toast.LENGTH_LONG).show();
            Log.d("PERMS", "Coarse Granted");
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, PERMISSION_COARSE_LOCATION)){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("This app requires Coarse Location permission")
                    .setTitle("Permission Required")
                    .setCancelable(false)
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[] {PERMISSION_COARSE_LOCATION}, PERMISSION_REQUEST_CODE);
                        }
                    })
                    .setNegativeButton("Cancel", ((dialog, which)-> dialog.dismiss()));

            builder.show();
        } else {
            ActivityCompat.requestPermissions(this, new String[] {PERMISSION_COARSE_LOCATION, //PERMISSION_NEARBY_DEVICES
                     }, PERMISSION_REQUEST_CODE);
        }


        if (ActivityCompat.checkSelfPermission(this, PERMISSION_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ){
            //Toast.makeText(this,"Fine permission granted", Toast.LENGTH_LONG).show();
            Log.d("PERMS", "Fine Granted");
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, PERMISSION_FINE_LOCATION)){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("This app requires Fine Location permission")
                    .setTitle("Permission Required")
                    .setCancelable(false)
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[] {PERMISSION_FINE_LOCATION}, PERMISSION_REQUEST_CODE+1);
                        }
                    })
                    .setNegativeButton("Cancel", ((dialog, which)-> dialog.dismiss()));

            builder.show();
        } else {
            ActivityCompat.requestPermissions(this, new String[] {PERMISSION_FINE_LOCATION}, PERMISSION_REQUEST_CODE+1);
        }


        if (ActivityCompat.checkSelfPermission(this, PERMISSION_NEARBY_DEVICES) == PackageManager.PERMISSION_GRANTED ){
            //Toast.makeText(this,"Nearby Devices permission granted", Toast.LENGTH_LONG).show();
            Log.d("PERMS", "Nearby Devices Granted");
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, PERMISSION_NEARBY_DEVICES)){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("This app requires Nearby Devices permission")
                    .setTitle("Permission Required")
                    .setCancelable(false)
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[] {PERMISSION_NEARBY_DEVICES}, PERMISSION_REQUEST_CODE+2);
                        }
                    })
                    .setNegativeButton("Cancel", ((dialog, which)-> dialog.dismiss()));

            builder.show();
        } else {
            ActivityCompat.requestPermissions(this, new String[] {PERMISSION_NEARBY_DEVICES}, PERMISSION_REQUEST_CODE+5);
        }
    }


    WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {
//            if(!wifiP2pDeviceList.equals(peers)){
//                peers.clear();
//                peers.addAll(wifiP2pDeviceList.getDeviceList());
//
//                deviceNameArray = new String[wifiP2pDeviceList.getDeviceList().size()];
//                deviceArray = new WifiP2pDevice[wifiP2pDeviceList.getDeviceList().size()];
//
//                int index = 0;
//                for(WifiP2pDevice device: wifiP2pDeviceList.getDeviceList()){
//                    deviceNameArray[index] = device.deviceName;
//                    deviceArray[index] = device;
//                    index++;
//                }
//
//                ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, deviceNameArray);
//                listView.setAdapter(adapter);
//
//                if(peers.isEmpty()){
//                    connectionStatus.setText(R.string.peer_list_empty);
//                }
//            }
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

                ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, deviceNameArray);
                listView.setAdapter(adapter);
            }
            if(peers.isEmpty()){
                Log.d("Peer List", "No devices found");
                connectionStatus.setText(R.string.peer_list_empty);
            }
        }
    };

    WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
            final InetAddress groupOwnerAddress = wifiP2pInfo.groupOwnerAddress;
            if(wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner){
                connectionStatus.setText(R.string.host);
                isHost = true;
                serverClass = new ServerClass();
                serverClass.start();
//                String finalMsg = new MessageHelper("We are now connected....", true, 1.00, deviceNameArray).getFullMessage();
//                serverClass.write(finalMsg.getBytes());
            }else if (wifiP2pInfo.groupFormed){
                connectionStatus.setText(R.string.client);
                isHost = false;
                clientClass = new ClientClass(groupOwnerAddress);
                clientClass.start();
//                String finalMsg = new MessageHelper("We are now connected....", true, 1.00, deviceNameArray).getFullMessage();
//                clientClass.write(finalMsg.getBytes());
            }
        }
    };




//    @Override
//    protected void onDestroy(){
//        super.onDestroy();
//        try{
//            if(serverClass != null && (serverClass.serverSocket != null)){
//                serverClass.end();
//            }
//            if(clientClass != null && socket != null){
//                clientClass.end();
//            }
//
//
//        } catch (Error e) {
//            Log.e("Socket", e.toString());
//        }
//    }

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
                                                senderTxt.setText(messageMap.get("sender"));
                                                messageTxt.setText(messageMap.get("message"));
                                                confidenceTxt.setText(messageMap.get("confidence"));
                                                peersTxt.setText(messageMap.get("peers"));
                                                timeTxt.setText(messageMap.get("time"));

                                                String difference = Long.toString(currentTime - Long.parseLong(Objects.requireNonNull(messageMap.get("time"))));
                                                Log.e("Time From Sent to Received", difference);
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
                                                senderTxt.setText(messageMap.get("sender"));
                                                messageTxt.setText(messageMap.get("message"));
                                                confidenceTxt.setText(messageMap.get("confidence"));
                                                peersTxt.setText(messageMap.get("peers"));
                                                timeTxt.setText(messageMap.get("time"));

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