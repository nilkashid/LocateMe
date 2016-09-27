package com.example.nileshkashid.locateme;

import android.app.IntentService;
import android.content.Intent;
import android.location.Location;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationServices;

/**
 * Created by Nilesh.Kashid on 24-09-2016.
 */
public class LocationPendingIntentService extends IntentService {

    public LocationPendingIntentService(String name) {
        super(name);
    }

    public LocationPendingIntentService() {
        super("");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Location location = intent.getParcelableExtra(FusedLocationProviderApi.KEY_LOCATION_CHANGED);
        if (location != null) {
            Log.i("***********", "Lcation Received ");
            Intent locationIntent = new Intent("LOCATION_UPDATES_AVAILABLE");
            locationIntent.putExtra("location", location);
            LocalBroadcastManager.getInstance(this).sendBroadcast(locationIntent);
        }
    }
}