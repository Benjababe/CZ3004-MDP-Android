package com.ntu.cz3004.group4.androidremote.arena;

import com.ntu.cz3004.group4.androidremote.R;

public class ObstacleImages {
    // returns drawable id of corresponding recognised image
    public static int getDrawableID(int imageID) {
        switch (imageID) {
            case 1:
                return R.drawable.ic_bullseye;
            case 11:
			    return R.drawable.img_1;
            case 12:
				return R.drawable.img_2;
            case 13:
				return R.drawable.img_3;
            case 14:
				return R.drawable.img_4;
            case 15:
				return R.drawable.img_5;
            case 16:
				return R.drawable.img_6;
            case 17:
				return R.drawable.img_7;
            case 18:
				return R.drawable.img_8;
            case 19:
				return R.drawable.img_9;
            case 20:
				return R.drawable.img_a;
            case 21:
				return R.drawable.img_b;
            case 22:
				return R.drawable.img_c;
            case 23:
				return R.drawable.img_d;
            case 24:
				return R.drawable.img_e;
            case 25:
				return R.drawable.img_f;
            case 26:
				return R.drawable.img_g;
            case 27:
				return R.drawable.img_h;
            case 28:
				return R.drawable.img_s;
            case 29:
				return R.drawable.img_t;
            case 30:
				return R.drawable.img_u;
            case 31:
				return R.drawable.img_v;
            case 32:
				return R.drawable.img_w;
            case 33:
				return R.drawable.img_x;
            case 34:
				return R.drawable.img_y;
            case 35:
				return R.drawable.img_z;
            case 36:
                return R.drawable.img_arrow_up;
            case 37:
                return R.drawable.img_arrow_down;
            case 38:
                return R.drawable.img_arrow_right;
            case 39:
                return R.drawable.img_arrow_left;
            case 40:
                return R.drawable.img_circle;
            default:
                return -1;
        }
    }
}
