package com.ntu.cz3004.group4.androidremote.arena;

import static com.ntu.cz3004.group4.androidremote.Constants.A_ADD_OBSTACLE;
import static com.ntu.cz3004.group4.androidremote.Constants.A_REM_OBSTACLE;

import com.ntu.cz3004.group4.androidremote.bluetooth.Packet;

import org.json.JSONException;

import java.nio.charset.StandardCharsets;

public class ObstacleInfo {
    public int x, y, dir, btnID, obstacleID;

    public ObstacleInfo(int obstacleID, int btnID, int x, int y, int dirSelected) {
        this.obstacleID = obstacleID;
        this.btnID = btnID;
        this.x = x;
        this.y = y;
        this.dir = dirSelected;
    }

    // for sending obstacle addition to robot
    public byte[] toAddBytes() {
        String res = "";
        try {
            Packet packet = new Packet(A_ADD_OBSTACLE);
            packet.setX(x);
            packet.setY(y);
            packet.setObstacleID(obstacleID);
            packet.setDirection(dir);
            res = packet.getJSONString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return res.getBytes(StandardCharsets.UTF_8);
    }

    // for sending obstacle removal to robot
    public byte[] toRemoveBytes() {
        String res = "";
        try {
            Packet packet = new Packet(A_REM_OBSTACLE);
            packet.setX(x);
            packet.setY(y);
            packet.setObstacleID(obstacleID);
            res = packet.getJSONString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return res.getBytes(StandardCharsets.UTF_8);
    }
}
