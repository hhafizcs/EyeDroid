package com.utd.teameyedroid.eyedroid;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class PinListAdapter extends ArrayAdapter<Room> {
    private Context context;
    private ArrayList<Room> rooms;

    public PinListAdapter(Context context, ArrayList<Room> rooms) {
        super(context, R.layout.pin_row, rooms);
        this.context = context;
        this.rooms = rooms;
    }

    public ArrayList<Room> getRooms() {
        return rooms;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = null;

        try {
            rowView = inflater.inflate(R.layout.pin_row, parent, false);
        } catch (Exception e) {}

        if(rowView != null) {
            TextView displayNameTextView = rowView.findViewById(R.id.displayNameTextView);
            TextView dateTimeTextView = rowView.findViewById(R.id.dateTimeTextView);

            displayNameTextView.setText(rooms.get(position).pinDisplayName);
            displayNameTextView.setTag(rooms.get(position).pinUserName);

            long dateTimeLong = Long.parseLong(rooms.get(position).dateTime);
            SimpleDateFormat dateTimeformat = new SimpleDateFormat("MMMM dd, yyyy h:mm a", Locale.getDefault());
            String dateTimeString = dateTimeformat.format(new Date(dateTimeLong));
            dateTimeTextView.setText(dateTimeString);
        }

        return rowView;
    }
}
