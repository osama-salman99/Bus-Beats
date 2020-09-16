package com.unundefined.busbeats;

import android.view.View;

public class State {
    public static final State START_LOCATION_STATE = createStartLocationState();
    public static final State DESTINATION_STATE = createDestinationState();
    public static final State READY_STATE = createReadyState();
    public static final State IN_TRIP_STATE = createInTripState();
    public final int GO_BUTTON_VISIBILITY;
    public final int SEARCH_BAR_VISIBILITY;
    public final int MARKER_ICON_VISIBILITY;
    public final int INFO_BUTTON_VISIBILITY;
    public final boolean GO_BUTTON_FULL_WIDTH;

    public State(
            int goButtonVisibility,
            int searchBarVisibility,
            int markerIconVisibility,
            int infoButtonVisibility,
            boolean goButtonFullWidth) {
        this.GO_BUTTON_VISIBILITY = goButtonVisibility;
        this.SEARCH_BAR_VISIBILITY = searchBarVisibility;
        this.MARKER_ICON_VISIBILITY = markerIconVisibility;
        this.INFO_BUTTON_VISIBILITY = infoButtonVisibility;
        this.GO_BUTTON_FULL_WIDTH = goButtonFullWidth;
    }

    private static State createStartLocationState() {
        return new State(View.VISIBLE, View.VISIBLE, View.VISIBLE, View.GONE,
                false);
    }

    private static State createDestinationState() {
        return new State(View.VISIBLE, View.VISIBLE, View.VISIBLE, View.GONE,
                false);
    }

    private static State createReadyState() {
        return new State(View.VISIBLE, View.GONE, View.GONE, View.VISIBLE,
                true);
    }

    private static State createInTripState() {
        return new State(View.GONE, View.GONE, View.GONE, View.VISIBLE,
                true);
    }
}
