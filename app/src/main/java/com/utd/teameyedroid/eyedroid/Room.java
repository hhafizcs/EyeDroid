package com.utd.teameyedroid.eyedroid;

public class Room {
    public String roomName;
    public String pinUsername;
    public String helperUsername;
    public String cnxType;
    public boolean connected;

    public Room() {}

    public Room (String roomName, String pinUsername, String cnxType) {
        this.roomName = roomName;
        this.pinUsername = pinUsername;
        this.cnxType = cnxType;

        helperUsername = "";
        connected = false;
    }
}
