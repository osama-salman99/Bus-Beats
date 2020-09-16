package com.unundefined.busbeats;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setUpAppBar();

        View parentLayout = findViewById(android.R.id.content);
        final Snackbar mySnackbar = Snackbar.make(parentLayout, "link saved to clipboard", Snackbar.LENGTH_LONG);

        findViewById(R.id.button3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                startActivity(new Intent(getApplicationContext(), GeneralSettingsActivity.class));


            }
        });

        findViewById(R.id.button2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                startActivity(new Intent(getApplicationContext(), ProfileActivity.class));


            }
        });

        findViewById(R.id.button6).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mySnackbar.show();
            }
        });


        findViewById(R.id.button7).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), HelpActivity.class));
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

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
        color = ContextCompat.getColor(getApplicationContext(), R.color.colorInactive);
        mapButton.setTextColor(color);
        drawable = mapButton.getCompoundDrawables()[1];
        drawable.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        mapButton.setCompoundDrawables(null, drawable, null, null);

        // Settings button
        color = ContextCompat.getColor(getApplicationContext(), R.color.colorActive);
        settingsButton.setTextColor(color);
        drawable = settingsButton.getCompoundDrawables()[1];
        drawable.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        settingsButton.setCompoundDrawables(null, drawable, null, null);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(0, 0);
    }

    private void setUpAppBar() {
        findViewById(R.id.previous_rides_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), PreviousRidesActivity.class));
                overridePendingTransition(0, 0);
                finish();
            }
        });
        findViewById(R.id.map_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), MapActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                overridePendingTransition(0, 0);
                finish();
            }
        });
    }
}
