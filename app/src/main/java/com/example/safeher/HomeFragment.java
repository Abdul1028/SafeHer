package com.example.safeher;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.location.Location;
import java.util.Locale;

import com.google.android.material.card.MaterialCardView;

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

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

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

        // Find the card views by their IDs
        MaterialCardView cardEmergencyCall = view.findViewById(R.id.cardEmergencyCall);
        MaterialCardView cardCurrentLocation = view.findViewById(R.id.cardCurrentLocation);
        MaterialCardView cardSafestRoute = view.findViewById(R.id.cardSafestRoute);
        MaterialCardView cardHotspots = view.findViewById(R.id.cardHotspots);
        MaterialCardView cardForum = view.findViewById(R.id.cardForum);
        MaterialCardView cardFakeCall = view.findViewById(R.id.cardFakeCall);

        // Set OnClick Listeners
        if (cardEmergencyCall != null) {
            cardEmergencyCall.setOnClickListener(v -> {
                Log.d(TAG, "Emergency Call card clicked.");
                Log.d(TAG, "emergency number: "+EMERGENCY_NUMBER);

                Intent dialIntent = new Intent(Intent.ACTION_DIAL);
                dialIntent.setData(Uri.parse("tel:" + EMERGENCY_NUMBER));

                if (dialIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
                    Log.i(TAG, "Launching dialer with number: " + EMERGENCY_NUMBER);
                    startActivity(dialIntent);
                } else {
                    Log.w(TAG, "No activity found to handle ACTION_DIAL.");
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Could not open dialer app.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        if (cardCurrentLocation != null) {
            cardCurrentLocation.setOnClickListener(v -> {
                Log.d(TAG, "Current Location card clicked.");
                Location currentLocation = null;
                // Get location from parent activity
                if (getActivity() instanceof DashBoardActivity) {
                    currentLocation = ((DashBoardActivity) getActivity()).getLatestLocation();
                }

                if (currentLocation != null) {
                    // Create Uri for map intent with marker
                    // Format: geo:lat,lng?q=lat,lng(Label)
                    String geoUriString = String.format(Locale.US, "geo:%f,%f?q=%f,%f(Current Location)",
                            currentLocation.getLatitude(), currentLocation.getLongitude(),
                            currentLocation.getLatitude(), currentLocation.getLongitude());
                    Uri gmmIntentUri = Uri.parse(geoUriString);

                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                    // Optional: Attempt to specifically use Google Maps
                    // mapIntent.setPackage("com.google.android.apps.maps");

                    if (mapIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
                        Log.i(TAG, "Launching map to show location: " + geoUriString);
                        startActivity(mapIntent);
                    } else {
                        Log.w(TAG, "No map app found to handle geo intent.");
                        Toast.makeText(getContext(), "No map application found.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.w(TAG, "Current Location card clicked, but location is not available yet.");
                    Toast.makeText(getContext(), "Location not available yet.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (cardSafestRoute != null) {
            cardSafestRoute.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Safest Route Clicked", Toast.LENGTH_SHORT).show();
                // TODO: Implement safest route feature later
            });
        }

        if (cardHotspots != null) {
            cardHotspots.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Hotspots Clicked", Toast.LENGTH_SHORT).show();
                // TODO: Implement hotspots feature later
            });
        }

        if (cardForum != null) {
            cardForum.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Forum Clicked", Toast.LENGTH_SHORT).show();
                // TODO: Implement forum feature later
            });
        }

        if (cardFakeCall != null) {
            cardFakeCall.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Fake Call Clicked", Toast.LENGTH_SHORT).show();
                // TODO: Implement fake call feature later
            });
        }
    }
}