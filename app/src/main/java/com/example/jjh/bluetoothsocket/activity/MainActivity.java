package com.example.jjh.bluetoothsocket.activity;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jjh.bluetoothsocket.R;

import java.util.ArrayList;
import java.util.List;

import com.example.jjh.bluetoothsocket.util.Bluetooth;

public class MainActivity extends Activity {

    // UI component object
    private Bluetooth bluetooth;
    private Button scanButton;
    private TextView result;
    private ArrayAdapter<String> listAdapter;
    private ArrayList<String> deviceList;
    private ListView scanList;

    // Bluetooth object
    private Bluetooth.CommunicationCallback communicationCallback;
    private Bluetooth.DiscoveryCallback discoveryCallback;
    private List<BluetoothDevice> bluetoothDevices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI objects
        bluetooth = new Bluetooth(this);
        scanButton = (Button) findViewById(R.id.scanBtn);
        scanList = (ListView) findViewById(R.id.scanList);
        result = (TextView) findViewById(R.id.resultText);
        deviceList = new ArrayList<>();


        // Create callback function
        communicationCallback = new Bluetooth.CommunicationCallback() {
            @Override
            public void onConnect(BluetoothDevice device) {
                String deviceName = device.getName();
                Toast.makeText(MainActivity.this, deviceName + " 와 연결되었습니다.", Toast.LENGTH_SHORT).show();
                result.setText(deviceName);
            }

            @Override
            public void onDisconnect(BluetoothDevice device, String message) {
                String deviceName = device.getName();
                Toast.makeText(MainActivity.this, deviceName + " 와 연결 해제되었습니다.", Toast.LENGTH_SHORT).show();
                result.setText("NONE");
            }

            @Override
            public void onMessage(String message) {
                Toast.makeText(MainActivity.this, "알림 메세지가 왔습니다..", Toast.LENGTH_SHORT).show();
                result.setText("Notice Message: " + message + "\n");
            }

            @Override
            public void onError(String message) {
                Toast.makeText(MainActivity.this, "에러가 발생했습니다.", Toast.LENGTH_SHORT).show();
                result.setText("Error Message: " + message + "\n");
            }

            @Override
            public void onConnectError(BluetoothDevice device, String message) {
                String deviceName = device.getName();
                Toast.makeText(MainActivity.this, deviceName + " 와 연결 중 에러가 발생했습니다.", Toast.LENGTH_SHORT).show();
                result.setText("Error Message: " + message + "\n");
            }
        };
        discoveryCallback = new Bluetooth.DiscoveryCallback() {
            @Override
            public void onFinish() {
                Toast.makeText(MainActivity.this, "기기 탐색이 완료되었습니다.", Toast.LENGTH_SHORT).show();
                result.setText("기기 탐색이 완료되었습니다.");
            }

            @Override
            public void onDevice(BluetoothDevice device) {
                String deviceName = device.getName();
                Toast.makeText(MainActivity.this, deviceName + "을 찾았습니다.", Toast.LENGTH_SHORT).show();
                result.setText(deviceName + " is found!");
            }

            @Override
            public void onPair(BluetoothDevice device) {
                String deviceName = device.getName();
                Toast.makeText(MainActivity.this, deviceName + " 와 통신이 연결되었습니다.", Toast.LENGTH_SHORT).show();
                result.setText(deviceName + " Start Communication");
            }

            @Override
            public void onUnpair(BluetoothDevice device) {
                String deviceName = device.getName();
                Toast.makeText(MainActivity.this, deviceName + " 와 통신이 종료되었습니다.", Toast.LENGTH_SHORT).show();
                result.setText(deviceName + " Close Communication");
            }

            @Override
            public void onError(String message) {
                Toast.makeText(MainActivity.this, "에러발생", Toast.LENGTH_SHORT).show();
                result.setText("Pairing Error: " + message);
            }
        };
        // Set callback function
        bluetooth.setCommunicationCallback(communicationCallback);
        bluetooth.setDiscoveryCallback(discoveryCallback);

        // Enable bluetooth
        bluetooth.enableBluetooth();

        // Scanning
        pairedList();
        listAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, deviceList);
        scanList.setAdapter(listAdapter);
        scanList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String deviceName = deviceList.get(i);
                bluetooth.connectToName(deviceName);
            }
        });

        // Set ScanButton onClickEvent
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pairedList();
            }
        });
    }

    /**
     *  Scan neared bluetooth device
     */
    private void pairedList() {
        bluetooth.scanDevices();
        bluetoothDevices = bluetooth.getPairedDevices();
        for (BluetoothDevice device : bluetoothDevices) {
            deviceList.add(device.getName());
            result.setText(result.getText() + device.getName() + "\n");
        }
    }
}
