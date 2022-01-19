package com.ntu.cz3004.group4.androidremote.arena;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class ObstacleInfo {
    public int x, y, dir, btnID, obstacleID;

    public ObstacleInfo(int obstacleID, int btnID, int x, int y, String dirSelected) {
        this.obstacleID = obstacleID;
        this.btnID = btnID;
        this.x = x;
        this.y = y;

        ArrayList<String> dirs = new ArrayList<>(Arrays.asList("Top", "Right", "Bottom", "Left"));
        this.dir = dirs.indexOf(dirSelected);
    }

    // for sending obstacle addition to robot
    public byte[] toAddBytes() {
        String res = String.format(Locale.ENGLISH, "ADDOBS %d %d,%d,%d", obstacleID, x, y, dir);
        return res.getBytes(StandardCharsets.UTF_8);
    }

    // for sending obstacle removal to robot
    public byte[] toRemoveBytes() {
        String res = String.format(Locale.ENGLISH, "REMOBS %d", obstacleID);
        return res.getBytes(StandardCharsets.UTF_8);
    }
}
