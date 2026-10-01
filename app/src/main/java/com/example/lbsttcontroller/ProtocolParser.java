package com.example.lbsttcontroller;

public class ProtocolParser {
    private static final byte HEADER = (byte) 0xAA;
    private static final byte TYPE_TELEMETRY = (byte) 0x01;
    private static final byte END = (byte) 0x55;
    private static final int PACKET_SIZE = 18;

    public static class TelemetryData {
        public float altitude, speed;
        public double latitude, longitude;
        public int batteryPercent, flightMode;
    }

    public static TelemetryData parse(byte[] data) {
        if (data == null || data.length < PACKET_SIZE) return null;
        for (int i = 0; i <= data.length - PACKET_SIZE; i++) {
            if (data[i] == HEADER && data[i + 1] == TYPE_TELEMETRY
                    && data[i + PACKET_SIZE - 1] == END) {
                byte[] p = new byte[PACKET_SIZE];
                System.arraycopy(data, i, p, 0, PACKET_SIZE);
                if (!verifyChecksum(p)) continue;

                TelemetryData td = new TelemetryData();
                td.altitude = readShort(p, 2) / 100.0f;
                td.speed = readShort(p, 4) / 100.0f;
                td.latitude = readInt(p, 6) / 1e7;
                td.longitude = readInt(p, 10) / 1e7;
                td.batteryPercent = p[14] & 0xFF;
                td.flightMode = p[15] & 0xFF;
                return td;
            }
        }
        return null;
    }

    private static short readShort(byte[] d, int o) {
        return (short) (((d[o] & 0xFF) << 8) | (d[o + 1] & 0xFF));
    }
    private static int readInt(byte[] d, int o) {
        return ((d[o] & 0xFF) << 24) | ((d[o+1] & 0xFF) << 16)
                | ((d[o+2] & 0xFF) << 8) | (d[o+3] & 0xFF);
    }
    private static boolean verifyChecksum(byte[] p) {
        byte cs = 0;
        for (int i = 0; i < PACKET_SIZE - 2; i++) cs ^= p[i];
        return cs == p[PACKET_SIZE - 2];
    }

    public static byte[] buildCommandPacket(int throttle, int roll,
                                            int pitch, int yaw, int mode) {
        byte[] p = new byte[9];
        p[0] = HEADER;
        p[1] = (byte) 0x02;
        p[2] = (byte) (throttle & 0xFF);
        p[3] = (byte) (roll & 0xFF);
        p[4] = (byte) (pitch & 0xFF);
        p[5] = (byte) (yaw & 0xFF);
        p[6] = (byte) mode;
        byte cs = 0;
        for (int i = 0; i < 7; i++) cs ^= p[i];
        p[7] = cs;
        p[8] = END;
        return p;
    }
}