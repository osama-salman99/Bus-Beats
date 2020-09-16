package com.unundefined.busbeats.transportation;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.firestore.DocumentSnapshot;

public abstract class Vehicle {
    protected String name;
    protected String id;
    protected String routeId;
    protected PolylineOptions route;
    protected Polyline mapPolyline;
    protected LatLng location;
    protected Marker marker;

    protected Vehicle(DocumentSnapshot document) {
        this.id = document.getId();
        this.name = (String) document.get("name");
        this.routeId = (String) document.get("route");
        this.route = null;
        this.marker = null;
        fetchRoute();
    }

    protected abstract void fetchRoute();

    public void removeMapPolyline() {
        if (mapPolyline != null) {
            mapPolyline.remove();
        }
    }

    public void setMapPolyline(Polyline polyline) {
        this.mapPolyline = polyline;
    }

    public boolean routeReady() {
        return route != null;
    }

    public PolylineOptions getRoute() {
        return route;
    }

    public void updateMarker(Marker marker) {
        this.marker = marker;
    }

    public void removeMarker() {
        if (marker != null) {
            marker.remove();
        }
    }

    public Marker getMarker() {
        return marker;
    }
}
