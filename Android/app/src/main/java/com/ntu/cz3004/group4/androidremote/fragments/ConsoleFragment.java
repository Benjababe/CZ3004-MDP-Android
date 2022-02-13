package com.ntu.cz3004.group4.androidremote.fragments;

import android.os.Bundle;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.ntu.cz3004.group4.androidremote.Constants;
import com.ntu.cz3004.group4.androidremote.R;
import com.ntu.cz3004.group4.androidremote.bluetooth.BluetoothService;

import java.util.Map;


public class ConsoleFragment extends Fragment {

    private ListView listViewConsole;
    private EditText editTextPrompt;
    private Button btnSend;

    private ArrayAdapter<String> consoleArrayAdapter;
    private String connectedDeviceName;

    private BluetoothService bluetoothService;

    private final Handler handler = new Handler(Looper.myLooper(), message -> {
        switch (message.what) {
            case Constants.MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) message.obj;
                // construct a string from the buffer
                String writeMessage = new String(writeBuf);
                consoleArrayAdapter.add("Me:  " + writeMessage);
                break;

            case Constants.MESSAGE_READ:
                byte[] readBuf = (byte[]) message.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, message.arg1);
                consoleArrayAdapter.add(connectedDeviceName + ":  " + readMessage);
                break;

            case Constants.MESSAGE_DEVICE_NAME:
                // save the connected device's name
                btnSend.setEnabled(true);
                connectedDeviceName = message.getData().getString(Constants.DEVICE_NAME);
                if (connectedDeviceName != null) {
                    Toast.makeText(getContext(), "Connected to " + connectedDeviceName, Toast.LENGTH_SHORT).show();
                }
                break;
        }
        return false;
    });

    public ConsoleFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_console, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        listViewConsole = view.findViewById(R.id.lv_console);
        editTextPrompt = view.findViewById(R.id.et_command);
        btnSend = view.findViewById(R.id.btn_send);

        btnSend.setOnClickListener(v -> onClickSend());

        consoleArrayAdapter = new ArrayAdapter<>(getContext(), R.layout.item_message);
        listViewConsole.setAdapter(consoleArrayAdapter);
    }

    private void onClickSend() {
        String data = editTextPrompt.getText().toString();
        editTextPrompt.setText("");
        bluetoothService.write(data.getBytes());
    }

    public void setBluetoothService(BluetoothService bluetoothService) {
        this.bluetoothService = bluetoothService;
    }

    public Handler getHandler() {
        return handler;
    }
}