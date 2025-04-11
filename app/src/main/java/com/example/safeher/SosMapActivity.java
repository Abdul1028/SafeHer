package com.example.safeher;

import androidx.annotation.NonNull;
// import androidx.appcompat.app.AppCompatActivity; // Use FragmentActivity for maps
import androidx.fragment.app.FragmentActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
// import com.example.safeher.databinding.ActivitySosMapBinding; // Optional: Use ViewBinding if set up

// Ensure class extends FragmentActivity for SupportMapFragment
public class SosMapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private static final String TAG = "SosMapActivity";
    private double sosLat = 0.0;
    private double sosLng = 0.0;
    private static final float DEFAULT_ZOOM = 15f;
    private Button navigateButton;

    // private ActivitySosMapBinding binding; // Optional: ViewBinding

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "---- SosMapActivity onCreate START ----");
        Toast.makeText(this, "Map Activity Launched", Toast.LENGTH_SHORT).show();

        // binding = ActivitySosMapBinding.inflate(getLayoutInflater()); // Optional: ViewBinding
        // setContentView(binding.getRoot()); // Optional: ViewBinding
        try {
            Log.d(TAG, "Setting content view: R.layout.activity_sos_map");
            setContentView(R.layout.activity_sos_map); // Use standard setContentView
        } catch (Exception e) {
            Log.e(TAG, "Error setting content view!", e);
            Toast.makeText(this, "Error loading map layout!", Toast.LENGTH_LONG).show();
            finish(); // Exit if layout fails
            return;
        }

        navigateButton = findViewById(R.id.navigateButton); // Ensure this ID exists in your layout
        Log.d(TAG, "Found navigateButton: " + (navigateButton != null));

        // Get coordinates from intent
        Log.d(TAG, "Attempting to get coordinates from intent.");
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("sos_alert_lat") && intent.hasExtra("sos_alert_lng")) {
            sosLat = intent.getDoubleExtra("sos_alert_lat", 0.0);
            sosLng = intent.getDoubleExtra("sos_alert_lng", 0.0);
            Log.i(TAG, "Successfully received SOS coordinates: Lat=" + sosLat + ", Lng=" + sosLng);
            Toast.makeText(this, "SOS Location Received", Toast.LENGTH_SHORT).show();

            // Enable navigation button only if coordinates are valid
            if (navigateButton != null) {
                navigateButton.setOnClickListener(v -> launchNavigation());
                navigateButton.setEnabled(true);
                Log.d(TAG, "Navigate button enabled.");
            } else {
                Log.w(TAG, "Navigate button is null, cannot set listener.");
            }

        } else {
            Log.e(TAG, "SOS coordinates not found in intent extras! Cannot display location.");
            Toast.makeText(this, "Error: Could not load SOS location data.", Toast.LENGTH_LONG).show();
            if (navigateButton != null) {
                navigateButton.setEnabled(false);
                 Log.d(TAG, "Navigate button disabled due to missing coordinates.");
            }
            // Decide if you want to finish() or just show a map without a marker
            // finish();
            // return; // Stop map loading if no coordinates?
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        Log.d(TAG, "Attempting to find SupportMapFragment with ID: R.id.map");
        try {
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map);
            if (mapFragment != null) {
                Log.d(TAG, "SupportMapFragment found. Calling getMapAsync.");
                mapFragment.getMapAsync(this);
            } else {
                Log.e(TAG, "SupportMapFragment not found! Map will not load.");
                Toast.makeText(this, "Error initializing map view.", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
             Log.e(TAG, "Error finding or initializing SupportMapFragment!", e);
             Toast.makeText(this, "Critical map initialization error!", Toast.LENGTH_LONG).show();
        }
        Log.d(TAG, "---- SosMapActivity onCreate END ----");
    }

    private void launchNavigation() {
        Log.d(TAG, "---- launchNavigation START ----");
        // Ensure we have valid coordinates
        if (sosLat == 0.0 && sosLng == 0.0) {
            Log.w(TAG, "Navigation attempt with invalid coordinates (0,0). Aborting.");
            Toast.makeText(this, "Cannot navigate: Invalid location (0,0).", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + sosLat + "," + sosLng);
        Log.d(TAG, "Creating navigation intent with URI: " + gmmIntentUri);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");

        try {
             if (mapIntent.resolveActivity(getPackageManager()) != null) {
                 Log.i(TAG, "Launching Google Maps navigation.");
                 Toast.makeText(this, "Starting Google Maps Navigation...", Toast.LENGTH_SHORT).show();
                 startActivity(mapIntent);
             } else {
                 Log.w(TAG, "Google Maps app not found. Cannot launch navigation.");
                 Toast.makeText(this, "Google Maps app not installed.", Toast.LENGTH_LONG).show();
                 // Fallback? Open in browser?
                 // Uri webIntentUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + sosLat + "," + sosLng);
                 // Intent webIntent = new Intent(Intent.ACTION_VIEW, webIntentUri);
                 // startActivity(webIntent);
             }
        } catch (Exception e) {
            Log.e(TAG, "Error launching navigation intent", e);
            Toast.makeText(this, "Error starting navigation.", Toast.LENGTH_SHORT).show();
        }
        Log.d(TAG, "---- launchNavigation END ----");
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        Log.i(TAG, "---- onMapReady START ----");
        Toast.makeText(this, "Map Ready", Toast.LENGTH_SHORT).show();

        if (sosLat != 0.0 || sosLng != 0.0) {
            Log.d(TAG, "Valid coordinates found. Adding marker and moving camera.");
            LatLng sosLocation = new LatLng(sosLat, sosLng);
            mMap.addMarker(new MarkerOptions().position(sosLocation).title("SOS Location"));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sosLocation, DEFAULT_ZOOM));
            Log.d(TAG, "Marker added and camera moved to: " + sosLocation);
        } else {
            Log.w(TAG, "SOS coordinates are invalid or missing in onMapReady. Cannot place marker.");
            Toast.makeText(this, "Warning: Invalid location for marker.", Toast.LENGTH_SHORT).show();
        }
         Log.d(TAG, "---- onMapReady END ----");
    }
} 