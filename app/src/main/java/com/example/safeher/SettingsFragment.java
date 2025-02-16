//package com.example.safeher;
//
//import android.content.Context;
//import android.content.Intent;
//import android.content.SharedPreferences;
//import android.database.Cursor;
//import android.net.Uri;
//import android.os.Bundle;
//import android.provider.ContactsContract;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.Button;
//import android.widget.CheckBox;
//import android.widget.EditText;
//import android.widget.LinearLayout;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.annotation.Nullable;
//import androidx.fragment.app.Fragment;
//
//import com.google.android.material.button.MaterialButton;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class SettingsFragment extends Fragment {
//
//    private EditText contact1, contact2, contact3, messageBody;
//    private CheckBox playSound, sendSMS, sendNotification;
//    private Button saveButton;
//
//    private static final int PICK_CONTACT_REQUEST = 1;
//    private LinearLayout selectedContactsContainer;
//    private List<String> selectedContacts = new ArrayList<>();
//
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        View view = inflater.inflate(R.layout.fragment_settings, container, false);
//
//        // Initialize views
//
//        messageBody = view.findViewById(R.id.messageBody);
//        playSound = view.findViewById(R.id.playSound);
//        sendSMS = view.findViewById(R.id.sendSMS);
//        sendNotification = view.findViewById(R.id.sendNotification);
//        saveButton = view.findViewById(R.id.saveButton);
//
//        // Load saved settings
//        loadSettings();
//
//        // Save button click listener
//        saveButton.setOnClickListener(v -> saveSettings());
//
//        // Initialize views
//        selectedContactsContainer = view.findViewById(R.id.selectedContactsContainer);
//        MaterialButton selectContactsButton = view.findViewById(R.id.selectContactsButton);
//
//        // Open contact picker on button click
//        selectContactsButton.setOnClickListener(v -> openContactPicker());
//
//        return view;
//    }
//
//
//    // Open the contact picker
//    private void openContactPicker() {
//        if (selectedContacts.size() >= 5) {
//            Toast.makeText(getContext(), "You can only select up to 5 contacts.", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
//        startActivityForResult(intent, PICK_CONTACT_REQUEST);
//    }
//
//
//    @Override
//    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//
//        if (requestCode == PICK_CONTACT_REQUEST && resultCode == getActivity().RESULT_OK && data != null) {
//            Uri contactUri = data.getData();
//            if (contactUri != null) {
//                String phoneNumber = getPhoneNumberFromUri(contactUri);
//                if (phoneNumber != null && !selectedContacts.contains(phoneNumber)) {
//                    selectedContacts.add(phoneNumber);
//                    displaySelectedContacts();
//                }
//            }
//        }
//    }
//
//    // Retrieve the phone number from the selected contact URI
//    private String getPhoneNumberFromUri(Uri contactUri) {
//        String phoneNumber = null;
//        Cursor cursor = getActivity().getContentResolver().query(contactUri, null, null, null, null);
//
//        if (cursor != null && cursor.moveToFirst()) {
//            int phoneIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
//            phoneNumber = cursor.getString(phoneIndex);
//            cursor.close();
//        }
//
//        return phoneNumber;
//    }
//
//    // Display the selected contacts in the UI
//    private void displaySelectedContacts() {
//        selectedContactsContainer.removeAllViews();
//
//        for (String contact : selectedContacts) {
//            TextView contactView = new TextView(getContext());
//            contactView.setText(contact);
//            contactView.setTextSize(16);
//            contactView.setPadding(0, 8, 0, 8);
//            selectedContactsContainer.addView(contactView);
//        }
//    }
//
//
//    // Load saved settings from SharedPreferences
//    private void loadSettings() {
//        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("SOSSettings", Context.MODE_PRIVATE);
////        contact1.setText(sharedPreferences.getString("contact1", ""));
////        contact2.setText(sharedPreferences.getString("contact2", ""));
////        contact3.setText(sharedPreferences.getString("contact3", ""));
//        messageBody.setText(sharedPreferences.getString("messageBody", "Emergency! I need help!"));
//        playSound.setChecked(sharedPreferences.getBoolean("playSound", true));
//        sendSMS.setChecked(sharedPreferences.getBoolean("sendSMS", true));
//        sendNotification.setChecked(sharedPreferences.getBoolean("sendNotification", true));
//    }
//
//    // Save settings to SharedPreferences
//    private void saveSettings() {
//        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("SOSSettings", Context.MODE_PRIVATE);
//        SharedPreferences.Editor editor = sharedPreferences.edit();
//
////        editor.putString("contact1", contact1.getText().toString());
////        editor.putString("contact2", contact2.getText().toString());
////        editor.putString("contact3", contact3.getText().toString());
//        editor.putString("messageBody", messageBody.getText().toString());
//        editor.putBoolean("playSound", playSound.isChecked());
//        editor.putBoolean("sendSMS", sendSMS.isChecked());
//        editor.putBoolean("sendNotification", sendNotification.isChecked());
//
//        editor.apply();
//        Toast.makeText(getContext(), "Settings saved!", Toast.LENGTH_SHORT).show();
//    }
//}


