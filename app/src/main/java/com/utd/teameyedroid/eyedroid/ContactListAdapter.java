package com.utd.teameyedroid.eyedroid;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class ContactListAdapter extends ArrayAdapter<PersonalContact> {
    private Context context;
    private ArrayList<PersonalContact> contacts;

    public ContactListAdapter(Context context, ArrayList<PersonalContact> contacts) {
        super(context, R.layout.contact_row, contacts);
        this.context = context;
        this.contacts = contacts;
    }

    public ArrayList<PersonalContact> getContacts() {
        return contacts;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = null;

        try {
            rowView = inflater.inflate(R.layout.contact_row, parent, false);
        } catch (Exception e) {}

        if(rowView != null) {
            TextView contactDisplayNameTextView = rowView.findViewById(R.id.contactDisplayNameTextView);
            TextView emailTextView = rowView.findViewById(R.id.emailTextView);

            contactDisplayNameTextView.setText(contacts.get(position).displayName);
            contactDisplayNameTextView.setTag(contacts.get(position).username);
            emailTextView.setText(contacts.get(position).email);
        }

        return rowView;
    }
}
