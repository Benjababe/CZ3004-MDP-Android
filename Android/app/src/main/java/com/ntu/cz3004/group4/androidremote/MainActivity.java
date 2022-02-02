package com.ntu.cz3004.group4.androidremote;

import static android.view.DragEvent.ACTION_DRAG_ENTERED;
import static android.view.DragEvent.ACTION_DRAG_EXITED;
import static android.view.DragEvent.ACTION_DROP;

import static com.ntu.cz3004.group4.androidremote.bluetooth.BluetoothService.STATE_CONNECTED;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.FragmentManager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ClipData;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.ntu.cz3004.group4.androidremote.arena.ArenaButton;
import com.ntu.cz3004.group4.androidremote.arena.MyDragShadowBuilder;
import com.ntu.cz3004.group4.androidremote.arena.ObstacleImages;
import com.ntu.cz3004.group4.androidremote.arena.ObstacleInfo;
import com.ntu.cz3004.group4.androidremote.bluetooth.BluetoothDevicesActivity;
import com.ntu.cz3004.group4.androidremote.bluetooth.BluetoothListener;
import com.ntu.cz3004.group4.androidremote.bluetooth.BluetoothService;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@SuppressWarnings({"ConstantConditions", "IntegerDivisionInFloatingPointContext"})
public class MainActivity extends AppCompatActivity implements BluetoothListener, SensorEventListener {
    // 20x20 map variables
    int x, y, btnH, btnW, drawn = 0;
    int robotDrawable, robotRotation = 0;

    final String btAlert = "Connect via bluetooth before tampering with the map";

    // obstacleID: obstacleInfo obj
    HashMap<Integer, ObstacleInfo> obstacles = new HashMap<>();
    Drawable btnBG = null;

    private ActivityResultLauncher<Intent> activityLauncher;

    BluetoothAdapter adapter;
    BluetoothService bluetoothService;
    BluetoothDevice robotDev;
    String robotMACAddr = "";

    ConsoleFragment fragmentConsole;
    Button btnConnect;
    TextView txtConsole;
    ImageView imgRobot;
    ImageView imgRecog;
    TableLayout mapTable;
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

