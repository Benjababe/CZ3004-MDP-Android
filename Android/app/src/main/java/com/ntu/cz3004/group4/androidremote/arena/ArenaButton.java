package com.ntu.cz3004.group4.androidremote.arena;

import android.content.Context;

public class ArenaButton extends androidx.appcompat.widget.AppCompatButton {
    public int x, y, obstacleID = -1;

    public ArenaButton(Context context) {
        super(context);
    }

    public ArenaButton(Context context, int x, int y) {
        super(context);
        this.x = x;
        this.y = y;
    }
}
