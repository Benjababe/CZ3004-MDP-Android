package com.ntu.cz3004.group4.androidremote.arena;

import com.ntu.cz3004.group4.androidremote.R;

public class ObstacleImages {
    // returns drawable id of corresponding recognised image
    public static int getDrawableID(String imgName) {
        switch (imgName) {
            case "arrow_left":
                return R.drawable.img_arrow_left;
            case "arrow_right":
                return R.drawable.img_arrow_right;
            case "arrow_up": 
                return R.drawable.img_arrow_up;
            case "arrow_down":
                return R.drawable.img_arrow_down;
            case "circle":
                return R.drawable.img_circle;
            case "1":
			    return R.drawable.img_1;
            case "2":
				return R.drawable.img_2;
            case "3":
				return R.drawable.img_3;
            case "4":
				return R.drawable.img_4;
            case "5":
				return R.drawable.img_5;
            case "6":
				return R.drawable.img_6;
            case "7":
				return R.drawable.img_7;
            case "8":
				return R.drawable.img_8;
            case "9":
				return R.drawable.img_9;
            case "a":
				return R.drawable.img_a;
            case "b":
				return R.drawable.img_b;
            case "c":
				return R.drawable.img_c;
            case "d":
				return R.drawable.img_d;
            case "e":
				return R.drawable.img_e;
            case "f":
				return R.drawable.img_f;
            case "g":
				return R.drawable.img_g;
            case "h":
				return R.drawable.img_h;
            case "s":
				return R.drawable.img_s;
            case "t":
				return R.drawable.img_t;
            case "u":
				return R.drawable.img_u;
            case "v":
				return R.drawable.img_v;
            case "w":
				return R.drawable.img_w;
            case "x":
				return R.drawable.img_x;
            case "y":
				return R.drawable.img_y;
            case "z":
				return R.drawable.img_z;
            default:
                return -1;
        }
    }
}
