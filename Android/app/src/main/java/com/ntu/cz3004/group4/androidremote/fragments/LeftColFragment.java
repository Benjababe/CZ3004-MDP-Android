package com.ntu.cz3004.group4.androidremote.fragments;

import static com.ntu.cz3004.group4.androidremote.Constants.A_MOVE_BACKWARD;
import static com.ntu.cz3004.group4.androidremote.Constants.A_MOVE_FORWARD;
import static com.ntu.cz3004.group4.androidremote.bluetooth.BluetoothService.STATE_CONNECTED;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.ntu.cz3004.group4.androidremote.R;
import com.ntu.cz3004.group4.androidremote.bluetooth.BluetoothService;
import com.ntu.cz3004.group4.androidremote.bluetooth.Packet;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.VLCVideoLayout;

public class LeftColFragment extends Fragment {
    ListView lvConsole;
    TextView txtRoboStatus, txtRoboDirection, txtRoboPosition;
    ImageButton btnAccel, btnReverse;
    LibVLC libVLC;
    MediaPlayer mediaPlayer;
    VLCVideoLayout vidRPI;
    Button btnVid;

    ArrayAdapter<String> consoleArrayAdapter;

    BluetoothService bluetoothService;

    static final String testURL = "rtsp://wowzaec2demo.streamlock.net/vod/mp4:BigBuckBunny_115k.mp4";
    static final String url = "rtsp://192.168.4.4:8554/unicast";

    boolean vidToggle = false;

    public LeftColFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_left_col, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        lvConsole = view.findViewById(R.id.lvConsole);
        txtRoboStatus = view.findViewById(R.id.txtRoboStatus);
        txtRoboDirection = view.findViewById(R.id.txtRoboDirection);
        txtRoboPosition = view.findViewById(R.id.txtRoboPosition);
        btnAccel = view.findViewById(R.id.btnAccel);
        btnReverse = view.findViewById(R.id.btnReverse);
        vidRPI = view.findViewById(R.id.vidRPI);
        btnVid = view.findViewById(R.id.btnVid);

        btnAccel.setOnClickListener(this::accelerate);
        btnReverse.setOnClickListener(this::reverse);
        btnVid.setOnClickListener(this::toggleVideo);

        consoleArrayAdapter = new ArrayAdapter<>(getContext(), R.layout.item_message);
        lvConsole.setAdapter(consoleArrayAdapter);
        lvConsole.setSelection(consoleArrayAdapter.getCount() - 1);

        libVLC = new LibVLC(this.getContext());
        mediaPlayer = new MediaPlayer(libVLC);
    }

    @Override
    public void onStart() {
        super.onStart();

        mediaPlayer.attachViews(vidRPI, null, false, false);

        Media media = new Media(libVLC, Uri.parse(url));
        media.setHWDecoderEnabled(true, false);

        mediaPlayer.setMedia(media);
        media.release();
    }

    public void toggleVideo(View view) {
        vidToggle = !vidToggle;

        if (vidToggle)
            mediaPlayer.play();
        else
            mediaPlayer.pause();
    }

    public void accelerate(View view) {
        if (bluetoothService.state == STATE_CONNECTED) {
            Packet packet = new Packet(A_MOVE_FORWARD);
            bluetoothService.write(packet.getJSONBytes());
        }
    }

    public void reverse(View view) {
        if (bluetoothService.state == STATE_CONNECTED) {
            Packet packet = new Packet(A_MOVE_BACKWARD);
            bluetoothService.write(packet.getJSONBytes());
        }
    }


    public void setBluetoothService(BluetoothService bluetoothService) {
        this.bluetoothService = bluetoothService;
    }

    public void addConsoleMessage(String message) {
        consoleArrayAdapter.add(message);
    }

    public void setRoboStatus(String status) {
        txtRoboStatus.setText(status);
    }

    public void setRoboDirection(String direction) {
        txtRoboDirection.setText(direction);
    }

    public void setRobotPosition(String position) {
        txtRoboPosition.setText(position);
    }
}
