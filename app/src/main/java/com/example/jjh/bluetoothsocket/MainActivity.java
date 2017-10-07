package com.example.jjh.bluetoothsocket;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import java.util.Set;

public class MainActivity extends Activity {

    private BluetoothAdapter bluetoothAdapter;
    private Set<BluetoothDevice> pairedDevice;
    private ArrayAdapter<String> arrayAdapter;
    private IntentFilter filter;
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery found device
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                // Get the BluetoothDevice object from the intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adpater to show in a list view
                arrayAdapter.add(device.getName()+"\n"+device.getAddress());
            }
        }
    };

    private static int REQUEST_ENABLE_BT = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        pairedDevice = bluetoothAdapter.getBondedDevices();

        // Register the BroadcastReceiver
        registerReceiver(broadcastReceiver,filter);

        // Check bluetooth support
        if((bluetoothAdapter  = BluetoothAdapter.getDefaultAdapter())== null){
            // Device dose not support Bluetooth
            Toast.makeText(this, "해당 기기는 블루투스 기능을 지원하지 않습니다.", Toast.LENGTH_SHORT).show();
            finish();
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(getBaseContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},1);
        }

        // Check bluetooth status of this device
        if(!bluetoothAdapter.isEnabled()){
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent,REQUEST_ENABLE_BT);
        }

        // If there are paired devices
        if(pairedDevice.size() >0){
            // Loop through paired devices
            for(BluetoothDevice device : pairedDevice){
                arrayAdapter.add(device.getName()+"\n"+device.getAddress());
            }
        }
    }
}
