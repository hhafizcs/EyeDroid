package com.utd.teameyedroid.eyedroid;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class EditContactsActivity extends AppCompatActivity {

    private DatabaseReference mUserReference;
    private ContactListAdapter adapter;
    private PersonalContact selectedContact;
    private TextView noContactsTextView;
    private ListView contactsListView;
    private SharedPreferences preferences;
    private SharedPreferences.Editor prefEditor;
    private String emailToAdd = "";
    private DialogInterface.OnClickListener dialogClickListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_contacts);

        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        selectedContact = null;

        noContactsTextView = findViewById(R.id.noContactsTextView);
        contactsListView = findViewById(R.id.contactsListView);

        adapter = new ContactListAdapter(this, new ArrayList<PersonalContact>());
        contactsListView.setAdapter(adapter);
        registerForContextMenu(contactsListView);

        createListeners();
    }

    @Override
    public void onStart() {
        super.onStart();

        getContacts();
    }

    private void getContacts() {
        int numOfContacts = preferences.getInt("numOfContacts", 0);

        adapter.clear();

        if(numOfContacts > 0) {
            String contact1String = preferences.getString("contact1", "notSet");
            if(!contact1String.equals("notSet")) {
                String[] contact1Array = contact1String.split("/");
                PersonalContact contact1 = new PersonalContact(contact1Array[0], contact1Array[1], contact1Array[2]);
                adapter.add(contact1);
            }
        }

        if(numOfContacts > 1) {
            String contact2String = preferences.getString("contact2", "notSet");
            if(!contact2String.equals("notSet")) {
                String[] contact2Array = contact2String.split("/");
                PersonalContact contact2 = new PersonalContact(contact2Array[0], contact2Array[1], contact2Array[2]);
                adapter.add(contact2);
            }
        }

        if(numOfContacts > 2) {
            String contact3String = preferences.getString("contact3", "notSet");
            if(!contact3String.equals("notSet")) {
                String[] contact3Array = contact3String.split("/");
                PersonalContact contact3 = new PersonalContact(contact3Array[0], contact3Array[1], contact3Array[2]);
                adapter.add(contact3);
            }
        }

        adapter.notifyDataSetChanged();

        if(adapter.getContacts().size() == 0) {
            noContactsTextView.setVisibility(View.VISIBLE);
        } else {
            noContactsTextView.setVisibility(View.INVISIBLE);
        }
    }

    private void contactNotFound() {
        Toast.makeText(this, "There is no personal contact registered to use the application who has the provided email.", Toast.LENGTH_LONG).show();
    }

    public void addContactClicked (View v) {
        int numOfContacts = preferences.getInt("numOfContacts", 0);

        if(numOfContacts == 3) {
            Toast.makeText(this, "Only three personal contacts can be added!", Toast.LENGTH_LONG).show();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Add Contact");

            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
            builder.setView(input);

            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    emailToAdd = input.getText().toString();

                    if(contactAlreadyAdded(emailToAdd)) {
                        Toast.makeText(EditContactsActivity.this, "A personal contact with the provided email has already been added!", Toast.LENGTH_LONG).show();
                    } else {
                        mUserReference = FirebaseDatabase.getInstance().getReference().child("Users");

                        mUserReference.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                boolean contactAdded = false;

                                if (dataSnapshot != null) {
                                    if(dataSnapshot.exists()) {
                                        for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                                            User user = userSnapshot.getValue(User.class);
                                            if(user != null && user.email.equals(emailToAdd)) {
                                                int numOfContacts = preferences.getInt("numOfContacts", 0);
                                                numOfContacts++;

                                                String contactString = user.username + "/" + user.email + "/" + user.displayName;

                                                prefEditor = preferences.edit();

                                                if(numOfContacts == 1)
                                                    prefEditor.putString("contact1", contactString);
                                                else if(numOfContacts == 2)
                                                    prefEditor.putString("contact2", contactString);
                                                else if(numOfContacts == 3)
                                                    prefEditor.putString("contact3", contactString);

                                                prefEditor.commit();

                                                prefEditor.putInt("numOfContacts", numOfContacts);
                                                prefEditor.commit();

                                                contactAdded = true;

                                                getContacts();
                                            }
                                        }
                                    }
                                }

                                if(!contactAdded) {
                                    contactNotFound();
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                    }
                }
            });

            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

            builder.show();
        }
    }

    private void createListeners() {
        contactsListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                selectedContact = (PersonalContact) contactsListView.getAdapter().getItem(position);
                return false;
            }
        });

        dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        //Yes button clicked
                        if(selectedContact != null) {
                            ArrayList<String> contactStrings = new ArrayList<>();

                            String contact1String = preferences.getString("contact1", "notSet");
                            if(!contact1String.equals("notSet")) {
                                contactStrings.add(contact1String);
                            }

                            String contact2String = preferences.getString("contact2", "notSet");
                            if(!contact2String.equals("notSet")) {
                                contactStrings.add(contact2String);
                            }

                            String contact3String = preferences.getString("contact3", "notSet");
                            if(!contact3String.equals("notSet")) {
                                contactStrings.add(contact3String);
                            }

                            for(int i = 0; i < contactStrings.size(); i++) {
                                String[] contactArray = contactStrings.get(i).split("/");
                                if(contactArray[1].equals(selectedContact.email)) {
                                    contactStrings.remove(i);
                                }
                            }

                            prefEditor = preferences.edit();

                            prefEditor.putString("contact1", "notSet");
                            prefEditor.commit();
                            prefEditor.putString("contact2", "notSet");
                            prefEditor.commit();
                            prefEditor.putString("contact3", "notSet");
                            prefEditor.commit();

                            for(int i = 0; i < contactStrings.size(); i++) {
                                prefEditor.putString("contact" + (i + 1), contactStrings.get(i));
                                prefEditor.commit();
                            }

                            int numOfContacts = preferences.getInt("numOfContacts", 0);
                            numOfContacts--;
                            prefEditor.putInt("numOfContacts", numOfContacts);
                            prefEditor.commit();

                            getContacts();
                        }
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };
    }

    private boolean contactAlreadyAdded(String emailToCheck) {
        ArrayList<String> contactStrings = new ArrayList<>();

        String contact1String = preferences.getString("contact1", "notSet");
        if(!contact1String.equals("notSet")) {
            contactStrings.add(contact1String);
        }

        String contact2String = preferences.getString("contact2", "notSet");
        if(!contact2String.equals("notSet")) {
            contactStrings.add(contact2String);
        }

        String contact3String = preferences.getString("contact3", "notSet");
        if(!contact3String.equals("notSet")) {
            contactStrings.add(contact3String);
        }

        for(int i = 0; i < contactStrings.size(); i++) {
            String[] contactArray = contactStrings.get(i).split("/");
            if(contactArray[1].equals(emailToCheck)) {
                return true;
            }
        }

        return  false;
    }

    @Override
    public void onCreateContextMenu (ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo){
        if (v.getId() == R.id.contactsListView) {
            menu.add("Delete");
        }
    }

    @Override
    public boolean onContextItemSelected (MenuItem item){
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);

        builder.setMessage("Are you sure you want to remove " + selectedContact.displayName + " as a personal contact?")
                .setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener)
                .show();

        return super.onContextItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(EditContactsActivity.this, SettingsActivity.class));
        finish();
    }
}
