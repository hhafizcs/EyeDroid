package com.utd.teameyedroid.eyedroid;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class PinListActivity extends ListActivity {

    private DatabaseReference mRoomsReference;
    private ArrayAdapter<String> adapter;
    private ArrayList<Room> rooms = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_list);

        mRoomsReference = FirebaseDatabase.getInstance().getReference().child("Rooms");

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<String>());
        setListAdapter(adapter);
    }

    @Override
    public void onStart() {
        super.onStart();

        ValueEventListener roomsChangeListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ArrayList<String> pinUsernames = new ArrayList<>();

                rooms.clear();
                adapter.clear();

                for (DataSnapshot roomSnapshot : dataSnapshot.getChildren()) {
                    Room room = roomSnapshot.getValue(Room.class);
                    if(room != null && !room.connected) {
                        rooms.add(room);
                        pinUsernames.add(room.pinUsername);
                    }
                }

                for (String pinUsername : pinUsernames) {
                    adapter.add(pinUsername);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        };

        mRoomsReference.addValueEventListener(roomsChangeListener);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        String selectedPinUsername = (String) getListAdapter().getItem(position);

        Room selectedRoom = null;

        for (Room room : rooms) {
            if(room.pinUsername.equals(selectedPinUsername)) {
                selectedRoom = room;
            }
        }

        if(selectedRoom != null) {
            Intent intent = new Intent(PinListActivity.this, MainActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString("cnxType", "volunteerToPin");
            bundle.putString("roomName", selectedRoom.roomName);
            intent.putExtras(bundle);
            startActivity(intent);
            finish();
        }
    }
}
