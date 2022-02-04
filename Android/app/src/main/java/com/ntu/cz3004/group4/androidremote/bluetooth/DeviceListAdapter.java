package com.ntu.cz3004.group4.androidremote.bluetooth;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.ntu.cz3004.group4.androidremote.MainActivity;
import com.ntu.cz3004.group4.androidremote.R;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

public class DeviceListAdapter extends ArrayAdapter<BluetoothDevice> {
    private LayoutInflater layoutInflater;
    private ArrayList<BluetoothDevice> BTdevices;
    private int ViewResourceId;
    private Button btnPair;

    public DeviceListAdapter(@NonNull Context context, int textViewResourceId, @NonNull List<BluetoothDevice> devices) {
        super(context, textViewResourceId, devices);
        this.BTdevices = (ArrayList<BluetoothDevice>) devices;
        layoutInflater = (LayoutInflater) context.getSystemService(context.LAYOUT_INFLATER_SERVICE);
        ViewResourceId = textViewResourceId;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        convertView = layoutInflater.inflate(ViewResourceId, null);
        BluetoothDevice device = BTdevices.get(position);
        if (device != null) {
            TextView deviceName = (TextView) convertView.findViewById(R.id.tv_device_name);
            TextView deviceAddress = (TextView) convertView.findViewById(R.id.tv_device_address);
            if (deviceName != null) {
                deviceName.setText(device.getName());
            }
            if(deviceAddress != null)
            {
                deviceAddress.setText(device.getAddress());
            }
        }
        btnPair = convertView.findViewById(R.id.btn_pair_device);
        btnPair.setOnClickListener(view -> onClickPair(device));
        return convertView;
    };

    private void onClickPair(BluetoothDevice device) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
            device.createBond();
        }
    }
}