package com.example.safeher;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class SettingsFragment extends Fragment {

    private EditText messageBody;
    private CheckBox playSound, sendSMS, sendNotification;
    private Button saveButton;

    private static final int PICK_CONTACT_REQUEST = 1;
    private LinearLayout selectedContactsContainer;
    private List<String> selectedContacts = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // Initialize views
        messageBody = view.findViewById(R.id.messageBody);
        playSound = view.findViewById(R.id.playSound);
        sendSMS = view.findViewById(R.id.sendSMS);
        sendNotification = view.findViewById(R.id.sendNotification);
        saveButton = view.findViewById(R.id.saveButton);
        selectedContactsContainer = view.findViewById(R.id.selectedContactsContainer);

        // Load saved settings
        loadSettings();

        // Save button click listener
        saveButton.setOnClickListener(v -> saveSettings());

        // Open contact picker on button click
        MaterialButton selectContactsButton = view.findViewById(R.id.selectContactsButton);
        selectContactsButton.setOnClickListener(v -> openContactPicker());

        return view;
    }

    // Open the contact picker
    private void openContactPicker() {
        if (selectedContacts.size() >= 5) {
            Toast.makeText(getContext(), "You can only select up to 5 contacts.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        startActivityForResult(intent, PICK_CONTACT_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_CONTACT_REQUEST && resultCode == getActivity().RESULT_OK && data != null) {
            Uri contactUri = data.getData();
            if (contactUri != null) {
                String phoneNumber = getPhoneNumberFromUri(contactUri);
                if (phoneNumber != null && !selectedContacts.contains(phoneNumber)) {
                    selectedContacts.add(phoneNumber);
                    displaySelectedContacts();
                }
            }
        }
    }

    // Retrieve the phone number from the selected contact URI
    private String getPhoneNumberFromUri(Uri contactUri) {
        String phoneNumber = null;
        Cursor cursor = getActivity().getContentResolver().query(contactUri, null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            int phoneIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            phoneNumber = cursor.getString(phoneIndex);
            cursor.close();
        }

        return phoneNumber;
    }

    // Display the selected contacts in the UI
    private void displaySelectedContacts() {
        selectedContactsContainer.removeAllViews();

        for (String contact : selectedContacts) {
            TextView contactView = new TextView(getContext());
            contactView.setText(contact);
            contactView.setTextSize(16);
            contactView.setPadding(0, 8, 0, 8);
            selectedContactsContainer.addView(contactView);
        }
    }

    // Load saved settings from SharedPreferences
    private void loadSettings() {
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("SOSSettings", Context.MODE_PRIVATE);

        // Load SOS message
        messageBody.setText(sharedPreferences.getString("messageBody", "Emergency! I need help!"));

        // Load SOS actions
        playSound.setChecked(sharedPreferences.getBoolean("playSound", true));
        sendSMS.setChecked(sharedPreferences.getBoolean("sendSMS", true));
        sendNotification.setChecked(sharedPreferences.getBoolean("sendNotification", true));

        // Load selected contacts
        String contacts = sharedPreferences.getString("contacts", "");
        if (!contacts.isEmpty()) {
            String[] contactsArray = contacts.split(",");
            for (String contact : contactsArray) {
                selectedContacts.add(contact);
            }
            displaySelectedContacts();
        }
    }

    // Save settings to SharedPreferences
    private void saveSettings() {
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("SOSSettings", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Save SOS message
        editor.putString("messageBody", messageBody.getText().toString());

        // Save SOS actions
        editor.putBoolean("playSound", playSound.isChecked());
        editor.putBoolean("sendSMS", sendSMS.isChecked());
        editor.putBoolean("sendNotification", sendNotification.isChecked());

        // Save selected contacts
        StringBuilder contactsBuilder = new StringBuilder();
        for (String contact : selectedContacts) {
            contactsBuilder.append(contact).append(",");
        }
        String contacts = contactsBuilder.length() > 0 ? contactsBuilder.substring(0, contactsBuilder.length() - 1) : "";
        editor.putString("contacts", contacts);

        editor.apply();
        Toast.makeText(getContext(), "Settings saved!", Toast.LENGTH_SHORT).show();
    }
}