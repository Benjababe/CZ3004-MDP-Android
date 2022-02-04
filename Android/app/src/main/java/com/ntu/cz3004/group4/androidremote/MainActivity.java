package com.ntu.cz3004.group4.androidremote;

import static android.view.DragEvent.ACTION_DRAG_ENTERED;
import static android.view.DragEvent.ACTION_DRAG_EXITED;
import static android.view.DragEvent.ACTION_DROP;
import static com.ntu.cz3004.group4.androidremote.bluetooth.BluetoothService.STATE_CONNECTED;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.DragEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.FragmentManager;

import com.ntu.cz3004.group4.androidremote.arena.ArenaButton;
import com.ntu.cz3004.group4.androidremote.arena.ObstacleImages;
import com.ntu.cz3004.group4.androidremote.arena.ObstacleInfo;
import com.ntu.cz3004.group4.androidremote.bluetooth.BluetoothDevicesActivity;
import com.ntu.cz3004.group4.androidremote.bluetooth.BluetoothListener;
import com.ntu.cz3004.group4.androidremote.bluetooth.BluetoothService;
import com.ntu.cz3004.group4.androidremote.fragments.ConsoleFragment;
import com.ntu.cz3004.group4.androidremote.fragments.MapFragment;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@SuppressWarnings({"ConstantConditions", "IntegerDivisionInFloatingPointContext"})
public class MainActivity extends AppCompatActivity implements BluetoothListener, SensorEventListener {
    private ActivityResultLauncher<Intent> activityLauncher;

    BluetoothAdapter adapter;
    BluetoothService bluetoothService;
    BluetoothDevice robotDev;
    String robotMACAddr = "";

    ConsoleFragment fragmentConsole;
    MapFragment fragmentMap;

    Button btnConnect;
    TextView txtConsole;
    ImageView imgRecog;
    RadioGroup spawnGroup;
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    SwitchCompat switchTiltControl;

    Pattern msgPattern;

