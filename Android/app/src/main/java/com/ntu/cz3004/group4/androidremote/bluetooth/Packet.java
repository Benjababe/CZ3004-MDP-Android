package com.ntu.cz3004.group4.androidremote.bluetooth;

import com.ntu.cz3004.group4.androidremote.arena.ObstacleInfo;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class Packet {
    int type;
    int x = -1;
    int y = -1;
    int obstacleID = -1;
    int direction = -1;
    ArrayList<JSONObject> ObstacleList;

    public Packet(int type) {
        this.type = type;
    }

    public String getJSONString() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("type", type);
        JSONObject value = new JSONObject();

        if (x != -1)
            value.put("x", x);
        if (y != -1)
            value.put("y", y);
        if (obstacleID != -1)
            value.put("obstacle_id", obstacleID);
        if (direction != -1)
            value.put("direction", direction);
        if (type == 9)
        {
            value.put("obstaclelist",ObstacleList);
        }
        json.put("value", value);

        return json.toString();
    }

    public byte[] getJSONBytes() {
        byte[] bytes = new byte[1];

        try {
            bytes = getJSONString().getBytes(StandardCharsets.UTF_8);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return bytes;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setObstacleID(int obstacleID) {
        this.obstacleID = obstacleID;
    }

    public void setDirection(int direction) {
        this.direction = direction;
    }

    public void setObstacleList(ArrayList<JSONObject> list1)
    {
        this.ObstacleList = list1;
    }
}
