package com.utd.teameyedroid.eyedroid;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements Connector.IConnect {

    private static final String HOST = "prod.vidyo.io";

    private Connector mVidyoConnector = null;
    private int remoteParticipants = 2;
    private FrameLayout videoFrame;
    private EditText roomEditText;
    private EditText userEditText;

    Connector.IConnect mConnector = this;

    private DatabaseReference mDatabase = null;
    private FirebaseFunctions mFunctions;

    private static final String[] mPermissions = new String[] {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        videoFrame = findViewById(R.id.videoFrame);
        roomEditText = findViewById(R.id.roomEditText);
        userEditText = findViewById(R.id.userEditText);

        ConnectorPkg.setApplicationUIContext(this);
        ConnectorPkg.initialize();

        List<String> permissionsNeeded = new ArrayList<>();
        for (String permission : mPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED)
                permissionsNeeded.add(permission);
        }

        if (permissionsNeeded.size() > 0) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), 1988);
        }

        mDatabase = FirebaseDatabase.getInstance().getReference();
        mFunctions = FirebaseFunctions.getInstance();
    }

    public void Connect (View v) {
        final String roomName = roomEditText.getText().toString();
        final String userName = userEditText.getText().toString();

        if(userName.equals("")) {
            Toast.makeText(getApplicationContext(), "The User Name Cannot Be Empty", Toast.LENGTH_SHORT).show();
        } else {
            if(roomName.equals("")) {
                Toast.makeText(getApplicationContext(), "The Room Name Cannot Be Empty", Toast.LENGTH_SHORT).show();
            } else {
                mVidyoConnector = new Connector(videoFrame, ConnectorViewStyle.VIDYO_CONNECTORVIEWSTYLE_Default, remoteParticipants, "", "", 0);
                mVidyoConnector.showViewAt(videoFrame, 0, 0, videoFrame.getWidth(), videoFrame.getHeight());
                mVidyoConnector.setCameraPrivacy(false);
                mVidyoConnector.setMode(Connector.ConnectorMode.VIDYO_CONNECTORMODE_Foreground);

                generateToken(userName, roomName)
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
                                    mVidyoConnector.connect(HOST, token, userName, roomName, mConnector);
                                }
                            }
                        });
            }
        }
    }

    public void Disconnect (View v) {
        mVidyoConnector.disconnect();
        mVidyoConnector.hideView(videoFrame);
        mVidyoConnector.disable();
    }

    public void onSuccess() {}

    public void onFailure(Connector.ConnectorFailReason reason) {}

    public void onDisconnected(Connector.ConnectorDisconnectReason reason) {}

    private Task<String> generateToken(String username, String roomname) {
        // Create the arguments to the callable function, which is just one string
        Map<String, Object> data = new HashMap<>();
        data.put("username", username);
        data.put("roomname", roomname);

        return mFunctions
                .getHttpsCallable("generateTokenOC")
                .call(data)
                .continueWith(new Continuation<HttpsCallableResult, String>() {
                    @Override
                    public String then(@NonNull Task<HttpsCallableResult> task) throws Exception {
                        // This continuation runs on either success or failure, but if the task
                        // has failed then getResult() will throw an Exception which will be
                        // propagated down.
                        return (String) task.getResult().getData();
                    }
                });
    }
}
