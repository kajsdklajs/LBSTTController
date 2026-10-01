package com.example.lbsttcontroller;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Looper;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothManager {
    private static final UUID SPP_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private static final String DEVICE_NAME = "HC-05"; // имя Bluetooth-модуля БПЛА

    private BluetoothAdapter adapter;
    private BluetoothSocket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private Thread receiveThread;
    private volatile boolean isConnected = false;

    private OnDataReceivedListener dataListener;
    private OnConnectionListener connectionListener;

    public interface OnDataReceivedListener {
        void onDataReceived(byte[] data);
    }
    public interface OnConnectionListener {
        void onConnected(String deviceName);
        void onDisconnected();
        void onError(String message);
    }

    public void setDataListener(OnDataReceivedListener l) { dataListener = l; }
    public void setConnectionListener(OnConnectionListener l) { connectionListener = l; }

    @SuppressLint("MissingPermission")
    public boolean connect() {
        adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null || !adapter.isEnabled()) {
            notifyError("Bluetooth не доступен");
            return false;
        }

        BluetoothDevice device = findDevice();
        if (device == null) {
            notifyError("БПЛА не найден. Сопрягите HC-05 в настройках.");
            return false;
        }

        try {
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID);
            adapter.cancelDiscovery();
            socket.connect();
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
            isConnected = true;
            startReceiveThread();
            notifyConnected(device.getName());
            return true;
        } catch (IOException e) {
            notifyError("Ошибка подключения: " + e.getMessage());
            return false;
        }
    }

    @SuppressLint("MissingPermission")
    private BluetoothDevice findDevice() {
        for (BluetoothDevice d : adapter.getBondedDevices()) {
            if (DEVICE_NAME.equals(d.getName())) return d;
        }
        return null;
    }

    private void startReceiveThread() {
        receiveThread = new Thread(() -> {
            byte[] buffer = new byte[1024];
            while (isConnected) {
                try {
                    int bytes = inputStream.read(buffer);
                    if (bytes > 0) {
                        byte[] data = new byte[bytes];
                        System.arraycopy(buffer, 0, data, 0, bytes);
                        notifyDataReceived(data);
                    }
                } catch (IOException e) {
                    if (isConnected) {
                        notifyError("Ошибка приёма: " + e.getMessage());
                        disconnect();
                    }
                    break;
                }
            }
        });
        receiveThread.start();
    }

    public void sendCommand(byte[] command) {
        if (!isConnected || outputStream == null) return;
        try {
            outputStream.write(command);
            outputStream.flush();
        } catch (IOException e) {
            notifyError("Ошибка отправки: " + e.getMessage());
        }
    }

    public boolean isConnected() { return isConnected; }

    public void disconnect() {
        isConnected = false;
        if (receiveThread != null) receiveThread.interrupt();
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        notifyDisconnected();
    }

    private void notifyDataReceived(byte[] data) {
        if (dataListener != null)
            new Handler(Looper.getMainLooper()).post(() -> dataListener.onDataReceived(data));
    }
    private void notifyConnected(String name) {
        if (connectionListener != null)
            new Handler(Looper.getMainLooper()).post(() -> connectionListener.onConnected(name));
    }
    private void notifyDisconnected() {
        if (connectionListener != null)
            new Handler(Looper.getMainLooper()).post(() -> connectionListener.onDisconnected());
    }
    private void notifyError(String msg) {
        if (connectionListener != null)
            new Handler(Looper.getMainLooper()).post(() -> connectionListener.onError(msg));
    }
}