package com.unundefined.busbeats.transportation;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.unundefined.busbeats.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;

import static android.content.ContentValues.TAG;

public class Bus extends Vehicle {
    private MarkerOptions markerOptions;
    private OnBusReadyCallback activity;
    private int locationIndex;

    public Bus(DocumentSnapshot document, OnBusReadyCallback activity) {
        super(document);
        this.activity = activity;
        this.markerOptions = new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.bus_icon));
    }

    public String getName() {
        return name;
    }

    public MarkerOptions getMarkerOptions() {
        return markerOptions;
    }

    @Override
    protected void fetchRoute() {
        StorageReference storageReference = FirebaseStorage.getInstance().getReference();
        final File tempFile;
        try {
            tempFile = File.createTempFile(routeId, "txt");
        } catch (IOException exception) {
            Log.w(TAG, "fetchRoute: could not fetch route for " + name);
            exception.printStackTrace();
            return;
        }
        storageReference
                .child("routes/buses/" + routeId + ".txt")
                .getFile(tempFile)
                .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                        Scanner scanner;
                        try {
                            scanner = new Scanner(tempFile);
                        } catch (FileNotFoundException exception) {
                            Log.w(TAG, "onSuccess: Error: could not initialize scanner");
                            exception.printStackTrace();
                            return;
                        }
                        PolylineOptions route = new PolylineOptions();
                        while (scanner.hasNext()) {
                            String[] latLng = scanner.nextLine().split(",");
                            double lat = Double.parseDouble(latLng[0]);
                            double lng = Double.parseDouble(latLng[1]);
                            route.add(new LatLng(lat, lng));
                        }
                        Bus.this.route = route;
                        int size = route.getPoints().size();
                        locationIndex = (int) (Math.random() * size);
                        location = route.getPoints().get(locationIndex);
                        markerOptions.position(location);
                        Log.d(TAG, "onSuccess: " + name + ": " + route.getPoints().size());
                        activity.onBusReady();
                        startMoving();
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.w(TAG, "onFailure: could not fetch route", exception);
            }
        });
    }

    public boolean markerOptionsReady() {
        return markerOptions.getPosition() != null;
    }

    private void startMoving() {
        new Thread(new Runnable() {
            @Override
            public synchronized void run() {
                while (locationIndex >= 0) {
                    move();
                    try {
                        wait(2000);
                    } catch (InterruptedException exception) {
                        exception.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private void move() {
        locationIndex++;
        locationIndex %= route.getPoints().size();
        location = route.getPoints().get(locationIndex);
        markerOptions.position(location);
    }
}
