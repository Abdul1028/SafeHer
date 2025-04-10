package com.example.safeher;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

// Firebase Imports
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

// Import for Location List (needed for reading GeoFire data directly)
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ProfileFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";

    // UI Elements
    private TextView profileNameTextView;
    private TextView profileEmailTextView;
    private TextView profileDobTextView;
    private TextView profileBloodTextView;
    private TextView textViewFcmToken;
    private TextView textViewLatestLocation;
    private MaterialButton editProfileButton;
    private Button buttonLogout;

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference userRef;
    private DatabaseReference userLocationRef;
    private String currentUserId = null;

    // Listeners
    private ValueEventListener profileListener;
    private ValueEventListener fcmTokenListener;
    private ValueEventListener locationListener;

    // Data holder
    private Map<String, Object> userProfileData = null;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        profileNameTextView = view.findViewById(R.id.profileNameTextView);
        profileEmailTextView = view.findViewById(R.id.profileEmailTextView);
        profileDobTextView = view.findViewById(R.id.profileDobTextView);
        profileBloodTextView = view.findViewById(R.id.profileBloodTextView);
        editProfileButton = view.findViewById(R.id.editProfileButton);
        textViewFcmToken = view.findViewById(R.id.textViewFcmToken);
        textViewLatestLocation = view.findViewById(R.id.textViewLatestLocation);
        buttonLogout = view.findViewById(R.id.buttonLogout);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            profileEmailTextView.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "No email");

            userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUserId);
            userLocationRef = FirebaseDatabase.getInstance().getReference("user_locations").child(currentUserId);

            fetchAndDisplayProfileInfo();
            fetchAndDisplayLocation();

            editProfileButton.setOnClickListener(v -> showEditProfileDialog());
            buttonLogout.setOnClickListener(v -> logoutUser());

        } else {
            Log.w(TAG, "User not logged in, cannot display profile info.");
            clearProfileInfo();
            buttonLogout.setEnabled(false);
            editProfileButton.setEnabled(false);
        }
    }
    
    private void fetchAndDisplayProfileInfo() {
        if (userRef == null) return;

        profileListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    DataSnapshot profileSnapshot = dataSnapshot.child("profile");
                    if (profileSnapshot.exists()) {
                        try {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> profileMap = (Map<String, Object>) profileSnapshot.getValue();
                            if (profileMap != null) {
                                userProfileData = new HashMap<>(profileMap);

                                String name = Objects.toString(profileMap.get("name"), "Not Set");
                                String dob = Objects.toString(profileMap.get("dob"), "Not Set");
                                String bloodGroup = Objects.toString(profileMap.get("bloodGroup"), "Not Set");

                                profileNameTextView.setText(name);
                                profileDobTextView.setText(dob);
                                profileBloodTextView.setText(bloodGroup);

                                editProfileButton.setText("Edit Profile");
                                editProfileButton.setEnabled(true);
                            } else {
                                Log.w(TAG, "Profile data map is null.");
                                userProfileData = null;
                                clearProfileDetails();
                                editProfileButton.setText("Add Profile Info");
                                editProfileButton.setEnabled(true);
                            }

                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing profile data map.", e);
                            userProfileData = null;
                            clearProfileDetails();
                            editProfileButton.setText("Add Profile Info");
                            editProfileButton.setEnabled(true);
                        }

                    } else {
                        Log.w(TAG, "Profile node does not exist.");
                        userProfileData = null;
                        clearProfileDetails();
                        editProfileButton.setText("Add Profile Info");
                        editProfileButton.setEnabled(true);
                    }

                    DataSnapshot fcmSnapshot = dataSnapshot.child("fcm_token");
                    if (fcmSnapshot.exists() && fcmSnapshot.getValue() != null) {
                        String token = fcmSnapshot.getValue(String.class);
                        textViewFcmToken.setText(token != null ? token : "Not available");
                    } else {
                        Log.w(TAG, "FCM token not found in database.");
                        textViewFcmToken.setText("Not available");
                    }

                } else {
                    Log.w(TAG, "User node does not exist.");
                    userProfileData = null;
                    clearProfileInfo();
                    editProfileButton.setText("Add Profile Info");
                    editProfileButton.setEnabled(true);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to read user data.", databaseError.toException());
                textViewFcmToken.setText("Error loading");
                textViewLatestLocation.setText("Error loading");
                profileNameTextView.setText("Error");
                profileDobTextView.setText("Error");
                profileBloodTextView.setText("Error");
                editProfileButton.setEnabled(false);
                userProfileData = null;
                if (getContext() != null) {
                     Toast.makeText(getContext(), "Failed to load profile data.", Toast.LENGTH_SHORT).show();
                }
            }
        };
        userRef.addValueEventListener(profileListener);
    }

    private void fetchAndDisplayLocation() {
        if (userLocationRef == null) return;

        locationListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    DataSnapshot locationSnapshot = dataSnapshot.child("l");
                    if (locationSnapshot.exists()) {
                        try {
                            @SuppressWarnings("unchecked")
                            List<Object> locationList = (List<Object>) locationSnapshot.getValue();

                            if (locationList != null && locationList.size() == 2) {
                                Double lat = convertToDouble(locationList.get(0));
                                Double lng = convertToDouble(locationList.get(1));

                                if (lat != null && lng != null) {
                                    String locationString = String.format(Locale.US, "Lat: %.6f, Lng: %.6f", lat, lng);
                                    textViewLatestLocation.setText(locationString);
                                } else {
                                    Log.w(TAG, "Location list items are not valid numbers.");
                                    textViewLatestLocation.setText("Invalid location data");
                                }

                            } else {
                                Log.w(TAG, "Location list ('l') format is incorrect.");
                                textViewLatestLocation.setText("Location data format error");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing location data list.", e);
                            textViewLatestLocation.setText("Error parsing location");
                        }
                    } else {
                        Log.w(TAG, "Location list ('l') node not found under user location.");
                        textViewLatestLocation.setText("Not available");
                    }
                } else {
                    Log.w(TAG, "User location not found in database.");
                    textViewLatestLocation.setText("Not available");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to read user location.", databaseError.toException());
                textViewLatestLocation.setText("Error loading location");
                 if (getContext() != null) {
                     Toast.makeText(getContext(), "Failed to load location.", Toast.LENGTH_SHORT).show();
                 }
            }
        };
        userLocationRef.addValueEventListener(locationListener);
    }

    private Double convertToDouble(Object number) {
        if (number instanceof Double) {
            return (Double) number;
        } else if (number instanceof Long) {
            return ((Long) number).doubleValue();
        } else if (number instanceof String) {
            try {
                return Double.parseDouble((String) number);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private void showEditProfileDialog() {
        if (getContext() == null || currentUserId == null) {
            Log.e(TAG, "Cannot show dialog, context or user ID is null.");
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_edit_profile, null);

        final TextInputEditText editTextName = dialogView.findViewById(R.id.editTextName);
        final TextInputEditText editTextDob = dialogView.findViewById(R.id.editTextDob);
        final TextInputEditText editTextBloodGroup = dialogView.findViewById(R.id.editTextBloodGroup);

        boolean isEditMode = (userProfileData != null);
        builder.setTitle(isEditMode ? "Edit Profile Info" : "Add Profile Info");

        if (isEditMode) {
            editTextName.setText(Objects.toString(userProfileData.get("name"), ""));
            editTextDob.setText(Objects.toString(userProfileData.get("dob"), ""));
            editTextBloodGroup.setText(Objects.toString(userProfileData.get("bloodGroup"), ""));
        }

        builder.setView(dialogView)
                .setPositiveButton("Save", (dialog, id) -> {
                    String name = editTextName.getText() != null ? editTextName.getText().toString().trim() : "";
                    String dob = editTextDob.getText() != null ? editTextDob.getText().toString().trim() : "";
                    String bloodGroup = editTextBloodGroup.getText() != null ? editTextBloodGroup.getText().toString().trim() : "";

                    if (TextUtils.isEmpty(name)) {
                        Toast.makeText(getContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    saveProfileData(name, dob, bloodGroup);
                    dialog.dismiss();

                })
                .setNegativeButton("Cancel", (dialog, id) -> {
                    dialog.cancel();
                });

        builder.create().show();
    }

    private void saveProfileData(String name, String dob, String bloodGroup) {
        if (userRef == null) {
            Log.e(TAG, "User reference is null, cannot save profile.");
            Toast.makeText(getContext(), "Error: Cannot save profile.", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference profileRef = userRef.child("profile");

        Map<String, Object> profileUpdates = new HashMap<>();
        profileUpdates.put("name", name);
        profileUpdates.put("dob", dob);
        profileUpdates.put("bloodGroup", bloodGroup);

        profileRef.setValue(profileUpdates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Profile data saved successfully.");
                    Toast.makeText(getContext(), "Profile saved", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save profile data.", e);
                    Toast.makeText(getContext(), "Error saving profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void logoutUser() {
        if (mAuth != null) {
            mAuth.signOut();
            if (getActivity() != null) {
                Intent intent = new Intent(getActivity(), MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                getActivity().finish();
            }
        }
    }

    private void clearProfileInfo() {
        profileNameTextView.setText("N/A");
        profileEmailTextView.setText("N/A");
        profileDobTextView.setText("Not Set");
        profileBloodTextView.setText("Not Set");
        textViewFcmToken.setText("N/A");
        textViewLatestLocation.setText("N/A");
    }

    private void clearProfileDetails() {
        profileNameTextView.setText("N/A");
        profileDobTextView.setText("Not Set");
        profileBloodTextView.setText("Not Set");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (userRef != null && profileListener != null) {
            userRef.removeEventListener(profileListener);
        }
        if (userLocationRef != null && locationListener != null) {
            userLocationRef.removeEventListener(locationListener);
        }
        userProfileData = null;
    }
}