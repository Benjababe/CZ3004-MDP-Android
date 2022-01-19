package com.ntu.cz3004.group4.androidremote.bluetooth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.ntu.cz3004.group4.androidremote.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BluetoothDevicesActivity extends AppCompatActivity {

    private ListView devicesListView;
    private ArrayAdapter devicesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_devices);

        // [TODO] Discover devices not paired -> Allow uses to pair from app?
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        ArrayList<BluetoothDevice> list = new ArrayList();

        if(!pairedDevices.isEmpty()) {
            for(BluetoothDevice device : pairedDevices) {
                list.add(device);
            }

            devicesListView = findViewById(R.id.lv_devices);
            devicesAdapter = new DeviceAdapter(BluetoothDevicesActivity.this, list);
            devicesListView.setAdapter(devicesAdapter);
        }
    }

    private class DeviceAdapter extends ArrayAdapter<BluetoothDevice> {

        public DeviceAdapter(@NonNull Context context, @NonNull List<BluetoothDevice> devices) {
            super(context, 0, devices);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            BluetoothDevice device = getItem(position);

            if(convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_bluetooth_device, parent, false);
            }

            TextView tvName = convertView.findViewById(R.id.tv_device_name);
            TextView tvAddress = convertView.findViewById(R.id.tv_device_address);
            Button btnConnect = convertView.findViewById(R.id.btn_connect_device);

            tvName.setText(device.getName());
            tvAddress.setText(device.getAddress());
            btnConnect.setOnClickListener(view -> onClickConnect(device));

            return convertView;
        }

        private void onClickConnect(BluetoothDevice device) {
            Activity ctx = ((Activity) getContext());
            Intent data = new Intent();
            data.putExtra("bluetooth_address", device.getAddress());
            ctx.setResult(Activity.RESULT_OK, data);
            ctx.finish();
        }
    }
}