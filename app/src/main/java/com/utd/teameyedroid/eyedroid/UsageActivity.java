package com.utd.teameyedroid.eyedroid;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
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
    private LinearLayout selectContactLinearLayout;
    private LinearLayout usageLinearLayout;

    private int level = 0;

    private String useVoiceRec;
    private String usage;
    private boolean speechNotRecognized = false;
    private boolean afterExitChat = false;

    private static final long DOUBLE_PRESS_INTERVAL = 250;
    private boolean doubleClicked = false;
    private long lastPressTime;

    private final int RC_SPEECH_INPUT = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usage);

        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        topButton = findViewById(R.id.topButton);
        bottomButton = findViewById(R.id.bottomButton);
        selectContactLinearLayout = findViewById(R.id.selectContactLinearLayout);
        usageLinearLayout = findViewById(R.id.usageLinearLayout);

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
                    case "contactExited":
                        if(usage.equals("pin")) {
                            message = "The personal contact has left the chat.";
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
            level = 1;
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
                    int numOfContacts = preferences.getInt("numOfContacts", 0);
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
                        level = 4;
                        textToSpeechObj.speak(getOptionsMessage(level), TextToSpeech.QUEUE_ADD, null, "1");
                    } else if (matches.contains("one") && level == 4 && numOfContacts > 0) {
                        callPersonalContact(1);
                    } else if (matches.contains("two") && level == 4 && numOfContacts > 1) {
                        callPersonalContact(2);
                    } else if (matches.contains("three") && level == 4 && numOfContacts > 2) {
                        callPersonalContact(3);
                    } else if (matches.contains("buttons") && level > 2) {
                        topButton.setVisibility(View.VISIBLE);
                        bottomButton.setVisibility(View.VISIBLE);
                        prefEditor = preferences.edit();
                        prefEditor.putString("useVoiceRec", "no");
                        prefEditor.commit();
                        useVoiceRec = "no";
                        level = 2;
                        setButtonsTextAndTag(level);
                    } else {
                        speechNotRecognized = true;
                        textToSpeechObj.speak("Your command is not valid.", TextToSpeech.QUEUE_ADD, null, "1");
                    }
                }
                break;
            }

        }
    }

    public void callPersonalContact (int contactPosition) {
        String personalContactEmail = preferences.getString("contact" + contactPosition, "notSet").split("/")[1];

        Intent personalCallIntent = new Intent(UsageActivity.this, MainActivity.class);
        Bundle personalCallBundle = new Bundle();
        personalCallBundle.putString("cnxType", "pinToContact");
        personalCallBundle.putString("contactEmail", personalContactEmail);
        personalCallIntent.putExtras(personalCallBundle);
        startActivity(personalCallIntent);
        finish();
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
                prefEditor = preferences.edit();
                prefEditor.putString("useVoiceRec", "no");
                prefEditor.commit();
                useVoiceRec = "no";
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
                startActivity(new Intent(UsageActivity.this, SettingsActivity.class));
                finish();
                break;
            case "4":
                //show list of pins clicked
                startActivity(new Intent(UsageActivity.this, PinListActivity.class));
                finish();
                break;
            case "5":
                //volunteer settings clicked
                startActivity(new Intent(UsageActivity.this, SettingsActivity.class));
                finish();
                break;
            case "6":
                //call personal contact clicked
                level = 4;
                setButtonsTextAndTag(4);
                break;
            case "7":
                //call volunteer clicked
                Intent volunteerCallIntent = new Intent(UsageActivity.this, MainActivity.class);
                Bundle volunteerCallBundle = new Bundle();
                volunteerCallBundle.putString("cnxType", "pinToVolunteer");
                volunteerCallIntent.putExtras(volunteerCallBundle);
                startActivity(volunteerCallIntent);
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
                if(usage.equals("pin")) {
                    topButton.setVisibility(View.VISIBLE);
                    bottomButton.setVisibility(View.VISIBLE);
                    usageLinearLayout.setVisibility(View.VISIBLE);
                    selectContactLinearLayout.setVisibility(View.INVISIBLE);
                    topButton.setText("Call Personal Contact");
                    topButton.setTag("6");
                    bottomButton.setText("Call Volunteer");
                    bottomButton.setTag("7");
                }
                break;
            case 4:
                if(usage.equals("pin")) {
                    topButton.setVisibility(View.INVISIBLE);
                    bottomButton.setVisibility(View.INVISIBLE);
                    usageLinearLayout.setVisibility(View.INVISIBLE);
                    selectContactLinearLayout.setVisibility(View.VISIBLE);
                }
                break;
        }
    }

    private String getOptionsMessage (int wantedOptionsLevel) {
        String message = "";

        switch (wantedOptionsLevel) {
            case 1:
                message = "say, Person In Need, to use the application as a person in need, or say, Volunteer, to use the application as a volunteer.";
                break;
            case 2:
                message = "say, yes, if you would like to use voice commands, or say, no, otherwise.";
                break;
            case 3:
                message = "say, Contact, to call a personal contact, or say, Volunteer, to call a volunteer.";
                break;
            case 4:
                message = getCallPersonalConatctMessage();
                break;
        }

        return message;
    }

    private String getCallPersonalConatctMessage() {
        String message = "";
        int numOfContacts = preferences.getInt("numOfContacts", 0);

        switch (numOfContacts) {
            case 0:
                message = "No personal contacts have been added.";
                break;
            case 1:
                message = "Say one, to call contact one.";
                break;
            case 2:
                message = "Say one, to call contact one, or say two, to call contact two.";
                break;
            case 3:
                message = "Say one, to call contact one, say two, to call contact two, or say three, to call contact three.";
                break;
        }

        return  message;
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
                            int numOfContacts = preferences.getInt("numOfContacts", 0);

                            if(afterExitChat) {
                                level = 3;
                                textToSpeechObj.speak(getOptionsMessage(3), TextToSpeech.QUEUE_ADD, null, "1");
                                afterExitChat = false;
                            } else {
                                if(level == 4 && numOfContacts == 0) {
                                    level = 3;
                                    textToSpeechObj.speak(getOptionsMessage(level), TextToSpeech.QUEUE_ADD, null, "1");
                                } else if(speechNotRecognized) {
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

        selectContactLinearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long pressTime = System.currentTimeMillis();

                if (pressTime - lastPressTime <= DOUBLE_PRESS_INTERVAL) {
                    //Double click => Call Conatct 2
                    int numOfContacts = preferences.getInt("numOfContacts", 0);
                    if(numOfContacts > 1) {
                        callPersonalContact(2);
                    } else {
                        MediaPlayer wrongActionMP = MediaPlayer.create(UsageActivity.this,R.raw.wrong);
                        wrongActionMP.start();
                    }

                    doubleClicked = true;
                } else {
                    doubleClicked = false;

                    Handler myHandler = new Handler(new Handler.Callback() {
                        @Override
                        public boolean handleMessage(Message msg) {
                            if (!doubleClicked) {
                                //Single click => Call Conatct 1
                                int numOfContacts = preferences.getInt("numOfContacts", 0);
                                if(numOfContacts > 0) {
                                    callPersonalContact(1);
                                } else {
                                    MediaPlayer wrongActionMP = MediaPlayer.create(UsageActivity.this,R.raw.wrong);
                                    wrongActionMP.start();
                                }
                            }

                            return true;
                        }
                    });

                    Message m = new Message();
                    myHandler.sendMessageDelayed(m,DOUBLE_PRESS_INTERVAL);
                }

                lastPressTime = pressTime;
            }
        });

        selectContactLinearLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                //Long click => Call Conatct 3
                int numOfContacts = preferences.getInt("numOfContacts", 0);
                if(numOfContacts > 2) {
                    callPersonalContact(3);
                } else {
                    MediaPlayer wrongActionMP = MediaPlayer.create(UsageActivity.this,R.raw.wrong);
                    wrongActionMP.start();
                }

                return true;
            }
        });

        View.OnLongClickListener switchToVoiceListener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if(usage.equals("notSet")) {
                    level = 1;
                    textToSpeechObj.speak(getOptionsMessage(level), TextToSpeech.QUEUE_ADD, null, "1");
                } else if (usage.equals("pin") && useVoiceRec.equals("notSet")) {
                    level = 2;
                    textToSpeechObj.speak(getOptionsMessage(level), TextToSpeech.QUEUE_ADD, null, "1");
                } else if(usage.equals("pin") && (level == 2 || level == 3)) {
                    topButton.setVisibility(View.INVISIBLE);
                    bottomButton.setVisibility(View.INVISIBLE);
                    prefEditor = preferences.edit();
                    prefEditor.putString("useVoiceRec", "yes");
                    prefEditor.commit();
                    useVoiceRec = "yes";
                    level = 3;
                    textToSpeechObj.speak(getOptionsMessage(level), TextToSpeech.QUEUE_ADD, null, "1");
                }

                return true;
            }
        };

        topButton.setOnLongClickListener(switchToVoiceListener);
        bottomButton.setOnLongClickListener(switchToVoiceListener);
        usageLinearLayout.setOnLongClickListener(switchToVoiceListener);
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
                    level = 2;
                    setButtonsTextAndTag(2);
                    break;
                case 4:
                    level = 3;
                    setButtonsTextAndTag(3);
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