package com.utd.teameyedroid.eyedroid;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;
import com.google.firebase.functions.HttpsCallableResult;
import com.vidyo.VidyoClient.Connector.Connector.ConnectorViewStyle;
import com.vidyo.VidyoClient.Connector.ConnectorPkg;
import com.vidyo.VidyoClient.Connector.Connector;
import com.vidyo.VidyoClient.Device.RemoteCamera;
import com.vidyo.VidyoClient.Endpoint.Participant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements Connector.IConnect {

    private static final String HOST = "prod.vidyo.io";

    private Connector mVidyoConnector = null;
    private Connector.IConnect mConnector = this;
    private int remoteParticipants = 2;

    private FrameLayout videoFrame;
    private Button disconnectButton;
    private TextView connectingTextView;
    private TextView dotTextView;

    private DatabaseReference mDatabase = null;
    private FirebaseFunctions mFunctions;

    private SharedPreferences preferences;

    private String cnxType = "";
    private String roomName = "";
    private String userName = "";
    private String displayName = "";
    private String contactEmail = "";
    private String usage;
    private static volatile boolean connected = false;
    private static volatile String dotText = "";
    private boolean chatLoaded = false;

    private TextToSpeech textToSpeechObj;
    private TextToSpeech.OnInitListener ttsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connected = false;
        dotText = "";

        videoFrame = findViewById(R.id.videoFrame);
        disconnectButton = findViewById(R.id.disconnectButton);
        connectingTextView = findViewById(R.id.connectingTextView);
        dotTextView = findViewById(R.id.dotTextView);

        ConnectorPkg.setApplicationUIContext(this);
        ConnectorPkg.initialize();

        mDatabase = FirebaseDatabase.getInstance().getReference();
        mFunctions = FirebaseFunctions.getInstance();

        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        usage = preferences.getString("usage", "notSet");

        setListeners();

        textToSpeechObj = new TextToSpeech(getApplicationContext(), ttsListener);

        Bundle bundle = getIntent().getExtras();
        if(bundle != null) {
            cnxType = bundle.getString("cnxType");
            contactEmail = bundle.getString("contactEmail");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        userName = preferences.getString("username", "notSet");
        displayName = preferences.getString("displayName", "notSet");

        videoFrame.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if(!chatLoaded) {
                    startVideoChat();
                    chatLoaded = true;
                }
            }
        });
    }

    private void startVideoChat () {
        switch (cnxType) {
            case "pinToVolunteer":
            case "pinToContact":
                //A Person In Need Is Trying To Connect To A Volunteer
                videoFrame.setVisibility(View.INVISIBLE);
                disconnectButton.setVisibility(View.INVISIBLE);
                connectingTextView.setVisibility(View.INVISIBLE);
                dotTextView.setVisibility(View.INVISIBLE);

                openRoom();

                break;
            case "volunteerToPin":
            case "contactToPin":
                //A Volunteer Is Trying To Connect To A Person In Need
                videoFrame.setVisibility(View.INVISIBLE);
                disconnectButton.setVisibility(View.INVISIBLE);

                Bundle bundle = getIntent().getExtras();
                if(bundle != null)
                    roomName = bundle.getString("roomName");

                joinRoom();

                break;
        }
    }

    private void openRoom () {
        roomName = userName + "|" + System.currentTimeMillis();

        mVidyoConnector = new Connector(videoFrame, ConnectorViewStyle.VIDYO_CONNECTORVIEWSTYLE_Default, remoteParticipants, "", "", 0);
        mVidyoConnector.showViewAt(videoFrame, 0, 0, videoFrame.getWidth(), videoFrame.getHeight());
        mVidyoConnector.setCameraPrivacy(false);
        mVidyoConnector.setMode(Connector.ConnectorMode.VIDYO_CONNECTORMODE_Foreground);
        registerConnectorParticipantEventListener();

        generateToken(userName)
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Exception e = task.getException();
                            if (e instanceof FirebaseFunctionsException) {
                                FirebaseFunctionsException ffe = (FirebaseFunctionsException) e;
                                FirebaseFunctionsException.Code code = ffe.getCode();
                                Object details = ffe.getDetails();
                            }
                        } else {
                            String token = task.getResult();

                            Room newRoom = new Room(roomName, displayName, cnxType , System.currentTimeMillis() + "", userName, contactEmail);
                            mDatabase.child("Rooms").child(roomName).setValue(newRoom);

                            mVidyoConnector.connect(HOST, token, displayName, roomName, mConnector);
                            mVidyoConnector.cycleCamera();
                        }
                    }
                });
    }

    private void joinRoom () {
        mVidyoConnector = new Connector(videoFrame, ConnectorViewStyle.VIDYO_CONNECTORVIEWSTYLE_Default, remoteParticipants, "", "", 0);
        mVidyoConnector.showViewAt(videoFrame, 0, 0, videoFrame.getWidth(), videoFrame.getHeight());
        mVidyoConnector.setCameraPrivacy(false);
        mVidyoConnector.setMode(Connector.ConnectorMode.VIDYO_CONNECTORMODE_Foreground);
        registerConnectorParticipantEventListener();

        generateToken(userName)
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Exception e = task.getException();
                            if (e instanceof FirebaseFunctionsException) {
                                FirebaseFunctionsException ffe = (FirebaseFunctionsException) e;
                                FirebaseFunctionsException.Code code = ffe.getCode();
                                Object details = ffe.getDetails();
                            }
                        } else {
                            String token = task.getResult();

                            mDatabase.child("Rooms").child(roomName).child("notificationSent").setValue(true);
                            mDatabase.child("Rooms").child(roomName).child("helperUserName").setValue(userName);
                            mDatabase.child("Rooms").child(roomName).child("helperDisplayName").setValue(displayName);
                            mDatabase.child("Rooms").child(roomName).child("connected").setValue(true);

                            mVidyoConnector.connect(HOST, token, displayName, roomName, mConnector);
                            mVidyoConnector.showPreview(false);
                        }
                    }
                });

        checkIfConnected();
    }

    private void checkIfConnected() {
        new Thread(new Runnable() {
            public void run() {
                int counter = 0;
                long beingSeconds = System.currentTimeMillis() / 1000;

                while (!connected) {
                    long laterSeconds = System.currentTimeMillis() / 1000;

                    if(laterSeconds - beingSeconds >= 15) {
                        break;
                    } else {
                        try {
                            TimeUnit.MILLISECONDS.sleep(400);
                            switch (counter % 4) {
                                case 0:
                                    dotText = ".";
                                    break;
                                case 1:
                                    dotText = "..";
                                    break;
                                case 2:
                                    dotText = "...";
                                    break;
                                case 3:
                                    dotText = "";
                                    break;
                            }
                            counter++;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    dotTextView.setText(dotText);
                                }
                            });
                        } catch (Exception e) {}
                    }
                }

                if(connected) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            connectingTextView.setVisibility(View.INVISIBLE);
                            dotTextView.setVisibility(View.INVISIBLE);
                            videoFrame.setVisibility(View.VISIBLE);
                            disconnectButton.setVisibility(View.VISIBLE);
                        }
                    });
                } else {
                    couldNotConnect();
                }
            }
        }).start();
    }

    public void disconnectClicked (View v) {
        Intent intent = new Intent(MainActivity.this, UsageActivity.class);
        Bundle bundle = new Bundle();
        if(cnxType.equals("volunteerToPin"))
            bundle.putString("chatEnded", "volunteerExited");
        else
            bundle.putString("chatEnded", "contactExited");

        intent.putExtras(bundle);
        startActivity(intent);
        finish();
    }

    private void disconnect () {
        mVidyoConnector.unregisterParticipantEventListener();
        mVidyoConnector.disconnect();
        mVidyoConnector.hideView(videoFrame);
        mVidyoConnector.disable();
    }

    public void onSuccess() {}

    public void onFailure(Connector.ConnectorFailReason reason) {}

    public void onDisconnected(Connector.ConnectorDisconnectReason reason) {}

    private void registerConnectorParticipantEventListener() {
        mVidyoConnector.registerParticipantEventListener(new Connector.IRegisterParticipantEventListener() {
            @Override
            public void onParticipantJoined(Participant participant) {

            }

            @Override
            public void onParticipantLeft(Participant participant) {
                switch (cnxType) {
                    case "pinToVolunteer":
                        //The volunteer has left the chat
                        volunteerLeftChat();
                        break;
                    case "volunteerToPin":
                    case "contactToPin":
                        //The PIN has left the chat
                        pinLeftChat();
                        break;
                    case "pinToContact":
                        //The Personal Contact has left the chat
                        contactLeftChat();
                        break;
                }
            }

            @Override
            public void onDynamicParticipantChanged(ArrayList<Participant> arrayList, ArrayList<RemoteCamera> arrayList1) {
                if (arrayList.size() == 1) {
                    connected = true;

                    if(usage.equals("pin")) {
                        if(cnxType.equals("pinToVolunteer"))
                            textToSpeechObj.speak("You are connected to a volunteer.", TextToSpeech.QUEUE_ADD, null, "1");
                        else
                            textToSpeechObj.speak("You are connected to the personal contact.", TextToSpeech.QUEUE_ADD, null, "1");
                    }
                }
            }

            @Override
            public void onLoudestParticipantChanged(Participant participant, boolean b) {

            }
        });
    }

    private void pinLeftChat () {
        Intent intent = new Intent(MainActivity.this, UsageActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("chatEnded", "pinExited");
        bundle.putInt("level", 2);
        intent.putExtras(bundle);
        startActivity(intent);
        finish();
    }

    private void volunteerLeftChat () {
        Intent intent = new Intent(MainActivity.this, UsageActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("chatEnded", "volunteerExited");
        bundle.putInt("level", 2);
        intent.putExtras(bundle);
        startActivity(intent);
        finish();
    }

    private void contactLeftChat () {
        Intent intent = new Intent(MainActivity.this, UsageActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("chatEnded", "contactExited");
        bundle.putInt("level", 2);
        intent.putExtras(bundle);
        startActivity(intent);
        finish();
    }

    private void couldNotConnect () {
        Intent intent = new Intent(MainActivity.this, UsageActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("chatEnded", "couldNotConnect");
        bundle.putInt("level", 2);
        intent.putExtras(bundle);
        startActivity(intent);
        finish();
    }

    private Task<String> generateToken(String username) {
        Map<String, Object> data = new HashMap<>();
        data.put("username", username);

        return mFunctions
                .getHttpsCallable("generateTokenOC")
                .call(data)
                .continueWith(new Continuation<HttpsCallableResult, String>() {
                    @Override
                    public String then(@NonNull Task<HttpsCallableResult> task) throws Exception {
                        return (String) task.getResult().getData();
                    }
                });
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

                        }

                        @Override
                        public void onError(String utteranceId) {

                        }
                    });

                    if(usage.equals("pin")) {
                        if(connected) {
                            if(cnxType.equals("pinToVolunteer"))
                                textToSpeechObj.speak("You are connected to a volunteer.", TextToSpeech.QUEUE_ADD, null, "1");
                            else
                                textToSpeechObj.speak("You are connected to the personal contact.", TextToSpeech.QUEUE_ADD, null, "1");
                        } else {
                            if(cnxType.equals("pinToVolunteer"))
                                textToSpeechObj.speak("Waiting for a volunteer to join the chat.", TextToSpeech.QUEUE_ADD, null, "1");
                            else
                                textToSpeechObj.speak("Waiting for the personal contact to join the chat.", TextToSpeech.QUEUE_ADD, null, "1");
                        }
                    }
                }
            }
        };
    }

    @Override
    public void onBackPressed() {
        String chatEnded = "";
        Intent intent = new Intent(MainActivity.this, UsageActivity.class);
        Bundle bundle = new Bundle();

        if(usage.equals("pin")) {
            chatEnded = "pinExited";
        } else if (usage.equals("volunteer") && cnxType.equals("volunteerToPin")) {
            chatEnded = "volunteerExited";
        } else if (usage.equals("volunteer") && cnxType.equals("contactToPin")) {
            chatEnded = "contactExited";
        }

        bundle.putString("chatEnded", chatEnded);
        intent.putExtras(bundle);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy () {
        disconnect();

        textToSpeechObj.shutdown();

        mDatabase.child("Rooms").child(roomName).removeValue();

        super.onDestroy();
    }
}
