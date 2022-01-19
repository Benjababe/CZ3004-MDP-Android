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
import androidx.fragment.app.FragmentManager;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.ntu.cz3004.group4.androidremote.arena.ArenaButton;
import com.ntu.cz3004.group4.androidremote.arena.ObstacleInfo;
import com.ntu.cz3004.group4.androidremote.bluetooth.BluetoothDevicesActivity;
import com.ntu.cz3004.group4.androidremote.bluetooth.BluetoothService;
import com.ntu.cz3004.group4.androidremote.arena.Arena;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Set;


@SuppressWarnings({"ConstantConditions"})
public class MainActivity extends AppCompatActivity {
    // 20x20 map variables
    int x, y, btnH, btnW, drawn = 0;

    final String btAlert = "Connect via bluetooth before tampering with the map";

    HashMap<Integer, ObstacleInfo> obstacles = new HashMap<>();
    Drawable btnBG = null;

    private ActivityResultLauncher<Intent> activityLauncher;

    BluetoothService bluetoothService;
    boolean btConnected = false;

    ConsoleFragment fragmentConsole;
    Button btnConnect;
    TextView txtConsole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtConsole = findViewById(R.id.txtConsole);

        initBT();

        // draws a 20x20 map for robot traversal when first rendered
        TableLayout mapTable = findViewById(R.id.mapTable);
        mapTable.getViewTreeObserver().addOnPreDrawListener(() -> {
            Log.d("DRAW", "Map Drawn");
            if (drawn < 1) {
                initMap(mapTable);
                drawn++;
            }
            return true;
        });
    }

    private void initBT() {
        btnConnect = findViewById(R.id.btnConnect);

        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentConsole = (ConsoleFragment) fragmentManager.findFragmentById(R.id.fragmentConsole);

        bluetoothService = new BluetoothService(MainActivity.this, fragmentConsole.getHandler());
        fragmentConsole.setBluetoothService(bluetoothService);

        promptBTPermissions();
        listenBTFragment();
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
                String address = intentBundle.getString("bluetooth_address");

                // connects with the selected device from BT intent
                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                BluetoothDevice device = adapter.getRemoteDevice(address);
                bluetoothService.connect(device);

                // updates UI on connection
                if (bluetoothService.state == STATE_CONNECTED) {
                    fragmentConsole.setBluetoothService(bluetoothService);
                    //setBtConnected(true);
                }
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
        for (y = 0; y < 20; y++) {
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
        int obstacleID = Integer.parseInt(btn.getText().toString());

        // updates robot on obstacle removal
        ObstacleInfo obstacleInfo = obstacles.get(obstacleID);
        sendRemoveObstacle(obstacleInfo);

        obstacles.remove(obstacleID);
        btn.setText("");
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
                Log.d("Robo", "Spawning robot");
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
        RadioGroup spawnGroup = findViewById(R.id.spawnGroup);
        int btnID = spawnGroup.getCheckedRadioButtonId();
        RadioButton btn = findViewById(btnID);
        return btn.getText().toString();
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
                View.DragShadowBuilder shadow = new Arena.MyDragShadowBuilder(btn);

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
            btn.setBackground(btnBG);
            btn.setOnLongClickListener(null);

            // updates robot on obstacle removal
            sendRemoveObstacle(obstacleInfo);
        }

        obstacles.clear();
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
}




































