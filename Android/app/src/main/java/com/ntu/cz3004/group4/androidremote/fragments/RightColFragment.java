package com.ntu.cz3004.group4.androidremote.fragments;

import static com.ntu.cz3004.group4.androidremote.bluetooth.BluetoothService.STATE_CONNECTED;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioGroup;

import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.ntu.cz3004.group4.androidremote.R;
import com.ntu.cz3004.group4.androidremote.bluetooth.BluetoothService;

import java.nio.charset.StandardCharsets;

public class RightColFragment extends Fragment {
    Button btnConnect, btnImageRecog, btnFastestPath;
    RadioGroup spawnGroup;
    SwitchCompat switchTiltControl;
    ImageButton btnSteerLeft, btnSteerRight;

    BluetoothService bluetoothService;
    public boolean btEnabled = false;

    public RightColFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) { super.onCreate(savedInstanceState); }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_right_col, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        btnConnect = view.findViewById(R.id.btnConnect);
        btnImageRecog = view.findViewById(R.id.btnImageRecog);
        btnFastestPath = view.findViewById(R.id.btnFastestPath);
        spawnGroup = view.findViewById(R.id.spawnGroup);
        switchTiltControl = view.findViewById(R.id.switchTiltControl);
        btnSteerLeft = view.findViewById(R.id.btnSteerLeft);
        btnSteerRight = view.findViewById(R.id.btnSteerRight);

        btnImageRecog.setOnClickListener(this::startImageRecog);
        btnFastestPath.setOnClickListener(this::startFastestPath);
        btnSteerLeft.setOnClickListener(this::steerLeft);
        btnSteerRight.setOnClickListener(this::steerRight);
    }


    public void startImageRecog(View view) {
        if (bluetoothService.state == STATE_CONNECTED)
            bluetoothService.write("img_recog".getBytes(StandardCharsets.UTF_8));
    }

    public void startFastestPath(View view) {
        if (bluetoothService.state == STATE_CONNECTED)
            bluetoothService.write("fastest_path".getBytes(StandardCharsets.UTF_8));
    }


    public void steerLeft(View view) {
        if (bluetoothService.state == STATE_CONNECTED)
            bluetoothService.write("sl".getBytes(StandardCharsets.UTF_8));
    }

    public void steerRight(View view) {
        if (bluetoothService.state == STATE_CONNECTED)
            bluetoothService.write("sr".getBytes(StandardCharsets.UTF_8));
    }


    public void setBluetoothService(BluetoothService bluetoothService) {
        this.bluetoothService = bluetoothService;
    }

    public RadioGroup getSpawnGroup() {
        return spawnGroup;
    }

    public SwitchCompat getSwitchTiltControl() {
        return switchTiltControl;
    }

    public Button getBtnConnect() {
        return btnConnect;
    }
}
