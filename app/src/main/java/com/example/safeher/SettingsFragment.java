package com.example.safeher;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SettingsFragment extends Fragment {

    private EditText messageBody;
    private CheckBox playSound, sendSMS, sendNotification;
    private Button saveButton;
    private MaterialButton selectContactsButton, signOutButton;
    private TextView userEmailTextView;
    private LinearLayout selectedContactsContainer;

    private static final int PICK_CONTACT_REQUEST = 1;
    private static final String PREFS_NAME = "SOSSettings";
    private static final String CONTACTS_KEY = "emergencyContacts";

    private Set<String> selectedContacts = new HashSet<>();

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        mAuth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        if (getActivity() != null) {
            mGoogleSignInClient = GoogleSignIn.getClient(getActivity(), gso);
        }

        messageBody = view.findViewById(R.id.messageBody);
        playSound = view.findViewById(R.id.playSound);
        sendSMS = view.findViewById(R.id.sendSMS);
        sendNotification = view.findViewById(R.id.sendNotification);
        saveButton = view.findViewById(R.id.saveButton);
        selectedContactsContainer = view.findViewById(R.id.selectedContactsContainer);
        selectContactsButton = view.findViewById(R.id.selectContactsButton);
        userEmailTextView = view.findViewById(R.id.userEmailTextView);
        signOutButton = view.findViewById(R.id.signOutButton);

        loadSettings();
        loadContacts();
        displaySelectedContacts();

        setupUserUI();

        saveButton.setOnClickListener(v -> saveSettings());

        selectContactsButton.setOnClickListener(v -> openContactPicker());

        signOutButton.setOnClickListener(v -> signOut());

        return view;
    }

    private void setupUserUI() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            userEmailTextView.setText("Logged in as: " + currentUser.getEmail());
            userEmailTextView.setVisibility(View.VISIBLE);
            signOutButton.setVisibility(View.VISIBLE);
        } else {
            userEmailTextView.setText("Not logged in");
            userEmailTextView.setVisibility(View.VISIBLE);
            signOutButton.setVisibility(View.GONE);
        }
    }

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
                if (phoneNumber != null) {
                    phoneNumber = phoneNumber.replaceAll("[\\s\\-()]", "");
                    if (!selectedContacts.contains(phoneNumber) && selectedContacts.size() < 5) {
                        selectedContacts.add(phoneNumber);
                        displaySelectedContacts();
                        saveContacts();
                    } else if (selectedContacts.contains(phoneNumber)) {
                        Toast.makeText(getContext(), "Contact already selected.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Cannot add more than 5 contacts.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "Could not retrieve phone number.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private String getPhoneNumberFromUri(Uri contactUri) {
        String phoneNumber = null;
        if (getActivity() == null || getActivity().getContentResolver() == null) return null;

        Cursor cursor = getActivity().getContentResolver().query(contactUri,
                new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},
                null, null, null);

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    int phoneIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                    if (phoneIndex != -1) {
                        phoneNumber = cursor.getString(phoneIndex);
                    }
                }
            } finally {
                cursor.close();
            }
        }
        return phoneNumber;
    }

    private void displaySelectedContacts() {
        if (getContext() == null) return;
        selectedContactsContainer.removeAllViews();

        for (String contact : selectedContacts) {
            LinearLayout contactLayout = new LinearLayout(getContext());
            contactLayout.setOrientation(LinearLayout.HORIZONTAL);
            contactLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            contactLayout.setPadding(0, 8, 0, 8);

            TextView contactView = new TextView(getContext());
            contactView.setText(contact);
            contactView.setTextSize(16);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            contactView.setLayoutParams(textParams);

            Button removeButton = new Button(getContext());
            removeButton.setText("Remove");
            removeButton.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            removeButton.setOnClickListener(v -> {
                selectedContacts.remove(contact);
                displaySelectedContacts();
                saveContacts();
            });

            contactLayout.addView(contactView);
            contactLayout.addView(removeButton);
            selectedContactsContainer.addView(contactLayout);
        }
    }

    private void loadSettings() {
        if (getContext() == null) return;
        SharedPreferences sharedPreferences = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        messageBody.setText(sharedPreferences.getString("messageBody", "Emergency! I need help! My location: [Location]"));
        playSound.setChecked(sharedPreferences.getBoolean("playSound", true));
        sendSMS.setChecked(sharedPreferences.getBoolean("sendSMS", true));
        sendNotification.setChecked(sharedPreferences.getBoolean("sendNotification", true));
    }

    private void loadContacts() {
        if (getContext() == null) return;
        SharedPreferences sharedPreferences = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        selectedContacts = new HashSet<>(sharedPreferences.getStringSet(CONTACTS_KEY, new HashSet<>()));
    }

    private void saveSettings() {
        if (getContext() == null) return;
        SharedPreferences sharedPreferences = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString("messageBody", messageBody.getText().toString());
        editor.putBoolean("playSound", playSound.isChecked());
        editor.putBoolean("sendSMS", sendSMS.isChecked());
        editor.putBoolean("sendNotification", sendNotification.isChecked());

        editor.apply();
        saveContacts();
        Toast.makeText(getContext(), "Settings saved!", Toast.LENGTH_SHORT).show();
    }

    private void saveContacts() {
        if (getContext() == null) return;
        SharedPreferences sharedPreferences = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet(CONTACTS_KEY, selectedContacts);
        editor.apply();
    }

    private void signOut() {
        mAuth.signOut();

        if (mGoogleSignInClient != null) {
            mGoogleSignInClient.signOut().addOnCompleteListener(getActivity(), task -> {
                Log.d("SettingsFragment", "Google Sign Out complete.");
            });
        }

        if (getActivity() != null) {
            Intent intent = new Intent(getActivity(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish();
        }
    }
}