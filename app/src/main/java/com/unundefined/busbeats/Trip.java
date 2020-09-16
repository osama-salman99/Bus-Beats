package com.unundefined.busbeats;

import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.PolylineOptions;
import com.unundefined.busbeats.directions.FetchURL;
import com.unundefined.busbeats.directions.TaskLoadedCallback;

public class Trip implements TaskLoadedCallback {
    public static final int START_LOCATION = 0;
    public static final int DESTINATION = 1;
    public static final int READY = 2;
    public static final int IN_TRIP = 3;
    private int state;
    private LatLng startLocation;
    private Marker startLocationMarker;
    private LatLng destinationLocation;
    private Marker destinationMarker;
    private MapActivity mapActivity;

    public Trip(@NonNull MapActivity mapActivity) {
        this.mapActivity = mapActivity;
        this.state = START_LOCATION;
    }

    public void setStartLocation(LatLng startLocation, Marker startLocationMarker) {
        this.startLocation = startLocation;
        this.startLocationMarker = startLocationMarker;
    }

    public void setDestination(LatLng destination, Marker destinationMarker) {
        this.destinationLocation = destination;
        this.destinationMarker = destinationMarker;
    }

    private void removeStartMarker() {
        startLocationMarker.remove();
    }

    private void removeDestinationMarker() {
        destinationMarker.remove();
    }

    public int getState() {
        return state;
    }

    public void goToNextState() {
        State state = State.START_LOCATION_STATE;
        switch (this.state) {
            case START_LOCATION:
                state = State.DESTINATION_STATE;
                this.state = DESTINATION;
                break;
            case DESTINATION:
                state = State.READY_STATE;
                this.state = READY;
                findRoute();
                break;
            case READY:
                state = State.IN_TRIP_STATE;
                this.state = IN_TRIP;
        }
        loadState(state);
    }

    public void goToPreviousState() {
        State state = State.START_LOCATION_STATE;
        switch (this.state) {
            case DESTINATION:
                removeStartMarker();
                state = State.START_LOCATION_STATE;
                this.state = START_LOCATION;
                break;
            case READY:
                mapActivity.removePolylines();
                removeDestinationMarker();
                state = State.DESTINATION_STATE;
                this.state = DESTINATION;
                break;
            case IN_TRIP:
                state = State.READY_STATE;
                this.state = READY;
        }
        loadState(state);
    }

    private void loadState(State state) {
        Button goButton = findViewById(R.id.go_button);
        Button startButton = findViewById(R.id.start_button);
        if (state.GO_BUTTON_FULL_WIDTH) {
            goButton.setVisibility(View.GONE);
            startButton.setVisibility(state.GO_BUTTON_VISIBILITY);
        } else {
            startButton.setVisibility(View.GONE);
            goButton.setVisibility(state.GO_BUTTON_VISIBILITY);
        }
        findViewById(R.id.search_bar).setVisibility(state.SEARCH_BAR_VISIBILITY);
        findViewById(R.id.marker_icon).setVisibility(state.MARKER_ICON_VISIBILITY);
        findViewById(R.id.info_button).setVisibility(state.INFO_BUTTON_VISIBILITY);
    }

    private <T extends View> T findViewById(int id) {
        return mapActivity.findViewById(id);
    }

    private void findRoute() {
        int busStart = 740;
        int busEnd = 940;
        int serviceStart = 100;
        int serviceEnd = 320;
        String url;
        url = getURL(startLocation,
                mapActivity.getBuses().get(2).getRoute().getPoints().get(busStart));
        new FetchURL(this).execute(url);

        mapActivity.addPolyline(getBusLocations(busStart, busEnd));

        url = getURL(mapActivity.getBuses().get(2).getRoute().getPoints().get(busEnd),
                mapActivity.getServices().get(1).getRoute().getPoints().get(serviceStart));
        new FetchURL(this).execute(url);

        mapActivity.addPolyline(getServiceLocations(serviceStart, serviceEnd));

        url = getURL(mapActivity.getServices().get(1).getRoute().getPoints().get(serviceEnd),
                destinationLocation);
        new FetchURL(this).execute(url);
    }

    private PolylineOptions getBusLocations(int from, int to) {
        if (from > to) {
            int temp = from;
            from = to;
            to = temp;
        }
        PolylineOptions route = mapActivity.getBuses().get(2).getRoute();
        return new PolylineOptions()
                .addAll(route.getPoints().subList(from, to))
                .color(mapActivity.getResources().getColor(R.color.busColor, null));
    }

    private PolylineOptions getServiceLocations(int from, int to) {
        if (from > to) {
            int temp = from;
            from = to;
            to = temp;
        }
        PolylineOptions route = mapActivity.getServices().get(1).getRoute();
        return new PolylineOptions()
                .addAll(route.getPoints().subList(from, to))
                .color(mapActivity.getResources().getColor(R.color.serviceColor, null));
    }

    @Override
    public void onTaskDone(Object... values) {
        mapActivity.addPolyline((PolylineOptions) values[0]);
    }

    private String getURL(LatLng startLocation, LatLng destinationLocation) {
        String origin = startLocation.latitude + "," + startLocation.longitude;
        String destination = destinationLocation.latitude + "," + destinationLocation.longitude;
        return "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=" + origin + "&" + "destination=" + destination + "&" +
                "key=" + mapActivity.getResources().getString(R.string.google_cloud_api_key);
    }
}
