package com.utd.teameyedroid.eyedroid;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class UsageActivity extends AppCompatActivity {

    private SharedPreferences preferences;
    private SharedPreferences.Editor prefEditor;
    private Button topButton;
    private Button bottomButton;

    private static final String[] mPermissions = new String[] {
            android.Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usage);

        List<String> permissionsNeeded = new ArrayList<>();
        for (String permission : mPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED)
                permissionsNeeded.add(permission);
        }

        if (permissionsNeeded.size() > 0) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), 1988);
        }

        //String storedPreference = preferences.getString("usage", "notSet");

        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        topButton = findViewById(R.id.topButton);
        bottomButton = findViewById(R.id.bottomButton);

        Bundle bundle = getIntent().getExtras();
        if(bundle != null) {
            String chatEnded = bundle.getString("chatEnded");
            if(chatEnded != null && chatEnded.equals("volunteerExited")) {
                Toast.makeText(this, "The volunteer has left the chat.", Toast.LENGTH_LONG).show();
            }

            int level = bundle.getInt("level");
            if(level == 0) {
                setButtonsTextAndTag(1);
            } else {
                setButtonsTextAndTag(level);
            }
        } else {
            setButtonsTextAndTag(1);
        }
    }

    public void topButtonClicked (View v) {
        switch (v.getTag().toString()) {
            case "0":
                //Use As Person In Need Of Help Clicked
                prefEditor = preferences.edit();
                prefEditor.putString("usage", "pin");
                prefEditor.commit();

                setButtonsTextAndTag(2);

                break;
            case "2":
                //Call Personal Contact Clicked
                break;
        }
    }

    public void bottomButtonClicked (View v) {
        switch (v.getTag().toString()) {
            case "1":
                //Use As Volunteer Clicked
                prefEditor = preferences.edit();
                prefEditor.putString("usage", "volunteer");
                prefEditor.commit();

                startActivity(new Intent(this, PinListActivity.class));

                break;
            case "3":
                //Call Volunteer Clicked
                Intent intent = new Intent(UsageActivity.this, MainActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("cnxType", "pinToVolunteer");
                intent.putExtras(bundle);
                startActivity(intent);
                finish();

                break;
        }
    }

    private void setButtonsTextAndTag (int level) {
        switch (level) {
            case 1:
                topButton.setText("Use As Person In Need Of Help");
                topButton.setTag("0");
                bottomButton.setText("Use As Volunteer");
                bottomButton.setTag("1");
                break;
            case 2:
                topButton.setText("Call Personal Contact");
                topButton.setTag("2");
                bottomButton.setText("Call Volunteer");
                bottomButton.setTag("3");
                break;
        }
    }
}