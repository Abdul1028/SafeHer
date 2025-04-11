package com.example.safeher;

import android.os.Bundle;
import android.widget.Toast;
import android.widget.TextView;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import java.util.Locale;

// Firebase Imports
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.google.android.material.card.MaterialCardView;

import java.util.Map;
import java.util.Objects;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link HomeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HomeFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private static final String TAG = "HomeFragment";
    private static final String EMERGENCY_NUMBER = "112";

    // Define helpline numbers
    private static final String WOMEN_HELPLINE = "1091"; // Or "181"
    private static final String POLICE_HELPLINE = "112"; // Or "100"
    private static final String CHILD_HELPLINE = "1098";
    private static final String CYBER_HELPLINE = "1930";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    // UI Elements
    private TextView welcomeText;
    // ... other UI elements as needed ...

    private Location latestLocation = null; // Store the latest location

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference userProfileRef;
    private ValueEventListener profileListener;

    public HomeFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment HomeFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static HomeFragment newInstance(String param1, String param2) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        mAuth = FirebaseAuth.getInstance(); // Initialize Firebase Auth
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Find UI Elements
        welcomeText = view.findViewById(R.id.welcomeText);
        // textLocationValue = view.findViewById(R.id.textLocationValue); // Removed this line

        // Find Helpline Cards
        MaterialCardView cardWomenHelpline = view.findViewById(R.id.cardWomenHelpline);
        MaterialCardView cardPolice = view.findViewById(R.id.cardPolice);
        MaterialCardView cardChildHelpline = view.findViewById(R.id.cardChildHelpline);
        MaterialCardView cardCyberCrime = view.findViewById(R.id.cardCyberCrime);

        // Find Feature Cards
        MaterialCardView cardEmergencyCall = view.findViewById(R.id.cardEmergencyCall);
        MaterialCardView cardCurrentLocation = view.findViewById(R.id.cardCurrentLocation);
        MaterialCardView cardSafestRoute = view.findViewById(R.id.cardSafestRoute);
        MaterialCardView cardHotspots = view.findViewById(R.id.cardHotspots);
        MaterialCardView cardForum = view.findViewById(R.id.cardForum);
        MaterialCardView cardFakeCall = view.findViewById(R.id.cardFakeCall);

        // Fetch user profile
        fetchUserProfile();

        // Setup Click Listeners for Helpline cards
        setupHelplineClickListeners(cardWomenHelpline, cardPolice, cardChildHelpline, cardCyberCrime);

        // Setup Click Listeners for Feature cards
        setupFeatureClickListeners(cardEmergencyCall, cardCurrentLocation, cardSafestRoute, cardHotspots, cardForum, cardFakeCall);

        // Remove initial location update as the TextView is removed
        // updateLocationDisplay();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Remove location update
        // updateLocationDisplay();
    }

    @Override
    public void onStop() {
        super.onStop();
        // Detach Firebase listener when fragment is not visible
        if (userProfileRef != null && profileListener != null) {
            userProfileRef.removeEventListener(profileListener);
        }
    }

    private void fetchUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            userProfileRef = FirebaseDatabase.getInstance().getReference("users").child(userId).child("profile");

            if (profileListener != null) {
                userProfileRef.removeEventListener(profileListener); // Remove previous listener
            }

            profileListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        String name = dataSnapshot.child("name").getValue(String.class);
                        if (name != null && !name.isEmpty()) {
                            welcomeText.setText("Hello " + name + "!");
                            Log.d(TAG, "Welcome message updated for user: " + name);
                        } else {
                            welcomeText.setText("Hello!"); // Fallback if name is not set
                            Log.d(TAG, "User name not found in profile, using default welcome.");
                        }
                    } else {
                        welcomeText.setText("Hello!"); // Fallback if profile node doesn't exist
                        Log.d(TAG, "Profile node not found, using default welcome.");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e(TAG, "Failed to read user profile.", databaseError.toException());
                    welcomeText.setText("Hello!"); // Fallback on error
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to load user name.", Toast.LENGTH_SHORT).show();
                    }
                }
            };
            // Use addListenerForSingleValueEvent if you only need to load it once per view creation
            // Use addValueEventListener if you want real-time updates (e.g., name changes elsewhere)
            userProfileRef.addListenerForSingleValueEvent(profileListener);
            // userProfileRef.addValueEventListener(profileListener); // Use this if you need real-time name updates
        } else {
            welcomeText.setText("Hello Guest!"); // User not logged in
            Log.w(TAG, "User not logged in, showing guest welcome.");
        }
    }

    // Helper for Helpline Click Listeners
    private void setupHelplineClickListeners(MaterialCardView cardWomen, MaterialCardView cardPolice, MaterialCardView cardChild, MaterialCardView cardCyber) {
        if (cardWomen != null) {
            cardWomen.setOnClickListener(v -> dialHelpline(WOMEN_HELPLINE));
        }
        if (cardPolice != null) {
            cardPolice.setOnClickListener(v -> dialHelpline(POLICE_HELPLINE));
        }
        if (cardChild != null) {
            cardChild.setOnClickListener(v -> dialHelpline(CHILD_HELPLINE));
        }
        if (cardCyber != null) {
            cardCyber.setOnClickListener(v -> dialHelpline(CYBER_HELPLINE));
        }
    }

    // Renamed previous setupClickListeners
    private void setupFeatureClickListeners(MaterialCardView cardEmergencyCall, MaterialCardView cardCurrentLocation, MaterialCardView cardSafestRoute, MaterialCardView cardHotspots, MaterialCardView cardForum, MaterialCardView cardFakeCall) {
        // Emergency Call Card (Still uses the generic EMERGENCY_NUMBER, could be changed to POLICE_HELPLINE)
        if (cardEmergencyCall != null) {
            cardEmergencyCall.setOnClickListener(v -> dialHelpline(EMERGENCY_NUMBER)); // Consider using POLICE_HELPLINE here too
        }

        // Current Location Listener (Unchanged)
        if (cardCurrentLocation != null) {
             cardCurrentLocation.setOnClickListener(v -> {
                Log.d(TAG, "Current Location card clicked.");
                Location currentLocation = null;
                if (getActivity() instanceof DashBoardActivity) {
                    currentLocation = ((DashBoardActivity) getActivity()).getLatestLocation();
                }
                if (currentLocation != null) {
                    String geoUriString = String.format(Locale.US, "geo:%f,%f?q=%f,%f(Current Location)",
                            currentLocation.getLatitude(), currentLocation.getLongitude(),
                            currentLocation.getLatitude(), currentLocation.getLongitude());
                    Uri gmmIntentUri = Uri.parse(geoUriString);
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                    if (mapIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
                        Log.i(TAG, "Launching map to show location: " + geoUriString);
                        startActivity(mapIntent);
                    } else {
                        Log.w(TAG, "No map app found to handle geo intent.");
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "No map application found.", Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    Log.w(TAG, "Current Location card clicked, but location is not available yet.");
                     if (getContext() != null) {
                        Toast.makeText(getContext(), "Location not available yet.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        // Listeners for other feature cards (Toast only)
        if (cardSafestRoute != null) {
            cardSafestRoute.setOnClickListener(v -> Toast.makeText(getContext(), "Safest Route Clicked", Toast.LENGTH_SHORT).show());
        }
        if (cardHotspots != null) {
            cardHotspots.setOnClickListener(v -> Toast.makeText(getContext(), "Hotspots Clicked", Toast.LENGTH_SHORT).show());
        }
        if (cardForum != null) {
            cardForum.setOnClickListener(v -> Toast.makeText(getContext(), "Forum Clicked", Toast.LENGTH_SHORT).show());
        }
        if (cardFakeCall != null) {
            cardFakeCall.setOnClickListener(v -> {
                Log.d(TAG, "Fake Call card clicked.");
                Intent fakeCallIntent = new Intent(getActivity(), FakeCallActivity.class);
                // TODO: Optionally add extras like caller name/image later
                // fakeCallIntent.putExtra("CALLER_NAME", "Work");
                startActivity(fakeCallIntent);
            });
        }
    }

    // Helper method to dial a number
    private void dialHelpline(String phoneNumber) {
        Log.d(TAG, "Dialing helpline: " + phoneNumber);
        Intent dialIntent = new Intent(Intent.ACTION_DIAL);
        dialIntent.setData(Uri.parse("tel:" + phoneNumber));
        if (dialIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivity(dialIntent);
        } else {
            Log.w(TAG, "No activity found to handle ACTION_DIAL for number: " + phoneNumber);
            if (getContext() != null) {
                Toast.makeText(getContext(), "Could not open dialer app.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}