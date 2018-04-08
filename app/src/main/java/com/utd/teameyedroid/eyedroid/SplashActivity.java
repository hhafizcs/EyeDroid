package com.utd.teameyedroid.eyedroid;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SplashActivity extends AppCompatActivity {

    Button givePermissionsButton;

    private SharedPreferences preferences;
    private SharedPreferences.Editor prefEditor;

    private DatabaseReference mDatabase;
    private DatabaseReference mUserReference;

    private static final int RC_SIGN_IN = 123;
    List<AuthUI.IdpConfig> providers = Arrays.asList(new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build());

    private static final String[] mPermissions = new String[] {
            android.Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        givePermissionsButton = findViewById(R.id.givePermissionsButton);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    }

    @Override
    protected void onStart() {
        super.onStart();

        checkPermissions();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        boolean allPermissionGranted = true;
        for (int grantResult : grantResults) {
            if(grantResult != 0) {
                allPermissionGranted = false;
            }
        }

        if(allPermissionGranted) {
            givePermissionsButton.setVisibility(View.INVISIBLE);
            startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder().setAvailableProviders(providers).build(), RC_SIGN_IN);
        } else {
            Toast.makeText(this, "The app cannot operate if not all permissions are granted. If the permissions dialog is not appearing anymore, please re-install the app.", Toast.LENGTH_LONG).show();
            givePermissionsButton.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                // Successfully signed in
                if(FirebaseAuth.getInstance().getCurrentUser() != null) {
                    handleUser(FirebaseAuth.getInstance().getCurrentUser());
                }
            }
        }
    }

    private void handleUser (FirebaseUser firebaseUser) {
        final User user = new User(firebaseUser.getUid(), firebaseUser.getDisplayName());

        prefEditor = preferences.edit();
        prefEditor.putString("username", user.username);
        prefEditor.putString("displayName", user.displayName);
        prefEditor.commit();

        mUserReference = FirebaseDatabase.getInstance().getReference().child("Users").child(user.username);

        mUserReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot != null) {
                    if(!dataSnapshot.exists()) {
                        mDatabase.child("Users").child(user.username).setValue(user);
                    }
                }

                startActivity(new Intent(SplashActivity.this, UsageActivity.class));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void givePermissions (View v) {
        checkPermissions();
    }

    private void checkPermissions () {
        List<String> permissionsNeeded = new ArrayList<>();
        for (String permission : mPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED)
                permissionsNeeded.add(permission);
        }

        if (permissionsNeeded.size() > 0) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), 1988);
        } else {
            startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder().setAvailableProviders(providers).build(), RC_SIGN_IN);
        }
    }
}
