package com.utd.teameyedroid.eyedroid;

public class Room {
    public String roomName;
    public String pinDisplayName;
    public String pinUserName;
    public String helperDisplayName;
    public String helperUserName;
    public String cnxType;
    public String dateTime;
    public String contactEmail;
    public boolean connected;
    public boolean notificationSent;

    public Room() {}

    public Room (String roomName, String pinDisplayName, String cnxType, String dateTime, String pinUserName, String contactEmail) {
        this.roomName = roomName;
        this.pinDisplayName = pinDisplayName;
        this.pinUserName = pinUserName;
        this.cnxType = cnxType;
        this.dateTime = dateTime;
        this.contactEmail = contactEmail;

        helperDisplayName = "";
        helperUserName = "";
        connected = false;
        notificationSent = false;
    }
}
