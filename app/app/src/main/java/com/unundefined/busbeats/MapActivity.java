package com.unundefined.busbeats;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.unundefined.busbeats.transportation.Bus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.content.ContentValues.TAG;

public class MapActivity extends AppCompatActivity
        implements OnMapReadyCallback, OnBusReadyCallback {
    public static final String SIGNED_IN_KEY = "com.unundefined.busbeats.SIGNED_IN";
    private static final int LOCATION_REQUEST_CODE = 1;
    private static final int AUTOCOMPLETE_REQUEST_CODE = 2;
    private static final List<Place.Field> FIELDS = Arrays.asList(
            Place.Field.ID,
            Place.Field.NAME,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG);
    public static SharedPreferences sharedPreferences;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private GoogleMap mMap;
    private boolean inForeground;
    private boolean cameraAnimating;
    private Thread keepFollowingThread;
    private boolean keepFollowing;
    private SearchView searchView;
    private Trip currentTrip;
    private ArrayList<Polyline> polylines;
    private ArrayList<Bus> buses;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        sharedPreferences = getPreferences(MODE_PRIVATE);

        boolean signedIn = sharedPreferences.getBoolean(SIGNED_IN_KEY, false);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Log.d(TAG, "onCreate: " + signedIn + " " + Auth.getFirebaseAuth().getCurrentUser());
        if (!signedIn || Auth.getFirebaseAuth().getCurrentUser() == null) {
            editor.putBoolean(SIGNED_IN_KEY, false);
            editor.apply();
            goToLogInScreen();
            return;
        }

        User.setUpUserFromFirebase();
        setUpAppBar();

        Places.initialize(getApplicationContext(), getResources().getString(R.string.google_cloud_api_key));
        Places.createClient(getApplicationContext());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment == null) {
            Log.w(TAG, "onCreate: mapFragment is null");
            return;
        }
        mapFragment.getMapAsync(this);
        setUpSearch();

        currentTrip = new Trip(this);
        polylines = new ArrayList<>();

        fetchVehicles();
        setUpMarkerListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        overridePendingTransition(0, 0);

        Button previousRidesButton = findViewById(R.id.previous_rides_button);
        Button mapButton = findViewById(R.id.map_button);
        Button settingsButton = findViewById(R.id.settings_button);
        int color;
        Drawable drawable;

        // Previous rides button
        color = ContextCompat.getColor(getApplicationContext(), R.color.colorInactive);
        previousRidesButton.setTextColor(color);
        drawable = previousRidesButton.getCompoundDrawables()[1];
        drawable.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        previousRidesButton.setCompoundDrawables(null, drawable, null, null);

        // Map button
        color = ContextCompat.getColor(getApplicationContext(), R.color.colorActive);
        mapButton.setTextColor(color);
        drawable = mapButton.getCompoundDrawables()[1];
        drawable.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        mapButton.setCompoundDrawables(null, drawable, null, null);

        // Settings button
        color = ContextCompat.getColor(getApplicationContext(), R.color.colorInactive);
        settingsButton.setTextColor(color);
        drawable = settingsButton.getCompoundDrawables()[1];
        drawable.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        settingsButton.setCompoundDrawables(null, drawable, null, null);
    }

    private void setUpMarkerListeners() {
        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                for (Bus bus : buses) {
                    if (bus.getMarker().equals(marker)) {
                        showBusRoute(bus);
                        return;
                    }
                }
            }
        });
        mMap.setOnInfoWindowCloseListener(new GoogleMap.OnInfoWindowCloseListener() {
            @Override
            public void onInfoWindowClose(Marker marker) {
                for (Bus bus : buses) {
                    bus.removeMapPolyline();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            if (data == null) {
                Log.w(TAG, "onActivityResult: Autocomplete request data is null");
                return;
            }
            if (resultCode == RESULT_OK) {
                Place place = Autocomplete.getPlaceFromIntent(data);
                Log.i(TAG, "Place: " + place.getName() + ", " + place.getLatLng());
                float zoom = Math.max(mMap.getCameraPosition().zoom, 15.6F); // Constant
                animateCamera(place.getLatLng(), zoom);
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                Status status = Autocomplete.getStatusFromIntent(data);
                //noinspection ConstantConditions
                Log.w(TAG, status.getStatusMessage());
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        inForeground = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        inForeground = true;
        if (keepFollowingThread != null && !keepFollowingThread.isAlive()) {
            startCameraFollowingThread();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQUEST_CODE) {
            boolean granted = false;
            for (int i = 0; i < permissions.length; i++) {
                if (permissions[i].equals(Manifest.permission.ACCESS_COARSE_LOCATION) ||
                        permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    granted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
                }
            }
            if (granted) {
                onMapReady(mMap);
            } else {
                Toast.makeText(getApplicationContext(),
                        "Application requires location permission in order to work",
                        Toast.LENGTH_LONG)
                        .show();
                finish();
            }
        }
    }

    @Override
    public void onBackPressed() {
        clearSearchFocus();
        overridePendingTransition(0, 0);
        if (currentTrip.getState() != Trip.START_LOCATION) {
            currentTrip.goToPreviousState();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                clearSearchFocus();
            }
        });
        if (ActivityCompat
                .checkSelfPermission(getApplicationContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat
                .checkSelfPermission(getApplicationContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_REQUEST_CODE);
            return;
        }
        cameraAnimating = false;
        fusedLocationProviderClient =
                new FusedLocationProviderClient(getApplicationContext());
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        updateCameraToCurrentLocation();
        setUpMapButtons();
        keepFollowing = true;
        mMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onCameraMove() {
                fusedLocationProviderClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            Location myLocation = task.getResult();
                            if (myLocation == null) {
                                Log.w(TAG, "onCameraMove: myLocation is null");
                                return;
                            }
                            LatLng latLng =
                                    new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
                            ImageButton myLocationButton = findViewById(R.id.my_location_button);
                            int color;
                            if (distance(mMap.getCameraPosition().target, latLng)
                                    < 0.00621371) { // Constant: 100 meters (in miles)
                                keepFollowing = true;
                                color = ContextCompat.getColor(getApplicationContext(),
                                        R.color.colorActive);
                            } else {
                                keepFollowing = false;
                                color = ContextCompat.getColor(getApplicationContext(),
                                        R.color.colorInactive);
                            }
                            myLocationButton.setColorFilter(color);
                        }
                    }
                });
            }
        });
        startCameraFollowingThread();
    }

    @SuppressLint("MissingPermission")
    private void updateCameraToCurrentLocation() {
        fusedLocationProviderClient.getLastLocation().addOnCompleteListener(
                new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            Location myLocation = task.getResult();
                            if (myLocation == null) {
                                Log.w(TAG, "onCameraMove: myLocation is null");
                                return;
                            }
                            LatLng myLocationLatLng =
                                    new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
                            float zoom = Math.max(mMap.getCameraPosition().zoom, 14.6F); // Constant
                            animateCamera(myLocationLatLng, zoom);
                            Log.d(TAG, "onComplete: Location is " + myLocationLatLng);
                        } else {
                            Log.w(TAG, "onComplete: Could not fetch location data");
                        }
                    }
                });
    }

    private void animateCamera(LatLng location, float zoom) {
        cameraAnimating = true;
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, zoom),
                new GoogleMap.CancelableCallback() {
                    @Override
                    public void onFinish() {
                        cameraAnimating = false;
                    }

                    @Override
                    public void onCancel() {
                        cameraAnimating = false;
                    }
                });
    }

    private void setUpMapButtons() {
        findViewById(R.id.my_location_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearSearchFocus();
                updateCameraToCurrentLocation();
            }
        });
        final Button goButton = findViewById(R.id.go_button);
        goButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                choose(mMap.getCameraPosition().target);
            }
        });
        findViewById(R.id.start_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goButton.callOnClick();
            }
        });
    }

    private void goToLogInScreen() {
        startActivity(new Intent(getApplicationContext(), LoginActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
        finish();
    }

    private void setUpAppBar() {
        findViewById(R.id.previous_rides_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearSearchFocus();
                startActivity(new Intent(getApplicationContext(), PreviousRidesActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                overridePendingTransition(0, 0);
            }
        });
        findViewById(R.id.settings_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearSearchFocus();
                startActivity(new Intent(getApplicationContext(), SettingsActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                overridePendingTransition(0, 0);
            }
        });
    }

    private void startCameraFollowingThread() {
        keepFollowingThread = new Thread(new Runnable() {
            @Override
            public synchronized void run() {
                while (inForeground) {
                    if (keepFollowing) {
                        if (!cameraAnimating) {
                            updateCameraToCurrentLocation();
                        }
                        try {
                            wait(500); // Const: Every half a second
                            Log.d(TAG, "keepFollowing thread: Following myLocation");
                        } catch (InterruptedException exception) {
                            Log.w(TAG, "keepFollowing thread: " + exception.getMessage()
                                    , exception);
                        }
                    }
                }
            }
        });
        keepFollowingThread.start();
    }

    // Returns distance between two locations in miles
    private double distance(LatLng latLng1, LatLng latLng2) {
        double earthRadius = 3958.75; // in miles, change to 6371 for kilometer output
        double lat1 = latLng1.latitude;
        double lng1 = latLng1.longitude;
        double lat2 = latLng2.latitude;
        double lng2 = latLng2.longitude;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double sinDLat = Math.sin(dLat / 2);
        double sinDLng = Math.sin(dLng / 2);

        double a = Math.pow(sinDLat, 2) + Math.pow(sinDLng, 2)
                * Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2));

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return earthRadius * c;
    }

    private void setUpSearch() {
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = findViewById(R.id.search_bar);
        if (searchManager == null) {
            searchView.setVisibility(View.GONE);
            Log.w(TAG, "setUpSearch: Search view could not be set up");
            Toast.makeText(getApplicationContext(), "Could not load search bar",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callSearchIntent();
            }
        });
        searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    callSearchIntent();
                }
            }
        });

        Log.d(TAG, "setUpSearch: Setup done");
    }

    private void clearSearchFocus() {
        searchView.setQuery(null, false);
        searchView.clearFocus();
    }

    private void callSearchIntent() {
        clearSearchFocus();
        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, FIELDS)
                .setCountry("JO")
                .build(getApplicationContext());
        startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE);
    }

    private void choose(LatLng location) {
        int state = currentTrip.getState();
        MarkerOptions markerOptions = new MarkerOptions().position(location);
        Marker marker;
        switch (state) {
            case Trip.START_LOCATION:
                marker = mMap.addMarker(markerOptions.title("Start location"));
                currentTrip.setStartLocation(location, marker);
                marker.showInfoWindow();
                currentTrip.goToNextState();
                break;
            case Trip.DESTINATION:
                marker = mMap.addMarker(markerOptions.title("Destination"));
                currentTrip.setDestination(location, marker);
                marker.showInfoWindow();
                currentTrip.goToNextState();
                break;
            case Trip.READY:
                currentTrip.goToNextState();
                startTrip();
        }
    }

    private void startTrip() {
        // TODO:
    }

    public void addPolyline(PolylineOptions polyline) {
        polylines.add(mMap.addPolyline(polyline));
    }

    public void removePolylines() {
        for (Polyline polyline : polylines) {
            polyline.remove();
        }
        polylines = new ArrayList<>();
    }

    private void fetchVehicles() {
        buses = new ArrayList<>();
        FirebaseFirestore.getInstance()
                .collection("buses")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            QuerySnapshot querySnapshot = task.getResult();
                            if (querySnapshot == null) {
                                Log.w(TAG,
                                        "onComplete: document query snapshot result is null");
                                return;
                            }
                            List<DocumentSnapshot> documents = querySnapshot.getDocuments();
                            for (DocumentSnapshot document : documents) {
                                buses.add(new Bus(document, MapActivity.this));
                            }
                        }
                    }
                });
    }

    @Override
    public void onBusReady() {
        refreshBuses();
    }

    private void refreshBuses() {
        for (Bus bus : buses) {
            bus.removeMarker();
            Marker marker = mMap.addMarker(bus.getMarkerOptions());
            bus.updateMarker(marker);
        }
    }

    private void showBusRoute(Bus bus) {
        bus.removeMapPolyline();
        if (bus.routeReady()) {
            PolylineOptions polylineOptions = bus.getRoute()
                    .color(getResources().getColor(R.color.busColor, null));
            Polyline polyline = mMap.addPolyline(polylineOptions);
            bus.setMapPolyline(polyline);
        }
    }
}
