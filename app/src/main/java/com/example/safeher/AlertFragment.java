package com.example.safeher;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link AlertFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AlertFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private static final String TAG = "AlertFragment";

    private RecyclerView recyclerViewAlerts;
    private TextView textViewNoAlerts;
    private AlertAdapter alertAdapter;
    private List<SosAlert> alertList = new ArrayList<>();

    // Firebase References
    private Query userAlertsRef;
    private ValueEventListener alertsListener;
    private FirebaseAuth mAuth;

    public AlertFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment AlertFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static AlertFragment newInstance(String param1, String param2) {
        AlertFragment fragment = new AlertFragment();
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
        return inflater.inflate(R.layout.fragment_alert, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerViewAlerts = view.findViewById(R.id.recyclerViewAlerts);
        textViewNoAlerts = view.findViewById(R.id.textViewNoAlerts);
        mAuth = FirebaseAuth.getInstance(); // Initialize FirebaseAuth

        setupRecyclerView();
        fetchUserAlerts(); // Fetch real data now

        // Remove dummy data loader
        // loadDummyData();
    }

    private void setupRecyclerView() {
        // Pass context to adapter for clicks
        alertAdapter = new AlertAdapter(alertList, getContext());
        recyclerViewAlerts.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewAlerts.setAdapter(alertAdapter);
    }

    private void fetchUserAlerts() {
        String currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (currentUserId == null) {
            Log.w(TAG, "Cannot fetch alerts: User not logged in.");
            textViewNoAlerts.setText("Please log in to see your alerts.");
            textViewNoAlerts.setVisibility(View.VISIBLE);
            recyclerViewAlerts.setVisibility(View.GONE);
            return;
        }

        Log.d(TAG, "Fetching alerts for user: " + currentUserId);
        // Query alerts for the current user, ordered by timestamp (most recent first)
        // Note: Firebase Realtime DB ordering works best with numeric timestamps.
        // If timestamps are strings, ordering might be lexicographical.
        // For descending order (newest first), we'll reverse the list after fetching.
        userAlertsRef = FirebaseDatabase.getInstance().getReference("sos_alerts")
                                     .orderByChild("user_id")
                                     .equalTo(currentUserId);

        if (alertsListener != null) {
             // Detach from the previous query/reference if necessary
             // Need a way to store the *object* the listener was attached to.
             // For simplicity now, assuming userAlertsRef holds the correct object.
             // If you re-query often, you might need a separate variable for the object
             // the listener is currently attached to.
             if (userAlertsRef instanceof DatabaseReference) {
                ((DatabaseReference)userAlertsRef).removeEventListener(alertsListener);
             } else if (userAlertsRef instanceof Query) {
                 ((Query)userAlertsRef).removeEventListener(alertsListener);
             }
        }

        alertsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d(TAG, "Alerts data received. Count: " + dataSnapshot.getChildrenCount());
                alertList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        // Map the snapshot directly to SosAlert object
                        SosAlert alert = snapshot.getValue(SosAlert.class);
                        if (alert != null) {
                            // Set the alertId from the snapshot key
                            alert.setAlertId(snapshot.getKey());
                            alertList.add(alert);
                             Log.d(TAG, "Parsed alert: ID=" + alert.getAlertId() + ", Time=" + alert.getTimestamp());
                        } else {
                            Log.w(TAG, "Parsed alert is null for key: " + snapshot.getKey());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing alert data for key: " + snapshot.getKey(), e);
                    }
                }

                // Reverse the list to show newest first
                Collections.reverse(alertList);

                if (alertList.isEmpty()) {
                    Log.d(TAG, "No alerts found for user.");
                    textViewNoAlerts.setText("No SOS alerts initiated yet.");
                    textViewNoAlerts.setVisibility(View.VISIBLE);
                    recyclerViewAlerts.setVisibility(View.GONE);
                } else {
                    Log.d(TAG, "Updating adapter with " + alertList.size() + " alerts.");
                    textViewNoAlerts.setVisibility(View.GONE);
                    recyclerViewAlerts.setVisibility(View.VISIBLE);
                    alertAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to read alerts.", databaseError.toException());
                textViewNoAlerts.setText("Error loading alerts.");
                textViewNoAlerts.setVisibility(View.VISIBLE);
                recyclerViewAlerts.setVisibility(View.GONE);
                 if (getContext() != null) {
                     Toast.makeText(getContext(), "Error loading alerts.", Toast.LENGTH_SHORT).show();
                 }
            }
        };
        userAlertsRef.addValueEventListener(alertsListener); // Use addValueEventListener for real-time updates
    }

    // Remove dummy data method
    // private void loadDummyData() { ... }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Remove Firebase listener from the Query
        if (userAlertsRef != null && alertsListener != null) {
           Log.d(TAG, "Removing alerts listener.");
           userAlertsRef.removeEventListener(alertsListener); // Detach from Query
           alertsListener = null;
        }
    }

    // --- RecyclerView Adapter ---
    private static class AlertAdapter extends RecyclerView.Adapter<AlertAdapter.AlertViewHolder> {

        private List<SosAlert> alerts;
        private SimpleDateFormat dateFormat;
        private android.content.Context context; // Context for launching intent

        AlertAdapter(List<SosAlert> alerts, android.content.Context context) {
            this.alerts = alerts;
            this.context = context; // Store context
             // Defensive check for locale
             Locale currentLocale = context != null ? context.getResources().getConfiguration().getLocales().get(0) : Locale.getDefault();
             this.dateFormat = new SimpleDateFormat("dd MMM yyyy, hh:mm a", currentLocale);
        }

        @NonNull
        @Override
        public AlertViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_item_alert, parent, false);
            return new AlertViewHolder(view, context); // Pass context to ViewHolder
        }

        @Override
        public void onBindViewHolder(@NonNull AlertViewHolder holder, int position) {
            SosAlert alert = alerts.get(position);
            holder.bind(alert, dateFormat);
        }

        @Override
        public int getItemCount() {
            return alerts.size();
        }

        static class AlertViewHolder extends RecyclerView.ViewHolder {
            TextView timestampText;
            TextView locationText;
            Button viewMapButton;
            android.content.Context context; // Store context

            AlertViewHolder(@NonNull View itemView, android.content.Context context) {
                super(itemView);
                this.context = context; // Store context
                timestampText = itemView.findViewById(R.id.textViewAlertTimestamp);
                locationText = itemView.findViewById(R.id.textViewAlertLocation);
                viewMapButton = itemView.findViewById(R.id.buttonViewOnMap);
            }

            void bind(SosAlert alert, SimpleDateFormat formatter) {
                timestampText.setText(formatter.format(new Date(alert.getTimestamp())));
                locationText.setText(String.format(Locale.US, "Lat: %.4f, Lng: %.4f", alert.getLat(), alert.getLng()));

                viewMapButton.setOnClickListener(v -> {
                    if (context == null) return; // Safety check

                    // Create Uri for map intent with marker
                    String geoUriString = String.format(Locale.US, "geo:%f,%f?q=%f,%f(SOS @ %s)",
                            alert.getLat(), alert.getLng(),
                            alert.getLat(), alert.getLng(),
                            formatter.format(new Date(alert.getTimestamp()))); // Label with time
                    Uri gmmIntentUri = Uri.parse(geoUriString);
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);

                    // Check if an app exists to handle this map intent
                    if (mapIntent.resolveActivity(context.getPackageManager()) != null) {
                        Log.i(TAG, "Launching map for alert: " + geoUriString);
                        context.startActivity(mapIntent);
                    } else {
                        Log.w(TAG, "No map app found to handle geo intent for alert.");
                        Toast.makeText(context, "No map application found.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }

    // --- Data Model ---
    // Make sure field names match EXACTLY with Firebase keys (user_id, lat, lng, timestamp)
    private static class SosAlert {
        private String alertId; // Added to store the Firebase key
        private String user_id; // Matches Firebase key
        private double lat;
        private double lng;
        private long timestamp;

        public SosAlert() {} // Needed for Firebase

        // Constructor not strictly needed for Firebase mapping, but can be useful
        // public SosAlert(String alertId, String userId, double lat, double lng, long timestamp) { ... }

        // Getters are essential for Firebase mapping and adapter use
        public String getAlertId() { return alertId; }
        public String getUser_id() { return user_id; }
        public double getLat() { return lat; }
        public double getLng() { return lng; }
        public long getTimestamp() { return timestamp; }

        // Setter for alertId (since it's the key, not a value in the object)
        public void setAlertId(String alertId) { this.alertId = alertId; }

         // Optional Setters if needed elsewhere, but not for Firebase -> Object mapping
         // public void setUser_id(String user_id) { this.user_id = user_id; }
         // public void setLat(double lat) { this.lat = lat; }
         // public void setLng(double lng) { this.lng = lng; }
         // public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }
}