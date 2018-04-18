package com.utd.teameyedroid.eyedroid;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;

public class SettingsActivity extends AppCompatActivity {

    private Button editContactsButton;
    private RadioGroup usageRadioGroup;
    private RadioButton pinRadioButton;
    private RadioButton volunteerRadioButton;
    private Switch voiceSwitch;

    private String usage = "";
    private String useVoiceRec = "";

    private SharedPreferences preferences;
    private SharedPreferences.Editor prefEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        editContactsButton = findViewById(R.id.editContactsButton);
        usageRadioGroup = findViewById(R.id.usageRadioGroup);
        pinRadioButton = findViewById(R.id.pinRadioButton);
        volunteerRadioButton = findViewById(R.id.volunteerRadioButton);
        voiceSwitch = findViewById(R.id.voiceSwitch);

        usage = preferences.getString("usage", "notSet");
        useVoiceRec = preferences.getString("useVoiceRec", "notSet");

        setSettings();
        setListeners();
    }

    private void setSettings() {
        if(usage.equals("pin")) {
            pinRadioButton.setChecked(true);
            volunteerRadioButton.setChecked(false);
            editContactsButton.setVisibility(View.VISIBLE);
        } else {
            pinRadioButton.setChecked(false);
            volunteerRadioButton.setChecked(true);
        }

        if(useVoiceRec.equals("yes")) {
            voiceSwitch.setChecked(true);
        } else {
            voiceSwitch.setChecked(false);
        }
    }

    private void setListeners() {
        usageRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if(checkedId == R.id.pinRadioButton) {
                    prefEditor = preferences.edit();
                    prefEditor.putString("usage", "pin");
                    prefEditor.commit();
                } else {
                    prefEditor = preferences.edit();
                    prefEditor.putString("volunteer", "pin");
                    prefEditor.commit();
                }
            }
        });

        voiceSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    prefEditor = preferences.edit();
                    prefEditor.putString("useVoiceRec", "yes");
                    prefEditor.commit();
                } else {
                    prefEditor = preferences.edit();
                    prefEditor.putString("useVoiceRec", "no");
                    prefEditor.commit();
                }
            }
        });
    }

    public void editContactsClicked (View v) {
        startActivity(new Intent(SettingsActivity.this, EditContactsActivity.class));
        finish();
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(SettingsActivity.this, UsageActivity.class));
        finish();
    }
}