    SensorManager sensorManager;
    Sensor accel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initBT();
        initMotionControl();
    }

    // finds all used views in UI
    private void initViews() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentConsole = (ConsoleFragment) fragmentManager.findFragmentById(R.id.fragmentConsole);
        fragmentMap = (MapFragment) fragmentManager.findFragmentById(R.id.fragmentMap);

        btnConnect = findViewById(R.id.btnConnect);
        txtConsole = findViewById(R.id.txtConsole);
        imgRecog = findViewById(R.id.imgRecog);
        spawnGroup = findViewById(R.id.spawnGroup);
        switchTiltControl = findViewById(R.id.switchTiltControl);

        LinearLayout mainLayout = findViewById(R.id.main_layout);
        mainLayout.setOnDragListener(new OutOfBoundsDragListener());

        fragmentMap.setSpawnGroup(spawnGroup);
    }

    private void initBT() {
        adapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothService = new BluetoothService(btMsgHandler);
        fragmentConsole.setBluetoothService(bluetoothService);
        fragmentMap.setBluetoothService(bluetoothService);

        // passes onBluetoothStatusChange defined here into BluetoothService so it can manipulate views
        bluetoothService.setBluetoothStatusChange(this);

        // message regex for image recognition
        String regex = "imgrec\\s+(\\d+)\\s+(\\w+)";
        msgPattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

        promptBTPermissions();
        listenBTFragment();
    }

    private void initMotionControl() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onSensorChanged(SensorEvent e) {
        if (!switchTiltControl.isChecked())
            return;

        // tilt left
        if (e.values[0] > 3)
            onBtnSteerLeftClick(null);

        // tilt right
        if (e.values[0] < -3)
            onBtnSteerRightClick(null);

        // tilt forward
        if (e.values[1] < -3)
            onBtnAccelClick(null);

        // tilt backwards
        if (e.values[1] > 3)
            onBtnReverseClick(null);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    private final Handler btMsgHandler = new Handler(Looper.myLooper(), message -> {
        if (message.what == Constants.MESSAGE_READ) {
            byte[] readBuf = (byte[]) message.obj;
            String strMessage = new String(readBuf, 0, message.arg1);
            Matcher m = msgPattern.matcher(strMessage);

            if (m.matches())
                drawObstacleImg(m);
        }
        return false;
    });

    private void drawObstacleImg(Matcher m) {
        // retrieve values from bluetooth message
        int obstacleID = Integer.parseInt(m.group(1).trim());
        String imgName = m.group(2).toLowerCase(Locale.ROOT).trim();

        // gets image to be drawn
        Drawable imgDrawable = getImgDrawable(imgName);

        // gets corresponding obstacle
        ObstacleInfo obsInfo = fragmentMap.getObstacles().get(obstacleID);

        // draws recognised image onto obstacle block
        ArenaButton btn = findViewById(obsInfo.btnID);
        btn.setBackground(imgDrawable);
        btn.setText("");
    }

    // gets drawable object of recognised image to draw onto arena cell
    private Drawable getImgDrawable(String imgName) {
        int drawableID = ObstacleImages.getDrawableID(imgName);
        return AppCompatResources.getDrawable(this, drawableID);
    }

    private void promptBTPermissions() {
        // permissions to handle bluetooth
        if (checkSelfPermission(Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED) {
            btnConnect.setEnabled(true);
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder
                    .setMessage("This app requires Bluetooth to connect to the robot")
                    .setTitle("Alert");

            AlertDialog dialog = builder.create();
            dialog.show();

            btnConnect.setEnabled(false);
        }
    }

    private void listenBTFragment() {
        activityLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if(result.getResultCode() == Activity.RESULT_OK) {
                // retrieves data sent from closed BT intent
                Intent intent = result.getData();
                Bundle intentBundle = intent.getExtras();

                // connects with the selected device from BT intent
                robotMACAddr = intentBundle.getString("bluetooth_address");
                robotDev = adapter.getRemoteDevice(robotMACAddr);
                bluetoothService.connect(robotDev);

                // updates UI on connection
                if (bluetoothService.state == STATE_CONNECTED) {
                    fragmentConsole.setBluetoothService(bluetoothService);
                    fragmentMap.setBluetoothService(bluetoothService);
                }
            }
        });
    }

    // handles the runnable for reconnecting to bluetooth device
    Handler reconnectHandler = new Handler();

    // runnable that runs code to reconnect to bluetooth device
    Runnable reconnectRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                if (bluetoothService.state == STATE_CONNECTED)
                    reconnectHandler.removeCallbacks(reconnectRunnable);
                bluetoothService.connect(robotDev);
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "Failed to reconnect, trying in 5 second", Toast.LENGTH_SHORT).show();
            }
        }
    };


    // prompt user whether to reconnect to bluetooth device
    private void promptReconnect() {
        new AlertDialog.Builder(this).setTitle("Reconnect to Bluetooth Device")
        .setPositiveButton("Yes", (dialogInterface, i) -> reconnectHandler.postDelayed(reconnectRunnable, 2000))
        .setNegativeButton("No", null)
        .show();
    }

    // adopted from BluetoothListener interface, used in BluetoothService class
    public void onBluetoothStatusChange(int status) {
        ArrayList<String> text = new ArrayList<>(Arrays.asList("Not Connected", "", "Connecting", "Connected"));
        ArrayList<String> col = new ArrayList<>(Arrays.asList("#FFFF0000", "", "#FFFFFF00", "#FF00FF00"));

        runOnUiThread(() -> {
            btnConnect.setText(text.get(status));
            btnConnect.setTextColor(Color.parseColor(col.get(status)));

            if (bluetoothService.state == BluetoothService.STATE_NONE) {
                promptReconnect();
            }
        });
    }


    // for drag events to anything outside the grid
    // remove the dragged item
    private class OutOfBoundsDragListener implements View.OnDragListener {
        @Override
        public boolean onDrag(View view, DragEvent e) {
            switch (e.getAction()) {
                case ACTION_DRAG_ENTERED:
                case ACTION_DRAG_EXITED:
                    return true;

                case ACTION_DROP:
                    ArenaButton originalBtn = (ArenaButton) e.getLocalState();
                    fragmentMap.emptyCell(originalBtn);
                    return true;
            }
            return true;
        }
    }


    public void onBtnConnectClick(View view) {
        Intent intent = new Intent(MainActivity.this, BluetoothDevicesActivity.class);
        bluetoothService.start();
        activityLauncher.launch(intent);
    }

    public void onBtnAccelClick(View view) {
        if (bluetoothService.state == STATE_CONNECTED)
            bluetoothService.write("f".getBytes(StandardCharsets.UTF_8));
    }

    public void onBtnReverseClick(View view) {
        if (bluetoothService.state == STATE_CONNECTED)
            bluetoothService.write("r".getBytes(StandardCharsets.UTF_8));
    }

    public void onBtnSteerLeftClick(View view) {
        if (bluetoothService.state == STATE_CONNECTED)
            bluetoothService.write("sl".getBytes(StandardCharsets.UTF_8));
    }

    public void onBtnSteerRightClick(View view) {
        if (bluetoothService.state == STATE_CONNECTED)
            bluetoothService.write("sr".getBytes(StandardCharsets.UTF_8));
    }

    public void onImageRecogClick(View view) {
        if (bluetoothService.state == STATE_CONNECTED)
            bluetoothService.write("img_recog".getBytes(StandardCharsets.UTF_8));
    }

    public void onFastestPathClick(View view) {
        if (bluetoothService.state == STATE_CONNECTED)
            bluetoothService.write("fastest_path".getBytes(StandardCharsets.UTF_8));
    }

    public void onResetClick(View view) {
        fragmentMap.reset();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bluetoothService.stop();
    }
}




































