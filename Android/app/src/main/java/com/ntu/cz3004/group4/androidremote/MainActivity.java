package com.ntu.cz3004.group4.androidremote;

import static android.view.DragEvent.ACTION_DRAG_ENTERED;
import static android.view.DragEvent.ACTION_DRAG_EXITED;
import static android.view.DragEvent.ACTION_DROP;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;

import android.bluetooth.BluetoothAdapter;
import android.content.ClipData;
import android.content.Intent;
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
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.Toast;

import com.ntu.cz3004.group4.androidremote.map.Map;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Set;


@SuppressWarnings({"ConstantConditions", "deprecation"})
public class MainActivity extends AppCompatActivity {
    // 20x20 map variables
    int x, y, btnH, btnW;
    HashMap<Integer, Integer> obstacles = new HashMap<>();
    Drawable btnBG = null;

    BluetoothAdapter btAdapter;
    boolean btConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnConnect = findViewById(R.id.btnConnect);
        btnConnect.setText(btConnected ? "Connected" : "Not Connected");
        btnConnect.setTextColor(Color.parseColor(btConnected ? "#FFFFFFFF" : "#FFFF0000"));

        // draws a 20x20 map for robot traversal
        initMap();
    }

    private void initMap() {
        // set cell height and width
        btnH = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 25, getResources().getDisplayMetrics());
        btnW = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 25, getResources().getDisplayMetrics());

        // default background for cell
        btnBG = AppCompatResources.getDrawable(this, R.drawable.btn_background);
        TableLayout mapTable = findViewById(R.id.mapTable);

        // 20x20 map
        for (y = 0; y < 20; y++) {
            TableRow row = new TableRow(this);
            row.setHorizontalGravity(Gravity.CENTER_HORIZONTAL);

            for (x = 0; x < 20; x++) {
                Button btn = new Button(this);
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
    private void emptyCell(Button btn, Drawable background) {
        int val = Integer.parseInt(btn.getText().toString());
        obstacles.remove(val);
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
            Button btn = findViewById(this.id);

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

                        Button originalBtn = (Button) e.getLocalState();

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

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose direction of image");
        builder.setSingleChoiceItems(directions, 0, (dialogInterface, i) -> dirSelected[0] = directions[i]);

        builder.setPositiveButton("Confirm", (dialogInterface, i) -> {
            addObstacle(obstacleID, btnID, dirSelected[0]);
            dialogInterface.dismiss();
        });

        builder.setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss());

        builder.show();
    }

    public void addObstacle(int obstacleID, int btnID, String dirSelected) {
        // direction default to top
        int borderID = R.drawable.top_border;

        // get drawable ID for image direction
        switch(dirSelected) {
            case "Bottom":
                borderID = R.drawable.bottom_border;
                break;

            case "Left":
                borderID = R.drawable.left_border;
                break;

            case "Right":
                borderID = R.drawable.right_border;
                break;
        }

        // add obstacle id to cell
        obstacles.put(obstacleID, btnID);
        Button btn = findViewById(btnID);
        btn.setText(String.valueOf(obstacleID));

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
                View.DragShadowBuilder shadow = new Map.MyDragShadowBuilder(btn);

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

    // Clears all cells back to default state
    public void Reset(View view) {
        Set<Integer> keys = obstacles.keySet();

        for (int key: keys) {
            int buttonID = obstacles.get(key);
            Button btn = findViewById(buttonID);

            btn.setText("");
            btn.setBackground(btnBG);
            btn.setOnLongClickListener(null);
        }

        obstacles.clear();
    }

    private void setupBluetooth() {
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null){
            int duration = Toast.LENGTH_SHORT;
            Toast.makeText(this, "No Bluetooth on this device", duration).show();
        }

        if (!btAdapter.isEnabled()){
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 0xDEADBEEF);
        }
    }

    public void onBtnConnectClick(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Bluetooth");

        final View layout = getLayoutInflater().inflate(R.layout.bluetooth_menu, null);
        builder.setView(layout);

        ListView btListView = findViewById(R.id.btListView);
        setupBluetooth();
        btListView.isOpaque();

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}




































