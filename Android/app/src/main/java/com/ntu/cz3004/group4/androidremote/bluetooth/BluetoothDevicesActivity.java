package com.ntu.cz3004.group4.androidremote.bluetooth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.ntu.cz3004.group4.androidremote.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class BluetoothDevicesActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private ListView devicesListView;
    private ArrayAdapter devicesAdapter;

    public DeviceListAdapter mDeviceListAdapter;
    public ArrayList<BluetoothDevice> list1 = new ArrayList<>();
    public ArrayList<BluetoothDevice> list2 = new ArrayList<>();
    ListView lv_devices;
    ListView lvScan;
    BluetoothAdapter bluetoothAdapter;
    boolean m1 = false, m3 = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_devices);

        //initialise button and switch and listview
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Button btnOnOff = findViewById(R.id.btnOnOff);
        Button btnDiscover = findViewById(R.id.btnDiscover);
        Button btnScan = findViewById(R.id.btnScan);
        lvScan = (ListView) findViewById(R.id.lvScan);
        btnOnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                enableDisableBT();
            }
        });
        //Code for Discoveriblity
        btnDiscover.setOnClickListener(view -> {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
                startActivityForResult(discoverableIntent, 1);
                return;
            }
        });
        //Code for Scan button
        btnScan.setOnClickListener(v -> {
            Log.d("Bluetooth Activity", "Button scan");
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
                    int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
                    permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
                    if (permissionCheck != 0) {

                        this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
                    }
                } else {
                    Log.d("Bluetooth Activity", "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");
                }
                bluetoothAdapter.startDiscovery();
                IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
                m3 = true;
            }
            if (!bluetoothAdapter.isDiscovering()) {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
                    int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
                    permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
                    if (permissionCheck != 0) {

                        this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
                    }
                } else {
                    Log.d("Bluetooth Activity", "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");
                }
                bluetoothAdapter.startDiscovery();
                IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
                m3 = true;
            }


        });

        // [TODO] Discover devices not paired -> Allow uses to pair from app?
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver4, filter);


        if (!pairedDevices.isEmpty()) {
            list1.addAll(pairedDevices);
            devicesListView = findViewById(R.id.lv_devices);
            devicesAdapter = new DeviceAdapter(BluetoothDevicesActivity.this, list1);
            devicesListView.setAdapter(devicesAdapter);
        }
    }

    public void enableDisableBT() {
        if (bluetoothAdapter == null) {
            Toast.makeText(BluetoothDevicesActivity.this, "Device does not have Bluetooth capabilities", Toast.LENGTH_SHORT).show();
        }
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                startActivity(enableBTIntent);
                return;
            }
        }
        if (bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.disable();
            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver1, BTIntent);
            m1 = true;
        }
    }

    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mBroadcastReceiver1 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (action.equals(bluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, bluetoothAdapter.ERROR);

                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d("Bluetooth Activity", "onReceive: STATE OFF");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d("Bluetooth Activity", "mBroadcastReceiver1: STATE TURNING OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d("Bluetooth Activity", "mBroadcastReceiver1: STATE ON");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d("Bluetooth Activity", "mBroadcastReceiver1: STATE TURNING ON");
                        break;
                }
            }
        }
    };

    //Broadcast Receiver for listing devices that are not yet paired Executed by btnScan() method.
    private BroadcastReceiver mBroadcastReceiver3 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getName() != null && device.getName().length() > 0 && !list1.contains(device) && !list2.contains(device))
                    list2.add(device);
                Log.d("Bluetooth Activity","Broadcast Receiver 3");
                mDeviceListAdapter = new DeviceListAdapter(context, R.layout.item_scan_bluetooth_device, list2);
                lvScan.setAdapter(mDeviceListAdapter);
            }
        }
    };

    private BroadcastReceiver mBroadcastReceiver4 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String TAG = "BTBOND";
            final String action = intent.getAction();

            if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(mDevice.getBondState() == BluetoothDevice.BOND_BONDED){
                    Log.d(TAG, "BOND_BONDED.");
                    list2.remove(mDevice);
                    list1.add(mDevice);

                    mDeviceListAdapter = new DeviceListAdapter(context, R.layout.item_scan_bluetooth_device, list2);
                    lvScan.setAdapter(mDeviceListAdapter);

                    devicesAdapter = new DeviceAdapter(BluetoothDevicesActivity.this, list1);
                    devicesListView.setAdapter(devicesAdapter);
                }
                if(mDevice.getBondState() == BluetoothDevice.BOND_BONDING){
                    Log.d(TAG, "BOND_BONDING.");
                }
                if(mDevice.getBondState() == BluetoothDevice.BOND_NONE){
                    Log.d(TAG, "BOND_NONE.");
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (m1)
            unregisterReceiver(mBroadcastReceiver1);
        if (m3)
            unregisterReceiver(mBroadcastReceiver3);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            bluetoothAdapter.cancelDiscovery();
            list2.get(i).createBond();
            return;
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

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_bluetooth_device, parent, false);
            }

            TextView tvName = convertView.findViewById(R.id.tv_device_name);
            TextView tvAddress = convertView.findViewById(R.id.tv_device_address);
            Button btnConnect = convertView.findViewById(R.id.btn_pair_device);

            if (ActivityCompat.checkSelfPermission(BluetoothDevicesActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                tvName.setText(device.getName());
                tvAddress.setText(device.getAddress());
                btnConnect.setOnClickListener(view -> onClickConnect(device));
                return convertView;
            }
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
}