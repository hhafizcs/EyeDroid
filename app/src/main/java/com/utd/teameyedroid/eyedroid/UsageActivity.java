package com.utd.teameyedroid.eyedroid;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

public class UsageActivity extends AppCompatActivity {

    private SharedPreferences preferences;
    private SharedPreferences.Editor prefEditor;

    private TextToSpeech textToSpeechObj;
    private TextToSpeech.OnInitListener ttsListener;

    private Button topButton;
    private Button bottomButton;

    private int level = 0;

    private String useVoiceRec;
    private String usage;
    private boolean speechNotRecognized = false;
    private boolean afterExitChat = false;

    private final int RC_SPEECH_INPUT = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usage);

        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        topButton = findViewById(R.id.topButton);
        bottomButton = findViewById(R.id.bottomButton);

        setListeners();

        textToSpeechObj = new TextToSpeech(getApplicationContext(), ttsListener);
    }

    @Override
    protected void onStart() {
        super.onStart();

        boolean isEmulator = Build.MODEL.startsWith("sdk")
                || "google_sdk".equals(Build.MODEL)
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK");

        usage = preferences.getString("usage", "notSet");
        useVoiceRec = preferences.getString("useVoiceRec", "notSet");

        if(isEmulator && !useVoiceRec.equals("no")) {
            prefEditor = preferences.edit();
            prefEditor.putString("useVoiceRec", "no");
            prefEditor.commit();
            useVoiceRec = "no";
        }

        if (useVoiceRec.equals("no") || usage.equals("volunteer") || (usage.equals("pin") && useVoiceRec.equals("no"))) {
            topButton.setVisibility(View.VISIBLE);
            bottomButton.setVisibility(View.VISIBLE);
            initialize();
        }
    }

    private void initialize () {
        Bundle bundle = getIntent().getExtras();
        if(bundle != null) {
            String chatEnded = bundle.getString("chatEnded");
            if(chatEnded != null) {
                String message = "";

                switch (chatEnded) {
                    case "volunteerExited":
                        if(usage.equals("pin")) {
                            message = "The volunteer has left the chat.";
                        }
                        else if(usage.equals("volunteer")) {
                            message = "You have left the chat.";
                        }
                        break;
                    case "pinExited":
                        if(usage.equals("pin")) {
                            message = "You have left the chat.";
                        }
                        else if(usage.equals("volunteer")) {
                            message = "The person in need has left the chat.";
                        }
                        break;
                    case "couldNotConnect":
                        message = "Could not make a connection.";
                        break;
                }

                if (useVoiceRec.equals("yes")) {
                    afterExitChat = true;
                    textToSpeechObj.speak(message, TextToSpeech.QUEUE_ADD, null, "1");
                } else {
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    level = 2;
                    setButtonsTextAndTag(2);
                    topButton.setVisibility(View.VISIBLE);
                    bottomButton.setVisibility(View.VISIBLE);
                }
            }
        } else {
            setButtonsTextAndTag(1);
        }
    }

    private void recognizeSpeech() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US);
        startActivityForResult(intent, RC_SPEECH_INPUT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case RC_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && data != null) {
                    ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                    if (matches.contains("person in need") && level == 1) {
                        executeAction("0");
                    } else if (matches.contains("volunteer") && (level == 1 || level == 3)) {
                        if(level == 1) {
                            topButton.setVisibility(View.VISIBLE);
                            bottomButton.setVisibility(View.VISIBLE);
                            executeAction("1");
                        } else if(level == 3) {
                            executeAction("7");
                        }
                    } else if (matches.contains("yes") && level == 2) {
                        prefEditor = preferences.edit();
                        prefEditor.putString("useVoiceRec", "yes");
                        prefEditor.commit();
                        useVoiceRec = "yes";
                        level = 3;
                        textToSpeechObj.speak(getOptionsMessage(level), TextToSpeech.QUEUE_ADD, null, "1");
                    } else if (matches.contains("no") && level == 2) {
                        prefEditor = preferences.edit();
                        prefEditor.putString("useVoiceRec", "no");
                        prefEditor.commit();
                        useVoiceRec = "no";
                        topButton.setVisibility(View.VISIBLE);
                        bottomButton.setVisibility(View.VISIBLE);
                        initialize();
                    } else if (matches.contains("contact") && level == 3) {
                        executeAction("6");
                    } else {
                        speechNotRecognized = true;
                        textToSpeechObj.speak("Your command is not valid.", TextToSpeech.QUEUE_ADD, null, "1");
                    }
                }
                break;
            }

        }
    }

    public void buttonClicked (View v) {
        executeAction(v.getTag().toString());
    }

    private void executeAction (String actionID) {
        switch (actionID) {
            case "0":
                //use as pin clicked
                prefEditor = preferences.edit();
                prefEditor.putString("usage", "pin");
                prefEditor.commit();
                usage = "pin";
                if(useVoiceRec.equals("notSet")) {
                    level = 2;
                    textToSpeechObj.speak(getOptionsMessage(level), TextToSpeech.QUEUE_ADD, null, "1");
                } else if(useVoiceRec.equals("yes")) {
                    level = 2;
                    textToSpeechObj.speak(getOptionsMessage(level), TextToSpeech.QUEUE_ADD, null, "1");
                } else if(useVoiceRec.equals("no")) {
                    level = 2;
                    setButtonsTextAndTag(level);
                }
                break;
            case "1":
                //use as volunteer clicked
                prefEditor = preferences.edit();
                prefEditor.putString("usage", "volunteer");
                prefEditor.commit();
                usage = "volunteer";
                level = 2;
                setButtonsTextAndTag(level);
                break;
            case "2":
                //make a call clicked
                level = 3;
                setButtonsTextAndTag(3);
                break;
            case "3":
                //pin settings clicked
                break;
            case "4":
                //show list of pins clicked
                startActivity(new Intent(this, PinListActivity.class));
                break;
            case "5":
                //volunteer settings clicked
                break;
            case "6":
                //call personal contact clicked
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
                        level = 2;
                        setButtonsTextAndTag(level);
                        break;
                    case "volunteer":
                        setButtonsTextAndTag(2);
                        level = 2;
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
    }

    private String getOptionsMessage (int wantedOptionsLevel) {
        String message = "";

        switch (wantedOptionsLevel) {
            case 1:
                message = "say, Person In Need, to use the app as a person in need, or say, Volunteer, to use the app as a volunteer.";
                break;
            case 2:
                message = "say, yes, if you would like to use voice commands, or say, no, otherwise.";
                break;
            case 3:
                message = "say, Contact, to call a personal contact, or say, Volunteer, to call a volunteer.";
                break;
        }

        return message;
    }

    private void goHome() {
        if(textToSpeechObj.isSpeaking()) {
            textToSpeechObj.stop();
        }

        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void setListeners () {
        ttsListener =  new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    textToSpeechObj.setLanguage(Locale.UK);
                    textToSpeechObj.setSpeechRate(0.8f);

                    textToSpeechObj.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                        @Override
                        public void onStart(String utteranceId) {

                        }

                        @Override
                        public void onDone(String utteranceId) {
                            if(afterExitChat) {
                                level = 3;
                                textToSpeechObj.speak(getOptionsMessage(3), TextToSpeech.QUEUE_ADD, null, "1");
                                afterExitChat = false;
                            } else {
                                if(speechNotRecognized) {
                                    textToSpeechObj.speak(getOptionsMessage(level), TextToSpeech.QUEUE_ADD, null, "1");
                                    speechNotRecognized = false;
                                } else {
                                    recognizeSpeech();
                                }
                            }
                        }

                        @Override
                        public void onError(String utteranceId) {

                        }
                    });

                    Bundle bundle = getIntent().getExtras();
                    if(bundle != null) {
                        initialize();
                    } else {
                        if(!useVoiceRec.equals("no")) {
                            if (usage.equals("notSet")) {
                                level = 1;
                                textToSpeechObj.speak(getOptionsMessage(level), TextToSpeech.QUEUE_ADD, null, "1");
                            } else {
                                if(useVoiceRec.equals("notSet")) {
                                    level = 2;
                                    textToSpeechObj.speak(getOptionsMessage(level), TextToSpeech.QUEUE_ADD, null, "1");
                                } else if(useVoiceRec.equals("yes")) {
                                    level = 3;
                                    textToSpeechObj.speak(getOptionsMessage(level), TextToSpeech.QUEUE_ADD, null, "1");
                                }
                            }
                        }
                    }
                }
            }
        };
    }

    @Override
    public void onBackPressed() {
        if (useVoiceRec.equals("notSet") || useVoiceRec.equals("yes")) {
            goHome();
        } else {
            switch (level) {
                case 0:
                case 1:
                case 2:
                    goHome();
                    break;
                case 3:
                    setButtonsTextAndTag(2);
                    break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        textToSpeechObj.shutdown();

        super.onDestroy();
    }
}