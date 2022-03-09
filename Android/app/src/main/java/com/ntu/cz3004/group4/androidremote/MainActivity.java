package com.ntu.cz3004.group4.androidremote;

import static android.view.DragEvent.ACTION_DRAG_ENTERED;
import static android.view.DragEvent.ACTION_DRAG_EXITED;
import static android.view.DragEvent.ACTION_DROP;
import static com.ntu.cz3004.group4.androidremote.Constants.ADD_OBSTACLE;
import static com.ntu.cz3004.group4.androidremote.Constants.A_RESET;
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
import com.ntu.cz3004.group4.androidremote.bluetooth.Packet;
import com.ntu.cz3004.group4.androidremote.fragments.ConsoleFragment;
import com.ntu.cz3004.group4.androidremote.fragments.LeftColFragment;
import com.ntu.cz3004.group4.androidremote.fragments.MapFragment;
import com.ntu.cz3004.group4.androidremote.fragments.RightColFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;


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
        fragmentRightCol.setFragmentMap(fragmentMap);
        fragmentMap.setLeftColFragment(fragmentLeftCol);
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

            try {
                JSONObject json = new JSONObject(strMessage);
                rpiMessageHandler(json);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    });


    private void rpiMessageHandler(JSONObject json) throws JSONException {
        JSONObject val = (json.has("value")) ? json.getJSONObject("value") : null;
        int x, y, imageID, direction;

        switch (json.getInt("type")) {
            case MOVE_FORWARD:
                fragmentLeftCol.setRoboStatus("MOVE FORWARD");
                int t = val.getInt("time");
                for(int i =0;i<t;i++)
                {
                    fragmentMap.moveRobot(true);
                    fragmentLeftCol.setRobotPosition(fragmentMap.getPositionString());
                }
                //fragmentMap.moveRobot(true);
                //fragmentLeftCol.setRobotPosition(fragmentMap.getPositionString());
                break;

            case MOVE_BACKWARD:
                fragmentLeftCol.setRoboStatus("MOVE BACKWARD");
                fragmentMap.moveRobot(false);
                fragmentLeftCol.setRobotPosition(fragmentMap.getPositionString());
                break;

            case TURN_LEFT:
                fragmentLeftCol.setRoboStatus("TURN LEFT");
                fragmentMap.rotateRobot(null, -90);
                fragmentLeftCol.setRoboDirection(fragmentMap.getDirectionString());
                break;

            case TURN_RIGHT:
                fragmentLeftCol.setRoboStatus("TURN RIGHT");
                fragmentMap.rotateRobot(null, 90);
                fragmentLeftCol.setRoboDirection(fragmentMap.getDirectionString());
                break;

            case ADD_OBSTACLE:
                fragmentLeftCol.setRoboStatus("ADD OBSTACLE");
                x = val.getInt("x");
                y = val.getInt("y");
                if(x < 0) x= 0;
                if(x > 19) x =19;
                if(y < 0) y = 0;
                if(y > 19) y = 19;
                imageID = val.getInt("image_id");
                // direction = val.getInt("DIRECTION");
                drawObstacleImg(x, y, imageID);
                break;

            case REMOVE_OBSTACLE:
                fragmentLeftCol.setRoboStatus("REMOVE OBSTACLE");
                imageID = val.getInt("image_id");
                fragmentMap.emptyCellObsID(imageID);
                break;

            case UPDATE:
                fragmentLeftCol.setRoboStatus("Updating Robot");
                x = val.getInt("x");
                y = val.getInt("y");
                if(x < 0) x= 0;
                if(x > 19) x =19;
                if(y < 0) y = 0;
                if(y > 19) y = 19;
                direction = val.getInt("direction");
                fragmentMap.setRobotDirection(direction);
                fragmentMap.setRobotXY(x, y);
                fragmentLeftCol.setRoboDirection(fragmentMap.getDirectionString());
                fragmentLeftCol.setRobotPosition(fragmentMap.getPositionString());
                break;

            case LOG:
                String msg = val.getString("MESSAGE");
                fragmentLeftCol.addConsoleMessage(msg);
                break;
        }
    }

    private void drawObstacleImg(int x, int y, int imageID) {
        // gets image to be drawn
        Drawable imgDrawable = getImgDrawable(imageID);

        int obstacleID = fragmentMap.getObstacleIDByCoord(x, y);

        if (obstacleID == -1)
            return;

        // gets corresponding obstacle
        ObstacleInfo obsInfo = fragmentMap.getObstacles().get(obstacleID);

        // draws recognised image onto obstacle block
        ArenaButton btn = findViewById(obsInfo.btnID);
        imgDrawable.setBounds(2,2,btn.getWidth() - 2,btn.getHeight() - 2);
        btn.getOverlay().add(imgDrawable);
        btn.setText("");
        btn.setTextColor(Color.parseColor("#FFFFFFFF"));
    }


    // gets drawable object of recognised image to draw onto arena cell
    private Drawable getImgDrawable(int imageID) {
        int drawableID = ObstacleImages.getDrawableID(imageID);
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
            if (result.getResultCode() == Activity.RESULT_OK) {
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
        if (bluetoothService.state == STATE_CONNECTED) {
            try {
                Packet packet = new Packet(A_RESET);
                bluetoothService.write(packet.getJSONString().getBytes(StandardCharsets.UTF_8));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        bluetoothService.stop();
    }
}




































