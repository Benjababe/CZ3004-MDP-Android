package com.ntu.cz3004.group4.androidremote;

public interface Constants {
    // Message types sent from the BluetoothChatService Handler
    int MESSAGE_STATE_CHANGE = 1;
    int MESSAGE_READ = 2;
    int MESSAGE_WRITE = 3;
    int MESSAGE_DEVICE_NAME = 4;
    int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    String DEVICE_NAME = "device_name";
    String TOAST = "toast";

    // robot facing directions
    int NORTH = 0;
    int EAST = 1;
    int SOUTH = 2;
    int WEST = 3;

    // RPI message types
    int MOVE_FORWARD        = 1;    // [UNIT]
    int MOVE_BACKWARD       = 2;    //[UNIT]
    int TURN_LEFT           = 3;
    int TURN_RIGHT          = 4;
    int ADD_OBSTACLE        = 5;    //[IMAGE_ID] [DIRECTION] [X] [Y]
    int REMOVE_OBSTACLE     = 6;    //[IMAGE_ID] / [X] [Y]
    int UPDATE              = 7;    //[ROBOT_X], [ROBOT_Y]
    int LOG                 = 8;    //[MESSAGE]

    // Android message types
    int A_MOVE_FORWARD = 1;
    int A_MOVE_BACKWARD = 2;
    int A_MOVE_LEFT = 3;
    int A_MOVE_RIGHT = 4;
    int A_ADD_OBSTACLE = 5;
    int A_REM_OBSTACLE = 6;
    int A_ROBOT_POS = 7;
    int A_RESET = 8;
    int A_IMG_REC = 9;
    int A_FASTEST_PATH = 10;
}
