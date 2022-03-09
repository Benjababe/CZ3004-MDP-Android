package com.ntu.cz3004.group4.androidremote.fragments;

import static com.ntu.cz3004.group4.androidremote.Constants.A_FASTEST_PATH;
import static com.ntu.cz3004.group4.androidremote.Constants.A_IMG_REC;
import static com.ntu.cz3004.group4.androidremote.Constants.A_MOVE_LEFT;
import static com.ntu.cz3004.group4.androidremote.Constants.A_MOVE_RIGHT;
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
import com.ntu.cz3004.group4.androidremote.arena.ObstacleInfo;
import com.ntu.cz3004.group4.androidremote.bluetooth.BluetoothService;
import com.ntu.cz3004.group4.androidremote.bluetooth.Packet;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class RightColFragment<pulbic> extends Fragment {
    Button btnConnect, btnImageRecog, btnFastestPath;
    RadioGroup spawnGroup;
    SwitchCompat switchTiltControl;
    ImageButton btnSteerLeft, btnSteerRight;
    ArrayList<JSONObject> S1 = new ArrayList<JSONObject>();
    BluetoothService bluetoothService;
    public boolean btEnabled = false;
    private MapFragment fragmentMap;
    public RightColFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

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
        if (bluetoothService.state == STATE_CONNECTED) {
            try {
                Packet packet = new Packet(A_IMG_REC);

                S1 = fragmentMap.getObstacleList();
                packet.setObstacleList(S1);
                bluetoothService.write(packet.getJSONString().getBytes(StandardCharsets.UTF_8));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void startFastestPath(View view) {
        if (bluetoothService.state == STATE_CONNECTED) {
            try {
                Packet packet = new Packet(A_FASTEST_PATH);
                bluetoothService.write(packet.getJSONString().getBytes(StandardCharsets.UTF_8));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }


    public void steerLeft(View view) {
        if (bluetoothService.state == STATE_CONNECTED) {
            try {
                Packet packet = new Packet(A_MOVE_LEFT);
                bluetoothService.write(packet.getJSONString().getBytes(StandardCharsets.UTF_8));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void steerRight(View view) {
        if (bluetoothService.state == STATE_CONNECTED) {
            try {
                Packet packet = new Packet(A_MOVE_RIGHT);
                bluetoothService.write(packet.getJSONString().getBytes(StandardCharsets.UTF_8));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
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
    public void setFragmentMap(MapFragment obj)
    {
        this.fragmentMap = obj;
    }
}
