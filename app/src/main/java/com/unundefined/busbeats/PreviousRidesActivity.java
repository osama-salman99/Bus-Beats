package com.unundefined.busbeats;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class PreviousRidesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_previous_rides);

        setUpAppBar();
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
        color = ContextCompat.getColor(getApplicationContext(), R.color.colorActive);
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
        color = ContextCompat.getColor(getApplicationContext(), R.color.colorInactive);
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
        findViewById(R.id.map_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), MapActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                overridePendingTransition(0, 0);
                finish();
            }
        });
        findViewById(R.id.settings_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
                overridePendingTransition(0, 0);
                finish();
            }
        });
    }
}