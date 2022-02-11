package com.ntu.cz3004.group4.androidremote.fragments;

import static com.ntu.cz3004.group4.androidremote.Constants.A_MOVE_BACKWARD;
import static com.ntu.cz3004.group4.androidremote.Constants.A_MOVE_FORWARD;
import static com.ntu.cz3004.group4.androidremote.bluetooth.BluetoothService.STATE_CONNECTED;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.ntu.cz3004.group4.androidremote.R;
import com.ntu.cz3004.group4.androidremote.bluetooth.BluetoothService;
import com.ntu.cz3004.group4.androidremote.bluetooth.Packet;

import org.json.JSONException;

import java.nio.charset.StandardCharsets;

public class LeftColFragment extends Fragment {
    ListView lvConsole;
    TextView txtRoboStatus, txtRoboDirection, txtRoboPosition;
    ImageView imgRecog;
    ImageButton btnAccel, btnReverse;

    ArrayAdapter<String> consoleArrayAdapter;

    BluetoothService bluetoothService;

    public LeftColFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) { super.onCreate(savedInstanceState); }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_left_col, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        lvConsole = view.findViewById(R.id.lvConsole);
        txtRoboStatus = view.findViewById(R.id.txtRoboStatus);
        txtRoboDirection = view.findViewById(R.id.txtRoboDirection);
        txtRoboPosition = view.findViewById(R.id.txtRoboPosition);
        imgRecog = view.findViewById(R.id.imgRecog);
        btnAccel = view.findViewById(R.id.btnAccel);
        btnReverse = view.findViewById(R.id.btnReverse);

        btnAccel.setOnClickListener(this::accelerate);
        btnReverse.setOnClickListener(this::reverse);

        consoleArrayAdapter = new ArrayAdapter<>(getContext(), R.layout.item_message);
        lvConsole.setAdapter(consoleArrayAdapter);
        lvConsole.setSelection(consoleArrayAdapter.getCount()-1);
    }

    public void accelerate(View view) {
        if (bluetoothService.state == STATE_CONNECTED) {
            try {
                Packet packet = new Packet(A_MOVE_FORWARD);
                bluetoothService.write(packet.getJSONString().getBytes(StandardCharsets.UTF_8));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void reverse(View view) {
        if (bluetoothService.state == STATE_CONNECTED)
            try {
                Packet packet = new Packet(A_MOVE_BACKWARD);
                bluetoothService.write(packet.getJSONString().getBytes(StandardCharsets.UTF_8));
            } catch (JSONException e) {
                e.printStackTrace();
            }
    }

    public void setBluetoothService(BluetoothService bluetoothService) {
        this.bluetoothService = bluetoothService;
    }

    public void addConsoleMessage(String message) {
        consoleArrayAdapter.add(message);
    }

    public void setRoboStatus(String status) {
        txtRoboStatus.setText(status);
    }

    public void setRoboDirection(String direction) {
        txtRoboDirection.setText(direction);
    }

    public void setRobotPosition(String position) {
        txtRoboPosition.setText(position);
    }
}
