package com.example.lbsttcontroller;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class BroadcastTransmitter {
    private static final int BROADCAST_PORT = 8888;
    private DatagramSocket socket;
    private InetAddress broadcastAddress;
    private volatile boolean isRunning = false;

    public void start() {
        try {
            socket = new DatagramSocket();
            socket.setBroadcast(true);
            broadcastAddress = InetAddress.getByName("255.255.255.255");
            isRunning = true;
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void broadcastTelemetry(ProtocolParser.TelemetryData t) {
        if (!isRunning || socket == null) return;
        String msg = String.format(
                "LBS-TT|ALT:%.1f|SPD:%.1f|LAT:%.6f|LON:%.6f|BAT:%d|MODE:%d",
                t.altitude, t.speed, t.latitude, t.longitude,
                t.batteryPercent, t.flightMode);
        byte[] data = msg.getBytes();
        try {
            socket.send(new DatagramPacket(data, data.length,
                    broadcastAddress, BROADCAST_PORT));
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void stop() {
        isRunning = false;
        if (socket != null && !socket.isClosed()) socket.close();
    }
}