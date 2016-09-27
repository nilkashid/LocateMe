package com.example.nileshkashid.locateme;


import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Debug;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MyLocationActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener,ResultCallback<LocationSettingsResult> {

    private static final int PERMISSIONS_REQUEST_LOCATION = 10;
    private static final int PERMISSIONS_REQUEST_COARSE_LOCATION = 20;
    private static final int REQUEST_CHECK_SETTINGS = 30;

    EditText enterCodeEt;
    Button startLocationBtn;
    Spinner timeFilter;
    Location mLastLocation;
    TextView locationText;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    String lat,lon, time;
    boolean isLocationServiceEnable = false;
    protected LocationSettingsRequest mLocationSettingsRequest;
    protected PendingIntent locationPendingIntent;
    protected boolean isLocationUpdatesOn = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_location);

        updateStatusForLocationListening();
        initUi();
        if(!isLocationUpdatesOn)
        {
            initLocationListeningProcess();
        }
        registerLocationListener();
    }

    private void updateStatusForLocationListening() {
        isLocationUpdatesOn = getBooleanSharedPreference(this, "isLocationUpdatesOn", false);
    }

    private void initLocationListeningProcess() {

        createLocationPendingIntent();
        initLocationListener();

        createLocationRequest();
        buildLocationSettingsRequest();
        checkForLocationPermission();
    }

    private void registerLocationListener() {
        LocalBroadcastManager.getInstance(this).registerReceiver(locationReceiver, new IntentFilter("LOCATION_UPDATES_AVAILABLE"));
    }
    private void unregisterLocationListener() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(locationReceiver);
    }



    private void createLocationPendingIntent() {
        Intent intervalIntent = new Intent(this, LocationPendingIntentService.class);
        locationPendingIntent = PendingIntent.getService(this, 1, intervalIntent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private void initLocationListener() {
        buildGoogleApiClient();
    }

    private void checkForLocationPermission() {
        // Check the SDK version and whether the permission is already granted or not.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_LOCATION);
        } else {
            isLocationServiceEnable =true;
            //initLocationListener();
            checkLocationSettings();
        }
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSIONS_REQUEST_LOCATION);
        } else {
            isLocationServiceEnable =true;
            initLocationListener();
        }*/
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                isLocationServiceEnable = true;
//                initLocationListener();
                checkLocationSettings();
            } else {
                Toast.makeText(this, "Until you grant the permission, we canot display the names", Toast.LENGTH_SHORT).show();
            }
        }
       /* if (requestCode == PERMISSIONS_REQUEST_COARSE_LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                isLocationServiceEnable = true;
                initLocationListener();
            } else {
                Toast.makeText(this, "Until you grant the permission, we canot display the names", Toast.LENGTH_SHORT).show();
            }
        }*/
    }
    private void initUi() {
        enterCodeEt = (EditText)findViewById(R.id.enter_code_et);
        startLocationBtn = (Button) findViewById(R.id.start_location_btn);
        updateLocationButtonText();
        timeFilter = (Spinner) findViewById(R.id.time_filter);
//        initFilter();
        locationText = (TextView) findViewById(R.id.location_text);
        startLocationBtn.setOnClickListener(startLocationUpdates);
    }

    private void initFilter()
    {
        ArrayAdapter<String> adapter;
        String[] list = getResources().getStringArray(R.array.timer_filter_array);

        adapter = new ArrayAdapter<String>(getApplicationContext(),android.R.layout.simple_spinner_item, list);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        timeFilter.setAdapter(adapter);
    }

    private void updateLocationButtonText() {
        if(!isLocationUpdatesOn)
        {
            startLocationBtn.setText(getString(R.string.start_location));
        }
        else
        {
            startLocationBtn.setText(getString(R.string.stop_location));
        }
    }

    private View.OnClickListener startLocationUpdates = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if(isLocationUpdatesOn) {
                Log.i("^&^&^&^&^&^& ", "values for connection : "+mGoogleApiClient.isConnected());
                Log.i("^&^&^&^&^&^& ", "values for connection : "+mGoogleApiClient.isConnectionCallbacksRegistered(MyLocationActivity.this));
                processStopLocation();
                updateBooleanSharedPreference(MyLocationActivity.this, "isLocationUpdatesOn", false);
            }
            else
            {
                processStartLocation();
                updateBooleanSharedPreference(MyLocationActivity.this, "isLocationUpdatesOn", true);
            }
            updateStatusForLocationListening();
            updateLocationButtonText();
        }
    };

    private void processStartLocation() {
//        initLocationListeningProcess();
        mGoogleApiClient.connect();
    }

    private void processStopLocation() {

//        if(mGoogleApiClient !=null && mGoogleApiClient.isConnected()){
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, locationPendingIntent);
//        }
//        else{
//            //no need to stop updates - we are no longer connected to location service anyway
//        }
        unregisterLocationListener();
        mGoogleApiClient.disconnect();
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public void onConnected(Bundle bundle) {

        Log.i("^&^&^&^&^&^& ", "values for connection : "+mGoogleApiClient.isConnected());
        Log.i("^&^&^&^&^&^& ", "values for connection : "+mGoogleApiClient.isConnectionCallbacksRegistered(this));

        updateBooleanSharedPreference(this, "isLocationUpdatesOn", true);
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, locationPendingIntent);
//        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
//        LocationServices.FusedLocationApi.requestLocationUpdates()

        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            lat = String.valueOf(mLastLocation.getLatitude());
            lon = String.valueOf(mLastLocation.getLongitude());
            time = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS").format(new Date(mLastLocation.getTime()));
            updateUI();
        }
    }

    private void createLocationRequest() {
        mLocationRequest = LocationRequest.create();
//        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        mLocationRequest.setInterval(100); // Update location every second
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i("&&&&&&&&&&  "," "+i);
    }

    @Override
    public void onLocationChanged(Location location) {
        lat = String.valueOf(location.getLatitude());
        lon = String.valueOf(location.getLongitude());
        time = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS").format(new Date(location.getTime()));

        updateUI();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i("^&^&^&^&^&^& ", "values for connection : onConnectionFailed"+mGoogleApiClient.isConnected());
        initLocationListener();
    }

    synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();


    }

    @Override
    protected void onStart() {
        super.onStart();
       /* if (isLocationServiceEnable) {
            mGoogleApiClient.connect();
        }*/
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        /*if (isLocationServiceEnable) {
            mGoogleApiClient.disconnect();
        }*/
        unregisterLocationListener();
    }

    void updateUI() {
        locationText.setText("Latitude : " + lat + "\n" + "Longitude : " + lon + "\n" + "Last updated on : "+time);
    }


    ///////////////////////////////////////////////////////////////////////////////
    protected void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }
    protected void checkLocationSettings() {
        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(
                        mGoogleApiClient,
                        mLocationSettingsRequest
                );
        result.setResultCallback(this);
    }

    @Override
    public void onResult(LocationSettingsResult locationSettingsResult) {
        final Status status = locationSettingsResult.getStatus();
        switch (status.getStatusCode()) {
            case LocationSettingsStatusCodes.SUCCESS:
                Log.i("", "All location settings are satisfied.");
//                initLocationListener();
                break;
            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                Log.i("", "Location settings are not satisfied. Show the user a dialog to" +
                        "upgrade location settings ");

                try {
                    // Show the dialog by calling startResolutionForResult(), and check the result
                    // in onActivityResult().
                    status.startResolutionForResult(MyLocationActivity.this, REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException e) {
                    Log.i("", "PendingIntent unable to execute request.");
                }
                break;
            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                Log.i("", "Location settings are inadequate, and cannot be fixed here. Dialog " +
                        "not created.");
                break;
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.i("", "User agreed to make required location settings changes.");
//                        initLocationListener();
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.i("", "User chose not to make required location settings changes.");
                        break;
                }
                break;
        }
    }

    private BroadcastReceiver locationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Location location = intent.getParcelableExtra("location");
            onLocationChanged(location);
        }
    };

    public static void updateBooleanSharedPreference(Context ctx, String name, boolean value) {
        SharedPreferences settings = ctx.getSharedPreferences("locateMe", Activity.MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putBoolean(name, value);
        prefEditor.commit();
    }

    public static boolean getBooleanSharedPreference(Context ctx, String name,boolean defaultValue) {
        SharedPreferences settings = ctx.getSharedPreferences("locateMe", Activity.MODE_PRIVATE);
        return settings.getBoolean(name, defaultValue);
    }
}
