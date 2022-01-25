package com.ntu.cz3004.group4.androidremote.bluetooth;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.ntu.cz3004.group4.androidremote.Constants;
import com.ntu.cz3004.group4.androidremote.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

@SuppressLint("MissingPermission")
public class BluetoothService {
    private static final String TAG = "BluetoothService";

    private static final String APP_NAME = "BluetoothChatSecure";
    private static final UUID APP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private ConnectThread connectThread = null;
    private ConnectedThread connectedThread = null;
    private AcceptThread acceptThread = null;

    private final Handler handler;
    private BluetoothAdapter bluetoothAdapter;
    public int state;
    private BluetoothListener btListener;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    public BluetoothService(Handler handler) {
        this.handler = handler;

        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.state = STATE_NONE;
    }

    private boolean checkPermission() {
        return false;
    }

    public void setBluetoothStatusChange(BluetoothListener btListener) {
        this.btListener = btListener;
    }

    public synchronized void start() {
        // Cancel any thread attempting to make a connection
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        // Cancel any thread currently running a connection
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        if (acceptThread == null) {
            acceptThread = new AcceptThread();
            acceptThread.start();
        }

        // update UI
        updateBTConnected(STATE_NONE);
    }

    public synchronized void connect(BluetoothDevice device) {
        if (state == STATE_CONNECTING) {
            if (connectThread != null) {
                connectThread.cancel();
                connectThread = null;
            }
        }

        if(connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        if(connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        connectThread = new ConnectThread(device);
        connectThread.start();

        // update UI
        updateBTConnected(STATE_CONNECTING);
    }


    public synchronized void connected(BluetoothSocket bluetoothSocket, BluetoothDevice bluetoothDevice) {
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        if(acceptThread != null){
            acceptThread.cancel();
            acceptThread = null;
        }

        connectedThread = new ConnectedThread(bluetoothSocket);
        connectedThread.start();

        Message message = handler.obtainMessage(Constants.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.DEVICE_NAME, bluetoothDevice.getName());
        message.setData(bundle);
        handler.sendMessage(message);

        //update UI
        updateBTConnected(STATE_CONNECTED);
    }

    public synchronized void stop() {
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }

        state = STATE_NONE;

        //update ui
        updateBTConnected(STATE_NONE);
    }

    private void connectionFailed() {
        Message message = handler.obtainMessage(Constants.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Unable to connect device");
        message.setData(bundle);
        handler.sendMessage(message);

        state = STATE_NONE;

        // update ui
        updateBTConnected(STATE_NONE);
    }

    private void connectionLost() {
        Message message = handler.obtainMessage(Constants.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Device connection was lost");
        message.setData(bundle);
        handler.sendMessage(message);

        state = STATE_NONE;

        // update ui
        updateBTConnected(STATE_NONE);
    }

    private void updateBTConnected(int status) {
        btListener.onBluetoothStatusChange(status);
    }

    public void write(byte[] out) {
        ConnectedThread r;
        synchronized (this) {
            if (state != STATE_CONNECTED) return;
            r = connectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    private class ConnectThread extends Thread {
        private BluetoothSocket bluetoothSocket;
        private final BluetoothDevice bluetoothDevice;

        public ConnectThread(BluetoothDevice bluetoothDevice) {
            this.bluetoothDevice = bluetoothDevice;
            BluetoothSocket tmp = null;

            try {
                tmp = bluetoothDevice.createRfcommSocketToServiceRecord(APP_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }

            bluetoothSocket = tmp;
            state = STATE_CONNECTING;
        }

        public void run() {
            BluetoothSocket socket = null;

            try {
                socket = bluetoothDevice.createRfcommSocketToServiceRecord(APP_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }

            bluetoothSocket = socket;
            bluetoothAdapter.cancelDiscovery();

            try {
                bluetoothSocket.connect();
            } catch (IOException e) {
                try {
                    bluetoothSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "Unable to close() socket during connection failure", e2);
                }

                connectionFailed();
                return;
            }

            connected(bluetoothSocket, bluetoothDevice);
        }

        public void cancel() {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Socket's close() failed", e);
            }
        }
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket bluetoothServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;

            try{
                tmp = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(APP_NAME, APP_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's listen() failed", e);
            }

            bluetoothServerSocket = tmp;
            state = STATE_LISTEN;
        }

        public void run() {
            BluetoothSocket socket;

            while (state != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = bluetoothServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket accept() failed", e);
                    break;
                }

                if (socket != null) {
                    synchronized (BluetoothService.this) {
                        switch (state) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // Situation normal. Start the connected thread.
                                connected(socket, socket.getRemoteDevice());
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                // Either not ready or already connected. Terminate new socket.
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close unwanted socket", e);
                                }
                                break;
                        }
                    }
                }
            }
        }

        public void cancel() {
            try {
                bluetoothServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Close() of server failed", e);
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket socket;
        private final InputStream is;
        private final OutputStream os;
        private byte[] buffer;

        public ConnectedThread(BluetoothSocket socket) {
            this.socket = socket;
            InputStream _is = null;
            OutputStream _os = null;

            try {
                _is = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating input stream", e);
            }

            try {
                _os = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating output stream", e);
            }

            this.is = _is;
            this.os = _os;
            state = STATE_CONNECTED;
        }

        public void run() {
            buffer = new byte[1024];
            int bytes;

            while(state == STATE_CONNECTED) {
                try {
                    bytes = is.read(buffer);
                    handler.obtainMessage(Constants.MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    Log.d(TAG, "Input Stream was disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                os.write(bytes);
                handler.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Error occured while trying to send data", e);
                // port failure message to activity
            }
        }

        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the socket", e);
            }
        }
    }
}