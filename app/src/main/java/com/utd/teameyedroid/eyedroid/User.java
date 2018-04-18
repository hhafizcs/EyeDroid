package com.utd.teameyedroid.eyedroid;

public class User {
    public String username;
    public String displayName;
    public String email;
    public String regToken;

    public User() {}

    public User (String username, String displayName, String email, String regToken) {
        this.username = username;
        this.displayName = displayName;
        this.email = email;
        this.regToken = regToken;
    }
}
