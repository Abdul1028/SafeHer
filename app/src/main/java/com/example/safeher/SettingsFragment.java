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

    private static final int PICK_CONTACT_REQUEST_1 = 1;
    private static final int PICK_CONTACT_REQUEST_2 = 2;
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

        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        startActivityForResult(intent, PICK_CONTACT_REQUEST_1);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == getActivity().RESULT_OK && data != null) {
            Uri contactUri = data.getData();
            if (contactUri != null) {
                String contactId = getContactIdFromUri(contactUri);

                if (contactId != null) {
                    String phoneNumber = getPhoneNumberFromContactId(contactId);

                    if (phoneNumber != null) {
                        Log.d("SettingsFragment", "Selected contact ID: " + contactId + ", Phone number: " + phoneNumber);

                        if (selectedContacts.size() < 5) {
                            selectedContacts.add(phoneNumber);
                            displaySelectedContacts();
                            saveContacts();
                        } else {
                            Toast.makeText(getContext(), "Contact limit reached.", Toast.LENGTH_SHORT).show();
                        }

                    } else {
                        Log.w("SettingsFragment", "Could not retrieve phone number for selected contact ID: " + contactId);
                        Toast.makeText(getContext(), "Could not get phone number for this contact.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.w("SettingsFragment", "Could not retrieve contact ID from URI: " + contactUri);
                    Toast.makeText(getContext(), "Could not identify selected contact.", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            Log.d("SettingsFragment", "Contact selection cancelled or failed.");
        }
    }

    private String getContactIdFromUri(Uri contactUri) {
        String contactId = null;
        Cursor cursor = null;
        if (getActivity() == null) return null;

        try {
            String[] projection = {ContactsContract.Contacts._ID};
            cursor = getActivity().getContentResolver().query(contactUri, projection, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                int idColumnIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID);
                if (idColumnIndex >= 0) {
                    contactId = cursor.getString(idColumnIndex);
                } else {
                    Log.e("SettingsFragment", "Could not find _ID column for contact URI.");
                }
            } else {
                Log.w("SettingsFragment", "Cursor is null or empty for contact URI: " + contactUri);
            }
        } catch (Exception e) {
            Log.e("SettingsFragment", "Error querying contact URI for ID: " + contactUri, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return contactId;
    }

    private String getPhoneNumberFromContactId(String contactId) {
        String phoneNumber = null;
        Cursor phoneCursor = null;

        if (getActivity() == null || contactId == null) {
            Log.e("SettingsFragment", "Activity or Contact ID is null, cannot query phone number.");
            return null;
        }

        try {
            Uri phoneUri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
            String phoneSelection = ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?";
            String[] phoneSelectionArgs = { contactId };
            String[] phoneProjection = { ContactsContract.CommonDataKinds.Phone.NUMBER };

            phoneCursor = getActivity().getContentResolver().query(phoneUri, phoneProjection, phoneSelection, phoneSelectionArgs, null);

            if (phoneCursor != null && phoneCursor.moveToFirst()) {
                int numberColumn = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                if (numberColumn >= 0) {
                    phoneNumber = phoneCursor.getString(numberColumn);
                    Log.d("SettingsFragment", "Retrieved Phone Number using Contact ID: " + phoneNumber);
                } else {
                    Log.e("SettingsFragment", "Phone number column not found in phone query using Contact ID.");
                }
            } else {
                Log.w("SettingsFragment", "Phone cursor is null or empty for Contact ID: " + contactId);
            }
        } catch (Exception e) {
            Log.e("SettingsFragment", "Error querying phone numbers for Contact ID: " + contactId, e);
        } finally {
            if (phoneCursor != null) {
                phoneCursor.close();
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

    private void saveContactPreference(String key, String phoneNumber) {
        if (getContext() != null) {
            SharedPreferences sharedPreferences = getContext().getSharedPreferences("SOSSettings", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(key, phoneNumber);
            editor.apply();
            Log.d("SettingsFragment", "Saved " + key + " = " + phoneNumber);
        }
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