        // draws a 20x20 map for robot traversal when first rendered
        mapTable.getViewTreeObserver().addOnPreDrawListener(() -> {
            Log.d("DRAW", "Map Drawn");
            if (drawn < 1) {
                initMap(mapTable);
                drawn++;
            }
            return true;
        });
    }

    // finds all used views in UI
    private void initViews() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentConsole = (ConsoleFragment) fragmentManager.findFragmentById(R.id.fragmentConsole);

        btnConnect = findViewById(R.id.btnConnect);
        txtConsole = findViewById(R.id.txtConsole);
        imgRobot = findViewById(R.id.imgRobot);
        imgRecog = findViewById(R.id.imgRecog);
        mapTable = findViewById(R.id.mapTable);
        spawnGroup = findViewById(R.id.spawnGroup);
        switchTiltControl = findViewById(R.id.switchTiltControl);

        robotDrawable = R.drawable.img_robot;

        LinearLayout mainLayout = findViewById(R.id.main_layout);
        mainLayout.setOnDragListener(new OutOfBoundsDragListener());
    }

    private void initBT() {
        adapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothService = new BluetoothService(btMsgHandler);
        fragmentConsole.setBluetoothService(bluetoothService);

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
        ObstacleInfo obsInfo = obstacles.get(obstacleID);

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
                    //setBtConnected(true);
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

    private void initMap(TableLayout mapTable) {
        // set cell height and width
        btnH = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 25, getResources().getDisplayMetrics());
        btnW = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 25, getResources().getDisplayMetrics());

        // default background for cell
        btnBG = AppCompatResources.getDrawable(this, R.drawable.btn_background);

        // 20x20 map
        for (y = 19; y >= 0; y--) {
            TableRow row = new TableRow(this);
            row.setHorizontalGravity(Gravity.CENTER_HORIZONTAL);

            for (x = 0; x < 20; x++) {
                ArenaButton btn = new ArenaButton(this, x, y);
                btn.setId(View.generateViewId());
                btn.setPadding(1, 1, 1, 1);
                btn.setBackground(btnBG);
                btn.setLayoutParams(new TableRow.LayoutParams(btnW, btnH));
                btn.setTextColor(Color.rgb(255, 0, 0));

                btn.setOnClickListener(new MapBtnClickListener(x, y, btn.getId()));
                btn.setOnDragListener(new BtnDragListener(x, y, btn.getId()));
                row.addView(btn);
            }
            mapTable.addView(row);
        }
    }

    // returns specified cell to regular state
    private void emptyCell(ArenaButton btn, Drawable background) {
        int obstacleID = btn.obstacleID;

        // updates robot on obstacle removal
        ObstacleInfo obstacleInfo = obstacles.get(obstacleID);
        sendRemoveObstacle(obstacleInfo);

        obstacles.remove(obstacleID);
        btn.setText("");
        btn.obstacleID = -1;
        btn.setBackground(background);
    }

    // single click on cell to add/remove item
    private class MapBtnClickListener implements View.OnClickListener {
        int x, y, id;

        public MapBtnClickListener(int x, int y, int id) {
            this.x = x;
            this.y = y;
            this.id = id;
        }

        @Override
        public void onClick(View view) {
            // stops listener if not connected via bluetooth yet
            if (bluetoothService.state != STATE_CONNECTED) {
                Toast.makeText(MainActivity.this, btAlert, Toast.LENGTH_SHORT).show();
                return;
            }

            ArenaButton btn = findViewById(this.id);

            // removes item if it's on cell
            if (!btn.getText().equals("")) {
                emptyCell(btn, btnBG);
                return;
            }

            // get checked radiobutton value
            String spawn = getSpawn();

            // adds robot onto map
            if (spawn.equals(getResources().getString(R.string.btn_robot))) {
                spawnRobot(btn);
            }

            // adds obstacle otherwise
            if (spawn.equals(getResources().getString(R.string.btn_obstacle))) {
                for (int obsID = 1; obsID <= 400; obsID++) {
                    // finds next obstacle id
                    if (!obstacles.containsKey(obsID)) {
                        queryObstacleDirection(obsID, this.id);
                        break;
                    }
                }
            }
        }
    }

    // gets text value of radiobutton selected
    private String getSpawn() {
        int btnID = spawnGroup.getCheckedRadioButtonId();
        RadioButton btn = findViewById(btnID);
        return btn.getText().toString();
    }

    // spawns robot image on button position
    private void spawnRobot(ArenaButton btn) {
        int[] pt = new int[2];
        btn.getLocationInWindow(pt);

        imgRobot.setVisibility(View.VISIBLE);
        imgRobot.setImageResource(robotDrawable);

        // set robot drawing position to bottom left instead of top left
        imgRobot.setX(btn.getX());
        imgRobot.setY(pt[1] - dpToPixels(24) - dpToPixels(50));

        // rotates 90 degrees clockwise on click
        imgRobot.setOnClickListener(robot -> {
            robot.setPivotX(robot.getWidth() / 2);
            robot.setPivotY(robot.getHeight() / 2);

            robotRotation = (robotRotation + 90) % 360;
            robot.setRotation(robotRotation);
        });
    }


    private int dpToPixels(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
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
                    emptyCell(originalBtn, btnBG);
                    return true;
            }
            return true;
        }
    }


    // long click on cell to drag item to another cell
    private class BtnDragListener implements View.OnDragListener {
        int x, y, id;

        public BtnDragListener(int x, int y, int id) {
            this.x = x;
            this.y = y;
            this.id = id;
        }

        @Override
        public boolean onDrag(View newCell, DragEvent e) {
            switch (e.getAction()) {
                case ACTION_DRAG_ENTERED:
                case ACTION_DRAG_EXITED:
                    return true;

                case ACTION_DROP:
                    try {
                        // get drag data
                        ClipData dragData = e.getClipData();
                        JSONObject json = new JSONObject(dragData.getItemAt(0).getText().toString());

                        ArenaButton originalBtn = (ArenaButton) e.getLocalState();

                        // stops if dropped cell is same as original cell
                        if (originalBtn.getId() == newCell.getId())
                            return true;

                        // removes original cell
                        emptyCell(originalBtn, btnBG);

                        // moves obstacle over to new cell
                        int obstacleID = json.getInt("obstacleID");
                        String dirSelected = json.getString("dirSelected");
                        addObstacle(obstacleID, newCell.getId(), dirSelected);
                    }

                    catch (JSONException ex) {
                        ex.printStackTrace();
                    }
                    return true;
            }
            return true;
        }
    }

    private void queryObstacleDirection(int obstacleID, int btnID) {
        // 4 choices of directions
        final String[] directions = {"Top", "Left", "Bottom", "Right"};

        // direction default to Top
        final String[] dirSelected = {"Top"};

        // retrieves direction of image on obstacle through radiobuttons
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose direction of image");
        builder.setSingleChoiceItems(directions, 0, (dialogInterface, i) -> dirSelected[0] = directions[i]);

        // confirm to add obstacle
        builder.setPositiveButton("Confirm", (dialogInterface, i) -> {
            addObstacle(obstacleID, btnID, dirSelected[0]);
            dialogInterface.dismiss();
        });

        // exit process
        builder.setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss());

        builder.show();
    }

    public void addObstacle(int obstacleID, int btnID, String dirSelected) {
        // direction default to top
        int borderID = R.drawable.top_border;

        // get drawable ID for image direction
        switch(dirSelected) {
            case "Right":
                borderID = R.drawable.right_border;
                break;
            case "Bottom":
                borderID = R.drawable.bottom_border;
                break;
            case "Left":
                borderID = R.drawable.left_border;
                break;
        }

        // add obstacle id to cell
        ArenaButton btn = findViewById(btnID);
        btn.setText(String.valueOf(obstacleID));
        btn.obstacleID = obstacleID;

        // keeps track of obstacle in memory
        ObstacleInfo obstacleInfo = new ObstacleInfo(obstacleID, btnID, btn.x, btn.y, dirSelected);
        obstacles.put(obstacleID, obstacleInfo);

        // sends addition of obstacle over to robot
        sendAddObstacleData(obstacleInfo);

        // draws direction of image onto cell
        Drawable border = AppCompatResources.getDrawable(this, borderID);
        btn.setBackground(border);

        // set drag listener to move cell item
        btn.setOnLongClickListener(view -> {
            try {
                // we can to keep track of:
                // original button id, obstacle id and direction of image
                JSONObject json = new JSONObject();
                json.put("obstacleID", obstacleID);
                json.put("dirSelected", dirSelected);

                // store data in ClipItem which stays until drag is stopped
                ClipData.Item item = new ClipData.Item(json.toString());
                ClipData dragData = new ClipData(
                        "dragObstacle",
                        new String[]{},
                        item
                );

                // shadow will just be the button itself
                View.DragShadowBuilder shadow = new MyDragShadowBuilder(btn);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    view.startDragAndDrop(dragData, shadow, btn, 0);
                // deprecated method, for my old ass tablet
                else
                    view.startDrag(dragData, shadow, btn, 0);

            } catch (JSONException ex) {
                ex.printStackTrace();
            }

            return true;
        });
    }

    private void sendAddObstacleData(ObstacleInfo obstacleInfo) {
        if (bluetoothService.state == STATE_CONNECTED)
            bluetoothService.write(obstacleInfo.toAddBytes());
        else
            Toast.makeText(this, btAlert, Toast.LENGTH_LONG).show();
    }

    private void sendRemoveObstacle(ObstacleInfo obstacleInfo) {
        if (bluetoothService.state == STATE_CONNECTED)
            bluetoothService.write(obstacleInfo.toRemoveBytes());
        else
            Toast.makeText(this, btAlert, Toast.LENGTH_LONG).show();
    }

    // Clears all cells back to default state
    public void Reset(View view) {
        Set<Integer> keys = obstacles.keySet();

        for (int key: keys) {
            ObstacleInfo obstacleInfo = obstacles.get(key);
            int btnID = obstacleInfo.btnID;
            ArenaButton btn = findViewById(btnID);

            btn.setText("");
            btn.obstacleID = -1;
            btn.setBackground(btnBG);
            btn.setOnLongClickListener(null);

            // updates robot on obstacle removal
            sendRemoveObstacle(obstacleInfo);
        }

        obstacles.clear();
        imgRobot.setVisibility(View.GONE);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bluetoothService.stop();
    }
}




































