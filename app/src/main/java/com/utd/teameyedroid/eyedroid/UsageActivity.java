package com.utd.teameyedroid.eyedroid;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class UsageActivity extends AppCompatActivity {

    private SharedPreferences preferences;
    private SharedPreferences.Editor prefEditor;
    private Button topButton;
    private Button bottomButton;
    private int level = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usage);

        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        topButton = findViewById(R.id.topButton);
        bottomButton = findViewById(R.id.bottomButton);
    }

    @Override
    protected void onStart() {
        super.onStart();

        initialize();
    }

    private void initialize () {
        Bundle bundle = getIntent().getExtras();
        if(bundle != null) {
            String chatEnded = bundle.getString("chatEnded");
            if(chatEnded != null) {
                switch (chatEnded) {
                    case "volunteerExited":
                        Toast.makeText(this, "The volunteer has left the chat.", Toast.LENGTH_LONG).show();
                        break;
                    case "pinExited":
                        Toast.makeText(this, "The person in need has left the chat.", Toast.LENGTH_LONG).show();
                        break;
                    case "couldNotConnect":
                        Toast.makeText(this, "Could not make a connection.", Toast.LENGTH_LONG).show();
                        break;
                }
            }

            int wantedLevel = bundle.getInt("level");
            if(wantedLevel == 0) {
                setButtonsTextAndTag(1);
            } else {
                setButtonsTextAndTag(wantedLevel);
            }
        } else {
            setButtonsTextAndTag(1);
        }
    }

    public void topButtonClicked (View v) {
        switch (v.getTag().toString()) {
            case "0":
                //use as pin clicked
                prefEditor = preferences.edit();
                prefEditor.putString("usage", "pin");
                prefEditor.commit();
                setButtonsTextAndTag(2);
                break;
            case "2":
                //make a call clicked
                setButtonsTextAndTag(3);
                break;
            case "4":
                //show list of pins clicked
                startActivity(new Intent(this, PinListActivity.class));
                break;
            case "6":
                //call personal contact clicked
                break;
        }
    }

    public void bottomButtonClicked (View v) {
        switch (v.getTag().toString()) {
            case "1":
                //use as volunteer clicked
                prefEditor = preferences.edit();
                prefEditor.putString("usage", "volunteer");
                prefEditor.commit();
                setButtonsTextAndTag(2);
                break;
            case "3":
                //pin settings clicked
                break;
            case "5":
                //volunteer settings clicked
                break;
            case "7":
                //call volunteer clicked
                Intent intent = new Intent(UsageActivity.this, MainActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("cnxType", "pinToVolunteer");
                intent.putExtras(bundle);
                startActivity(intent);
                finish();
                break;
        }
    }

    private void setButtonsTextAndTag (int wantedLevel) {
        String usage = preferences.getString("usage", "notSet");

        switch (wantedLevel) {
            case 1:
                switch (usage) {
                    case "notSet":
                        topButton.setText("Use As Person In Need Of Help");
                        topButton.setTag("0");
                        bottomButton.setText("Use As Volunteer");
                        bottomButton.setTag("1");
                        break;
                    case "pin":
                        setButtonsTextAndTag(2);
                        break;
                    case "volunteer":
                        setButtonsTextAndTag(2);
                        break;
                }
                break;
            case 2:
                switch (usage) {
                    case "pin":
                        topButton.setText("Make a Call");
                        topButton.setTag("2");
                        bottomButton.setText("Settings");
                        bottomButton.setTag("3");
                        break;
                    case "volunteer":
                        topButton.setText("Show List of People In Need");
                        topButton.setTag("4");
                        bottomButton.setText("Settings");
                        bottomButton.setTag("5");
                        break;
                }
                break;
            case 3:
                switch (usage) {
                    case "pin":
                        topButton.setText("Call Personal Contact");
                        topButton.setTag("6");
                        bottomButton.setText("Call Volunteer");
                        bottomButton.setTag("7");
                        break;
                    case "volunteer":
                        break;
                }
                break;
        }

        level = wantedLevel;
    }

    @Override
    public void onBackPressed() {
        switch (level) {
            case 0:
            case 1:
            case 2:
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_HOME);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                break;
            case 3:
                setButtonsTextAndTag(2);
                break;
        }
    }
}