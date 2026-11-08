
package com.myrobotcar.controller;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity {

    BluetoothAdapter bluetoothAdapter;
    BluetoothSocket bluetoothSocket;
    OutputStream outputStream;

    TextView statusText;
    TextView speedText;

    SeekBar speedBar;

    final UUID BT_UUID = UUID.fromString(
        "00001101-0000-1000-8000-00805F9B34FB"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        createInterface();
    }

    void createInterface() {

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setPadding(25, 25, 25, 25);
        main.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title = new TextView(this);
        title.setText("MY ROBOT CAR");
        title.setTextSize(27);
        title.setGravity(Gravity.CENTER);
        main.addView(title);

        statusText = new TextView(this);
        statusText.setText("Bluetooth: Disconnected");
        statusText.setTextSize(17);
        statusText.setGravity(Gravity.CENTER);
        main.addView(statusText);

        Button connect = new Button(this);
        connect.setText("CONNECT BLUETOOTH");
        main.addView(connect);

        connect.setOnClickListener(v -> showDevices());

        addButton(main, "FORWARD", "F");
        addButton(main, "LEFT", "L");
        addButton(main, "STOP", "S");
        addButton(main, "RIGHT", "R");
        addButton(main, "BACKWARD", "B");

        TextView speedTitle = new TextView(this);
        speedTitle.setText("SPEED CONTROL");
        speedTitle.setTextSize(20);
        main.addView(speedTitle);

        speedText = new TextView(this);
        speedText.setText("Speed: 7 / 9");
        main.addView(speedText);

        speedBar = new SeekBar(this);
        speedBar.setMax(9);
        speedBar.setProgress(7);
        main.addView(speedBar);

        speedBar.setOnSeekBarChangeListener(
            new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(
                    SeekBar seekBar, int progress, boolean fromUser) {

                    speedText.setText("Speed: " + progress + " / 9");

                    if (fromUser) {
                        sendCommand(
                            String.valueOf(progress)
                        );
                    }
                }

                public void onStartTrackingTouch(SeekBar seekBar) {}

                public void onStopTrackingTouch(SeekBar seekBar) {}
            }
        );

        setContentView(main);
    }

    void addButton(
        LinearLayout layout,
        String label,
        String command
    ) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(18);

        layout.addView(button);

        button.setOnClickListener(v -> {
            sendCommand(command);
        });
    }

    void showDevices() {

        if (bluetoothAdapter == null) {
            Toast.makeText(
                this, "Bluetooth not supported",
                Toast.LENGTH_LONG
            ).show();
            return;
        }

        if (Build.VERSION.SDK_INT >= 31 &&
            checkSelfPermission(
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                new String[]{
                    Manifest.permission.BLUETOOTH_CONNECT
                }, 100
            );
            return;
        }

        try {
            if (!bluetoothAdapter.isEnabled()) {
                Toast.makeText(
                    this,
                    "Please turn on Bluetooth",
                    Toast.LENGTH_LONG
                ).show();
                return;
            }

            Set<BluetoothDevice> paired =
                bluetoothAdapter.getBondedDevices();

            ArrayList<BluetoothDevice> devices =
                new ArrayList<>(paired);

            if (devices.isEmpty()) {
                Toast.makeText(
                    this,
                    "Pair HC-05 in phone settings first",
                    Toast.LENGTH_LONG
                ).show();
                return;
            }

            String[] names = new String[devices.size()];

            for (int i = 0; i < devices.size(); i++) {
                String name = devices.get(i).getName();
                names[i] = name == null ? "Bluetooth Device" : name;
            }

            new AlertDialog.Builder(this)
                .setTitle("Select Bluetooth Device")
                .setItems(names, (dialog, which) -> {
                    connectDevice(devices.get(which));
                })
                .show();

        } catch (SecurityException e) {
            Toast.makeText(
                this, "Bluetooth permission required",
                Toast.LENGTH_LONG
            ).show();
        }
    }

    void connectDevice(BluetoothDevice device) {

        statusText.setText("Connecting...");

        new Thread(() -> {
            try {
                BluetoothSocket socket =
                    device.createRfcommSocketToServiceRecord(BT_UUID);

                socket.connect();

                bluetoothSocket = socket;
                outputStream = socket.getOutputStream();

                runOnUiThread(() ->
                    statusText.setText("Bluetooth: Connected")
                );

            } catch (Exception e) {

                runOnUiThread(() -> {
                    statusText.setText("Connection Failed");
                    Toast.makeText(
                        this,
                        "Could not connect to HC-05",
                        Toast.LENGTH_LONG
                    ).show();
                });
            }
        }).start();
    }

    void sendCommand(String command) {

        if (outputStream == null) {
            Toast.makeText(
                this,
                "Connect Bluetooth first",
                Toast.LENGTH_SHORT
            ).show();
            return;
        }

        try {
            outputStream.write(
                command.getBytes()
            );
            outputStream.flush();

        } catch (Exception e) {
            statusText.setText("Connection Lost");
        }
    }

    @Override
    public void onRequestPermissionsResult(
        int requestCode,
        String[] permissions,
        int[] grantResults
    ) {
        super.onRequestPermissionsResult(
            requestCode, permissions, grantResults
        );

        if (requestCode == 100 &&
            grantResults.length > 0 &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            showDevices();
        } else {
            Toast.makeText(
                this,
                "Bluetooth permission denied",
                Toast.LENGTH_LONG
            ).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            if (outputStream != null) outputStream.close();
            if (bluetoothSocket != null) bluetoothSocket.close();
        } catch (Exception ignored) {}
    }
                      }
