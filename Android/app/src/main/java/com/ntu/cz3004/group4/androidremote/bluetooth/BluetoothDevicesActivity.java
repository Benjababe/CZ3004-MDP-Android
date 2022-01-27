package com.ntu.cz3004.group4.androidremote.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.ntu.cz3004.group4.androidremote.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BluetoothDevicesActivity extends AppCompatActivity {

    private ListView devicesListView;
    private ListView newDevicesListView;
    private ArrayAdapter devicesAdapter;
    private ArrayAdapter mNewDevicesArrayAdapter;
    private ArrayList<BluetoothDevice> newDevicesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_devices);
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // [TODO] Discover devices not paired -> Allow uses to pair from app?
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        ArrayList<BluetoothDevice> list = new ArrayList<>();

        if(!pairedDevices.isEmpty()) {
            list.addAll(pairedDevices);

            devicesListView = findViewById(R.id.lv_devices);
            devicesAdapter = new DeviceAdapter(BluetoothDevicesActivity.this, list);
            devicesListView.setAdapter(devicesAdapter);
        }

        newDevicesList = new ArrayList();

        doDiscovery();
        IntentFilter filter2 = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver,filter2);

        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);



        newDevicesListView = findViewById(R.id.lv_new_devices);
        mNewDevicesArrayAdapter = new DeviceAdapter(BluetoothDevicesActivity.this,  newDevicesList);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);

    }
    private void doDiscovery() {
        // If we're already discovering, stop it
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        // Request discover from BluetoothAdapter
        bluetoothAdapter.startDiscovery();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Make sure we're not doing discovery anymore
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            bluetoothAdapter.cancelDiscovery();
        }

        // Unregister broadcast listeners
        this.unregisterReceiver(mReceiver);
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
            Activity ctx = (Activity) getContext();
            Intent data = new Intent();
            data.putExtra("bluetooth_address", device.getAddress());
            ctx.setResult(Activity.RESULT_OK, data);
            ctx.finish();
        }
    }
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("Receiver", "Test'");

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed already
                if (device != null && device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    mNewDevicesArrayAdapter.add(device);
                }
                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setProgressBarIndeterminateVisibility(false);
                if (mNewDevicesArrayAdapter.getCount() == 0) {
//                    mNewDevicesArrayAdapter.add("No device found");
                    Toast.makeText(BluetoothDevicesActivity.this, "No devices found", Toast.LENGTH_LONG).show();
                }
            }
        }
    };

}