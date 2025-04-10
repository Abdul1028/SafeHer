package com.example.safeher;

import androidx.annotation.NonNull;
// import androidx.appcompat.app.AppCompatActivity; // Use FragmentActivity for maps
import androidx.fragment.app.FragmentActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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

    // private ActivitySosMapBinding binding; // Optional: ViewBinding

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // binding = ActivitySosMapBinding.inflate(getLayoutInflater()); // Optional: ViewBinding
        // setContentView(binding.getRoot()); // Optional: ViewBinding
        setContentView(R.layout.activity_sos_map); // Use standard setContentView

        // Get coordinates from intent
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("sos_alert_lat") && intent.hasExtra("sos_alert_lng")) {
            sosLat = intent.getDoubleExtra("sos_alert_lat", 0.0);
            sosLng = intent.getDoubleExtra("sos_alert_lng", 0.0);
            Log.d(TAG, "Received SOS coordinates: Lat=" + sosLat + ", Lng=" + sosLng);
        } else {
            Log.e(TAG, "SOS coordinates not found in intent extras!");
            Toast.makeText(this, "Error: Could not load SOS location.", Toast.LENGTH_LONG).show();
            // Handle error, maybe finish activity?
            // finish();
            // return; // Don't proceed to load map if no coords
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        // Use getSupportFragmentManager() from FragmentActivity
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Log.e(TAG, "SupportMapFragment not found!");
            Toast.makeText(this, "Error initializing map.", Toast.LENGTH_SHORT).show();
        }
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
        Log.d(TAG, "Map is ready.");

        if (sosLat != 0.0 || sosLng != 0.0) { // Check if coords were actually received
            // Add a marker at the SOS location and move the camera
            LatLng sosLocation = new LatLng(sosLat, sosLng);
            mMap.addMarker(new MarkerOptions().position(sosLocation).title("SOS Location"));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sosLocation, DEFAULT_ZOOM));
            Log.d(TAG, "Marker added and camera moved to SOS location.");

            // Optional: Add other map configurations
            // mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
            // mMap.getUiSettings().setZoomControlsEnabled(true);
        } else {
            Log.w(TAG, "SOS coordinates are invalid or missing, cannot place marker accurately.");
            // Optionally show a default location or just log the warning
            // Example: Move camera to a default location
            // LatLng defaultLocation = new LatLng(0, 0); // Or a sensible default
            // mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 3f));
        }
    }
} 