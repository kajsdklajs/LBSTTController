package com.example.lbsttcontroller;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class MainActivity extends AppCompatActivity {

    private BluetoothManager bt;
    private BroadcastTransmitter broadcaster;
    private TextView tvStatus, tvTelemetry, tvBroadcast;
    private Button btnConnect;
    private JoystickView joystickLeft, joystickRight;

    private int throttle = 0, yaw = 0, pitch = 0, roll = 0, flightMode = 0;
    private long lastSendTime = 0;
    private static final long SEND_INTERVAL_MS = 50; // 20 Гц

    private final Handler sendHandler = new Handler(Looper.getMainLooper());
    private final Runnable sendRunnable = new Runnable() {
        @Override
        public void run() {
            if (bt != null && bt.isConnected()) {
                byte[] cmd = ProtocolParser.buildCommandPacket(
                        throttle, roll, pitch, yaw, flightMode);
                bt.sendCommand(cmd);
            }
            sendHandler.postDelayed(this, SEND_INTERVAL_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tvStatus);
        tvTelemetry = findViewById(R.id.tvTelemetry);
        tvBroadcast = findViewById(R.id.tvBroadcast);
        btnConnect = findViewById(R.id.btnConnect);
        joystickLeft = findViewById(R.id.joystickLeft);
        joystickRight = findViewById(R.id.joystickRight);

        bt = new BluetoothManager();
        broadcaster = new BroadcastTransmitter();

        bt.setConnectionListener(new BluetoothManager.OnConnectionListener() {
            @Override public void onConnected(String name) {
                tvStatus.setText("✅ Подключено: " + name);
                tvStatus.setTextColor(0xFF00FF88);
            }
            @Override public void onDisconnected() {
                tvStatus.setText("❌ Отключено");
                tvStatus.setTextColor(0xFFFF4444);
            }
            @Override public void onError(String msg) {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                tvStatus.setText("⚠ " + msg);
                tvStatus.setTextColor(0xFFFFAA00);
            }
        });

        bt.setDataListener(data -> {
            ProtocolParser.TelemetryData t = ProtocolParser.parse(data);
            if (t != null) {
                tvTelemetry.setText(String.format(
                        "ALT: %.1f м | SPD: %.1f м/с | BAT: %d%% | MODE: %d",
                        t.altitude, t.speed, t.batteryPercent, t.flightMode));
                broadcaster.broadcastTelemetry(t);
            }
        });

        btnConnect.setOnClickListener(v -> {
            if (bt.isConnected()) {
                bt.disconnect();
                broadcaster.stop();
                tvBroadcast.setText("📡 Broadcast: выключен");
            } else {
                requestBtPermissions();
                if (bt.connect()) {
                    broadcaster.start();
                    tvBroadcast.setText("📡 Broadcast: включён (порт 8888)");
                    sendHandler.post(sendRunnable);
                }
            }
        });

        // Левый джойстик: throttle (Y) + yaw (X)
        joystickLeft.setOnJoystickMoveListener((x, y) -> {
            throttle = (int) ((y + 1) * 127.5f);
            yaw = (int) ((x + 1) * 127.5f);
        });

        // Правый джойстик: pitch (Y) + roll (X)
        joystickRight.setOnJoystickMoveListener((x, y) -> {
            pitch = (int) ((y + 1) * 127.5f);
            roll = (int) ((x + 1) * 127.5f);
        });

        findViewById(R.id.btnStabilize).setOnClickListener(v -> flightMode = 0);
        findViewById(R.id.btnAltHold).setOnClickListener(v -> flightMode = 1);
        findViewById(R.id.btnLoiter).setOnClickListener(v -> flightMode = 2);
    }

    private void requestBtPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, 1);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sendHandler.removeCallbacks(sendRunnable);
        if (bt != null) bt.disconnect();
        if (broadcaster != null) broadcaster.stop();
    }
}