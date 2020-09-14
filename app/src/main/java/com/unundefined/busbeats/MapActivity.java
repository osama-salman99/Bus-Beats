package com.unundefined.busbeats;

import android.Manifest;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;

import java.util.Arrays;
import java.util.List;

import static android.content.ContentValues.TAG;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final int LOCATION_REQUEST_CODE = 1;
    private static final int AUTOCOMPLETE_REQUEST_CODE = 2;
    private static final List<Place.Field> FIELDS = Arrays.asList(
            Place.Field.ID,
            Place.Field.NAME,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG);
    private SearchView searchView;
    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

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

    private void setUpSearch() {
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = findViewById(R.id.search_view);
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

    private void callSearchIntent() {
        clearSearchFocus();
        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, FIELDS)
                .setCountry("JO")
                .build(getApplicationContext());
        startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE);
    }

    private void clearSearchFocus() {
        searchView.setQuery(null, false);
        searchView.clearFocus();
    }
}
