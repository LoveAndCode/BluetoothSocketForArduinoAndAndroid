package com.example.jjh.bluetoothsocket.service;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by JohnMark on 2017-10-01.
 * Project : BluetoothSocket.
 */

public class Bluetooth {

    // Identifier of this application
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    // Variables for bluetooth
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket socket;
    private BluetoothDevice device, devicePair;
    private BufferedReader input;
    private OutputStream output;

    // Variables for communication
    private boolean connected = false;
    private CommunicationCallback communicationCallback = null;
    private DiscoveryCallback discoveryCallback = null;

    private Activity activity;

    /**
     *  Declare broadcastReceivers
     */

    // The broadcastReceiver related of bluetoothScanning
    private BroadcastReceiver broadcastScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            switch (action) {
                case BluetoothAdapter.ACTION_STATE_CHANGED: {
                    final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    if (state == BluetoothAdapter.STATE_OFF) {
                        if (discoveryCallback != null) {
                            discoveryCallback.onError("Bluetooth turned off");
                        }
                    }
                    break;
                }
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED: {
                    context.unregisterReceiver(broadcastScanReceiver);
                    if (discoveryCallback != null) {
                        discoveryCallback.onFinish();
                    }
                    break;
                }
                case BluetoothDevice.ACTION_FOUND: {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (discoveryCallback != null) {
                        discoveryCallback.onDevice(device);
                    }
                    break;
                }
            }
        }
    };

    // The broadcastReceiver related of bluetooth device pairing
    private BroadcastReceiver broadcastPairReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                final int preState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);

                // If previous state is pairing and current state is paired
                if (state == BluetoothDevice.BOND_BONDED && preState == BluetoothDevice.BOND_BONDING) {
                    context.unregisterReceiver(broadcastPairReceiver);
                    if (discoveryCallback != null)
                        discoveryCallback.onPair(devicePair);
                } else if (state == BluetoothDevice.BOND_NONE && preState == BluetoothDevice.BOND_BONDED) { // If previous state is paired and current state is unpaired
                    context.unregisterReceiver(broadcastPairReceiver);
                    if (discoveryCallback != null)
                        discoveryCallback.onUnpair(devicePair);
                }
            }
        }
    };

    // Constructor by activity parameter
    public Bluetooth(Activity activity) {
        this.activity = activity;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }


    /**
     *  Bluetooth function toggle method
     */
    public void enableBluetooth() {
        if (bluetoothAdapter != null) {
            if (!bluetoothAdapter.isEnabled()) {
                bluetoothAdapter.enable();
            }
        }
    }
    public void disableBluetooth() {
        if (bluetoothAdapter != null) {
            if (bluetoothAdapter.isEnabled()) {
                bluetoothAdapter.disable();
            }
        }
    }


    /**
     * Try to connect by mac address of device
     *
     * @param address - Mac Address
     */
    public void connectToAddress(final String address) {
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
    }

    /**
     * Try to connect by device SSID
     *
     * @param name - device SSID
     */
    public void connectToName(final String name) {
        for (BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
            if (device.getName().equals(name)) {
                connectToAddress(device.getAddress());
                return;
            }
        }
    }

    /**
     * Connect To device
     *
     * @param device - bluetooth device
     */
    public void connectToDevice(BluetoothDevice device) {
        new ConnectThread(device).start();
    }

    public void disconnect() {
        try {
            socket.close();
        } catch (IOException e) {
            if (communicationCallback != null)
                communicationCallback.onError(e.getMessage());
        }
    }

    public boolean isConnected() {
        return connected;
    }


    private class ReceiveThread extends Thread implements Runnable {
        @Override
        public void run() {
            String message;
            try {
                while ((message = input.readLine()) != null) {
                    if (communicationCallback != null) {
                        communicationCallback.onMessage(message);
                    }
                }
            } catch (IOException e) {
                connected = false;
                if (communicationCallback != null)
                    communicationCallback.onDisconnect(device, e.getMessage());
            }
        }
    }

    private class ConnectThread extends Thread {

        public ConnectThread(BluetoothDevice device) {
            Bluetooth.this.device = device;
            try {
                Bluetooth.this.socket = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                if (communicationCallback != null) {
                    communicationCallback.onError(e.getMessage());
                }
            }
        }

        @Override
        public void run() {
            bluetoothAdapter.cancelDiscovery();

            try {
                socket.connect();
                output = socket.getOutputStream();
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                connected = true;
                new ReceiveThread().start();
                if (communicationCallback != null) {
                    communicationCallback.onConnect(device);
                }
            } catch (IOException e) {
                if (communicationCallback != null) {
                    communicationCallback.onConnectError(device, e.getMessage());
                }
                try {
                    socket.close();
                } catch (IOException socketCloseException) {
                    if (communicationCallback != null)
                        communicationCallback.onError(socketCloseException.getMessage());
                }
            }
        }
    }


    /**
     * Get List of Paired Devices
     *
     * @return - List of Paired Devices
     */
    public List<BluetoothDevice> getPairedDevice() {
        List<BluetoothDevice> devices = new ArrayList<>();
        for (BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
            devices.add(device);
        }
        return devices;
    }

    /**
     * Get Bluetooth socket instance
     *
     * @return - socket instance
     */
    public BluetoothSocket getSocket() {
        return this.socket;
    }

    /**
     * Get BluetoothDevice instance
     *
     * @return - bluetooth device instance
     */
    public BluetoothDevice getDevice() {
        return this.device;
    }


    public void scanBluetoothDevice() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        activity.registerReceiver(broadcastScanReceiver, filter);
    }

    /**
     * Try to pairing between this application and bluetooth device
     *
     * @param device - Bluetooth device pairing to this application
     */
    public void pair(BluetoothDevice device) {
        activity.registerReceiver(broadcastPairReceiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
        devicePair = device;

        // Do reflection for pairing to device
        try {
            Method method = device.getClass().getMethod("createBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
        } catch (Exception e) {
            if (discoveryCallback != null)
                discoveryCallback.onError(e.getMessage());
        }
    }

    /**
     * Try to unpairing to paired device
     * @param device - Bluetooth device paired to this application
     */
    public void unPair(BluetoothDevice device) {
        devicePair = device;

        // Do reflection for unpairing to device
        try {
            Method method = device.getClass().getMethod("removeBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
        } catch (Exception e) {
            if (discoveryCallback != null)
                discoveryCallback.onError(e.getMessage());
        }
    }



    public interface CommunicationCallback {
        void onConnect(BluetoothDevice device);

        void onDisconnect(BluetoothDevice device, String message);

        void onMessage(String message);

        void onError(String message);

        void onConnectError(BluetoothDevice device, String message);
    }

    public void setCommunicationCallback(CommunicationCallback communicationCallback) {
        this.communicationCallback = communicationCallback;
    }

    public void removeCommunicationCallback() {
        this.communicationCallback = null;
    }

    public interface DiscoveryCallback {
        void onFinish();

        void onDevice(BluetoothDevice device);

        void onPair(BluetoothDevice device);

        void onUnpair(BluetoothDevice device);

        void onError(String message);
    }

    public void setDiscoveryCallback(DiscoveryCallback discoveryCallback) {
        this.discoveryCallback = discoveryCallback;
    }

    public void removeDiscoveryCallback() {
        this.discoveryCallback = null;
    }
}


