package com.utd.teameyedroid.eyedroid;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class PinListActivity extends ListActivity {

    private DatabaseReference mRoomsReference;
    private PinListAdapter adapter;
    private DialogInterface.OnClickListener dialogClickListener;
    private Room selectedRoom;
    private TextView noPinsTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_list);

        mRoomsReference = FirebaseDatabase.getInstance().getReference().child("Rooms");

        createListeners();

        selectedRoom = null;

        noPinsTextView = findViewById(R.id.noPinsTextView);

        adapter = new PinListAdapter(this, new ArrayList<Room>());
        setListAdapter(adapter);
    }

    @Override
    public void onStart() {
        super.onStart();

        ValueEventListener roomsChangeListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                adapter.clear();

                for (DataSnapshot roomSnapshot : dataSnapshot.getChildren()) {
                    Room room = roomSnapshot.getValue(Room.class);
                    if(room != null && !room.connected) {
                        adapter.add(room);
                    }
                }

                adapter.notifyDataSetChanged();

                if(adapter.getRooms().size() == 0) {
                    noPinsTextView.setVisibility(View.VISIBLE);
                } else {
                    noPinsTextView.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        };

        mRoomsReference.addValueEventListener(roomsChangeListener);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        selectedRoom = (Room) getListAdapter().getItem(position);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage("Would you like to help " + selectedRoom.pinDisplayName + "?")
                .setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener)
                .show();
    }

    private void createListeners () {
        dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        //Yes button clicked
                        if(selectedRoom != null) {
                            Intent intent = new Intent(PinListActivity.this, MainActivity.class);
                            Bundle bundle = new Bundle();
                            bundle.putString("cnxType", "volunteerToPin");
                            bundle.putString("roomName", selectedRoom.roomName);
                            intent.putExtras(bundle);
                            startActivity(intent);
                            finish();
                        }
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(PinListActivity.this, UsageActivity.class));
        finish();
    }
}
