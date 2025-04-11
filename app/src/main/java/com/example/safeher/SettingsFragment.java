package com.example.safeher;
import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
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
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.app.AlarmManager;
import android.app.PendingIntent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SettingsFragment extends Fragment {

    private EditText messageBody;
    private CheckBox playSound, sendSMS, sendNotification;
    private EditText editTextFakeCallerName;
    private Button saveButton;
    private MaterialButton selectContactsButton, signOutButton;
    private TextView userEmailTextView;
    private LinearLayout selectedContactsContainer;
    private MaterialSwitch switchScheduleFakeCall;
    private Spinner spinnerFakeCallInterval;

    private static final int PICK_CONTACT_REQUEST_1 = 1;
    private static final int PICK_CONTACT_REQUEST_2 = 2;
    private static final String PREFS_NAME = "SOSSettings";
    private static final String CONTACTS_KEY = "emergencyContacts";
    private static final String FAKE_CALLER_NAME_KEY = "fakeCallerName";
    private static final String FAKE_CALL_SCHEDULE_ENABLED_KEY = "fakeCallScheduleEnabled";
    private static final String FAKE_CALL_INTERVAL_MS_KEY = "fakeCallIntervalMs";

    private static final long INTERVAL_OFF = 0;
    private static final long INTERVAL_1_MIN = 60 * 1000;
    private static final long INTERVAL_5_MIN = 5 * 60 * 1000;
    private static final long INTERVAL_10_MIN = 10 * 60 * 1000;

    private static final List<Long> INTERVAL_VALUES = Arrays.asList(INTERVAL_OFF, INTERVAL_1_MIN, INTERVAL_5_MIN, INTERVAL_10_MIN);
    private static final List<String> INTERVAL_NAMES = Arrays.asList("Off", "1 Minute", "5 Minutes", "10 Minutes");

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
        editTextFakeCallerName = view.findViewById(R.id.editTextFakeCallerName);
        switchScheduleFakeCall = view.findViewById(R.id.switchScheduleFakeCall);
        spinnerFakeCallInterval = view.findViewById(R.id.spinnerFakeCallInterval);
        saveButton = view.findViewById(R.id.saveButton);
        selectedContactsContainer = view.findViewById(R.id.selectedContactsContainer);
        selectContactsButton = view.findViewById(R.id.selectContactsButton);
        userEmailTextView = view.findViewById(R.id.userEmailTextView);
        signOutButton = view.findViewById(R.id.signOutButton);

        setupIntervalSpinner();
        loadSettings();
        loadContacts();
        displaySelectedContacts();

        setupUserUI();

        switchScheduleFakeCall.setOnCheckedChangeListener((buttonView, isChecked) -> {
            spinnerFakeCallInterval.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (!isChecked) {
                spinnerFakeCallInterval.setSelection(0);
            }
        });

        saveButton.setOnClickListener(v -> saveSettingsAndSchedule());

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
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        messageBody.setText(prefs.getString("messageBody", "Emergency! I need help!"));
        playSound.setChecked(prefs.getBoolean("playSound", true));
        sendSMS.setChecked(prefs.getBoolean("sendSMS", true));
        sendNotification.setChecked(prefs.getBoolean("sendNotification", true));
        editTextFakeCallerName.setText(prefs.getString(FAKE_CALLER_NAME_KEY, "Mom"));

        boolean scheduleEnabled = prefs.getBoolean(FAKE_CALL_SCHEDULE_ENABLED_KEY, false);
        long intervalMs = prefs.getLong(FAKE_CALL_INTERVAL_MS_KEY, INTERVAL_OFF);

        switchScheduleFakeCall.setChecked(scheduleEnabled);
        spinnerFakeCallInterval.setVisibility(scheduleEnabled ? View.VISIBLE : View.GONE);

        int selectionIndex = INTERVAL_VALUES.indexOf(intervalMs);
        if (selectionIndex < 0) {
            selectionIndex = 0;
        }
        spinnerFakeCallInterval.setSelection(selectionIndex);
    }

    private void loadContacts() {
        if (getContext() == null) return;
        SharedPreferences sharedPreferences = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        selectedContacts = new HashSet<>(sharedPreferences.getStringSet(CONTACTS_KEY, new HashSet<>()));
    }

    private void saveSettingsAndSchedule() {
        if (getContext() == null) return;
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString("messageBody", messageBody.getText().toString());
        editor.putBoolean("playSound", playSound.isChecked());
        editor.putBoolean("sendSMS", sendSMS.isChecked());
        editor.putBoolean("sendNotification", sendNotification.isChecked());
        String fakeCaller = editTextFakeCallerName.getText().toString().trim();
        editor.putString(FAKE_CALLER_NAME_KEY, fakeCaller.isEmpty() ? "Mom" : fakeCaller);

        boolean scheduleEnabled = switchScheduleFakeCall.isChecked();
        long intervalMs = INTERVAL_OFF;
        if (scheduleEnabled) {
            int selectedPosition = spinnerFakeCallInterval.getSelectedItemPosition();
            if (selectedPosition >= 0 && selectedPosition < INTERVAL_VALUES.size()) {
                intervalMs = INTERVAL_VALUES.get(selectedPosition);
                if (intervalMs == INTERVAL_OFF) {
                    scheduleEnabled = false;
                }
            }
        }

        editor.putBoolean(FAKE_CALL_SCHEDULE_ENABLED_KEY, scheduleEnabled);
        editor.putLong(FAKE_CALL_INTERVAL_MS_KEY, intervalMs);

        editor.apply();
        saveContacts();

        scheduleOrCancelFakeCallAlarm(scheduleEnabled, intervalMs);

        Toast.makeText(getContext(), "Settings saved successfully", Toast.LENGTH_SHORT).show();
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

    private void setupIntervalSpinner() {
        if (getContext() == null) return;
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, INTERVAL_NAMES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFakeCallInterval.setAdapter(adapter);
    }

    private void scheduleOrCancelFakeCallAlarm(boolean enable, long intervalMs) {
        if (getContext() == null) return;
        Context context = getContext();
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, FakeCallAlarmReceiver.class);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, flags);

        if (enable && intervalMs > 0 && alarmManager != null) {
            long triggerAtMillis = System.currentTimeMillis() + intervalMs;

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    Log.w(TAG, "Exact alarm permission not granted. Cannot schedule precisely.");
                    Toast.makeText(context, "Permission needed to schedule exact alarms.", Toast.LENGTH_LONG).show();
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
                    Log.i(TAG, "Scheduled fake call alarm for " + intervalMs + "ms from now.");
                    Toast.makeText(context, "Fake call scheduled for " + INTERVAL_NAMES.get(INTERVAL_VALUES.indexOf(intervalMs)), Toast.LENGTH_SHORT).show();
                }
            } catch (SecurityException se) {
                Log.e(TAG, "SecurityException scheduling exact alarm. Check permissions.", se);
                Toast.makeText(context, "Could not schedule fake call due to permission issues.", Toast.LENGTH_LONG).show();
            }
        } else {
            if (alarmManager != null) {
                alarmManager.cancel(pendingIntent);
                Log.i(TAG, "Cancelled existing fake call alarm.");
                if (enable && intervalMs <= 0) {
                } else if (!enable) {
                }
            }
        }
    }
}