package com.utd.teameyedroid.eyedroid;

public class Room {
    public String roomName;
    public String pinDisplayName;
    public String pinUserName;
    public String helperDisplayName;
    public String cnxType;
    public String dateTime;
    public boolean connected;

    public Room() {}

    public Room (String roomName, String pinDisplayName, String cnxType, String dateTime, String pinUserName) {
        this.roomName = roomName;
        this.pinDisplayName = pinDisplayName;
        this.cnxType = cnxType;
        this.dateTime = dateTime;
        this.pinUserName = pinUserName;

        helperDisplayName = "";
        connected = false;
    }
}
