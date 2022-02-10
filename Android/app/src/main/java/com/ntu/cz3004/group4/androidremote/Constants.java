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

    //RPI messageTypes
    int MOVE_FORWARD        = 1;    // [UNIT]
    int MOVE_BACKWARD       = 2;    //[UNIT]
    int TURN_LEFT           = 3;
    int TURN_RIGHT          = 4;
    // Direction
    int NORTH = 0;
    int EAST = 1;
    int SOUTH = 2;
    int WEST = 3;
    //Setting up Known Area
    int ADD_OBSTACLE        = 5;    //[IMAGE_ID] [DIRECTION] [X] [Y]
    int REMOVE_OBSTACLE     = 6;    //[IMAGE_ID] / [X] [Y]
    
    //Misc
    int UPDATE              = 7;    //[ROBOT_X], [ROBOT_Y]
    int LOG                 = 8;    //[MESSAGE]
}
