package com.ntu.cz3004.group4.androidremote;

import static android.view.DragEvent.ACTION_DRAG_ENTERED;
import static android.view.DragEvent.ACTION_DRAG_EXITED;
import static android.view.DragEvent.ACTION_DROP;
import static com.ntu.cz3004.group4.androidremote.Constants.ADD_OBSTACLE;
import static com.ntu.cz3004.group4.androidremote.Constants.LOG;
import static com.ntu.cz3004.group4.androidremote.Constants.MOVE_BACKWARD;
import static com.ntu.cz3004.group4.androidremote.Constants.MOVE_FORWARD;
import static com.ntu.cz3004.group4.androidremote.Constants.REMOVE_OBSTACLE;
import static com.ntu.cz3004.group4.androidremote.Constants.TURN_LEFT;
import static com.ntu.cz3004.group4.androidremote.Constants.TURN_RIGHT;
import static com.ntu.cz3004.group4.androidremote.Constants.UPDATE;
import static com.ntu.cz3004.group4.androidremote.bluetooth.BluetoothService.STATE_CONNECTED;

import android.Manifest;
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
import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.FragmentManager;

import com.ntu.cz3004.group4.androidremote.arena.ArenaButton;
import com.ntu.cz3004.group4.androidremote.arena.ObstacleImages;
import com.ntu.cz3004.group4.androidremote.arena.ObstacleInfo;
import com.ntu.cz3004.group4.androidremote.bluetooth.BluetoothDevicesActivity;
import com.ntu.cz3004.group4.androidremote.bluetooth.BluetoothListener;
import com.ntu.cz3004.group4.androidremote.bluetooth.BluetoothService;
import com.ntu.cz3004.group4.androidremote.fragments.ConsoleFragment;
import com.ntu.cz3004.group4.androidremote.fragments.LeftColFragment;
import com.ntu.cz3004.group4.androidremote.fragments.MapFragment;
import com.ntu.cz3004.group4.androidremote.fragments.RightColFragment;

import org.json.JSONException;
import org.json.JSONObject;

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
    LeftColFragment fragmentLeftCol;
    RightColFragment fragmentRightCol;

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

    @Override
    protected void onStart() {
        super.onStart();
        fragmentMap.setSpawnGroup(fragmentRightCol.getSpawnGroup());
    }

    // finds all used views in UI
    private void initViews() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentConsole = (ConsoleFragment) fragmentManager.findFragmentById(R.id.fragmentConsole);
        fragmentMap = (MapFragment) fragmentManager.findFragmentById(R.id.fragmentMap);
        fragmentLeftCol = (LeftColFragment) fragmentManager.findFragmentById(R.id.fragmentLeftCol);
        fragmentRightCol = (RightColFragment) fragmentManager.findFragmentById(R.id.fragmentRightCol);

        LinearLayout mainLayout = findViewById(R.id.main_layout);
        mainLayout.setOnDragListener(new OutOfBoundsDragListener());
    }

    private void initBT() {
        adapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothService = new BluetoothService(btMsgHandler);
        fragmentConsole.setBluetoothService(bluetoothService);
        fragmentMap.setBluetoothService(bluetoothService);
        fragmentLeftCol.setBluetoothService(bluetoothService);
        fragmentRightCol.setBluetoothService(bluetoothService);

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
        if (!fragmentRightCol.getSwitchTiltControl().isChecked())
            return;

        // tilt left
        if (e.values[0] > 3)
            fragmentRightCol.steerLeft(null);

        // tilt right
        if (e.values[0] < -3)
            fragmentRightCol.steerRight(null);

        // tilt forward
        if (e.values[1] < -3)
            fragmentLeftCol.accelerate(null);

        // tilt backwards
        if (e.values[1] > 3)
            fragmentLeftCol.reverse(null);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    private final Handler btMsgHandler = new Handler(Looper.myLooper(), message -> {
        if (message.what == Constants.MESSAGE_READ) {
            byte[] readBuf = (byte[]) message.obj;
            String strMessage = new String(readBuf, 0, message.arg1);
            Log.d("receive from RPI", strMessage);
            JSONObject objRPI = null;
            try {
                objRPI = new JSONObject(strMessage);
                Log.d("RPI string", "string ok");
                setRPImsghandler(objRPI);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Matcher m = msgPattern.matcher(strMessage);

            if (m.matches())
                drawObstacleImg(m);
        }
        return false;
    });
    private void setRPImsghandler(JSONObject objRPI ) throws JSONException{
        JSONObject val = objRPI.getJSONObject("value");
        switch (objRPI.getInt("type"))
        {
            case MOVE_FORWARD:
                fragmentMap.moveRobot(true);
                break;
            case MOVE_BACKWARD:
                fragmentMap.moveRobot(false);
                break;
            case TURN_LEFT:
                fragmentMap.rotateRobot(null,-90);
                break;
            case TURN_RIGHT:
                fragmentMap.rotateRobot(null,90);
                break;
            case ADD_OBSTACLE:

                break;
            case REMOVE_OBSTACLE:
                break;
            case UPDATE:
                int x = val.getInt("ROBOT_X");
                int y = val.getInt("ROBOT_Y");
                fragmentMap.setRobotXY(x,y);

                break;
            case LOG:
                String msg = val.getString("MESSAGE");
                fragmentLeftCol.addConsoleMessage(msg);
                break;
        }
    }

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
            fragmentRightCol.btEnabled = true;
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder
                    .setMessage("This app requires Bluetooth to connect to the robot")
                    .setTitle("Alert");

            AlertDialog dialog = builder.create();
            dialog.show();

            fragmentRightCol.btEnabled = false;
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
            fragmentRightCol.getBtnConnect().setText(text.get(status));
            fragmentRightCol.getBtnConnect().setTextColor(Color.parseColor(col.get(status)));

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
        if (fragmentRightCol.btEnabled) {
            Intent intent = new Intent(MainActivity.this, BluetoothDevicesActivity.class);
            bluetoothService.start();
            activityLauncher.launch(intent);
        }
    }

    public void onResetClick(View view) {
        fragmentMap.reset();
        if (bluetoothService.state == STATE_CONNECTED)
            bluetoothService.write("reset".getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bluetoothService.stop();
    }
}




































