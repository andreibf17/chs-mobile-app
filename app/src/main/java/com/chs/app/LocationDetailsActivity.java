package com.chs.app;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Toast;

import com.chs.app.db.DBUtilities;
import com.chs.app.entities.Location;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.HashMap;
import java.util.Map;

public class LocationDetailsActivity extends AppCompatActivity implements OnMapReadyCallback, NavigationView.OnNavigationItemSelectedListener {

    private GoogleMap mMap;
    private Location location = new Location();
    private boolean update;
    private boolean modeSet;
    private boolean markerSet = false;
    private SeekBar widthSeekBar;
    private SeekBar heightSeekBar;
    private CheckBox checkbox;
    private EditText editText;
    private Button modeButton;
    private Map<String,Object> dirtyState = new HashMap<>();

    private final int LOCATION_REQ_PERMISSION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_details);

        LocationManager lm = (LocationManager)getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        boolean gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean networkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        Toolbar toolbar = findViewById(R.id.toolbarLocationDetails);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        editText = findViewById(R.id.locationTitle);
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        update = this.getIntent().getExtras().getBoolean("Update");
        modeSet = this.getIntent().getExtras().getBoolean("ModeSet");

        checkbox = findViewById(R.id.checkBox);
        checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(location != null)
                    location.setReceiveNotification(isChecked);
            }
        });

        modeButton = findViewById(R.id.locationMode);

        if(update || modeSet) {
            location = this.getIntent().getExtras().getParcelable("Location");
            editText.setText(location.getName());
            checkbox.setChecked(location.getReceiveNotification());
            modeButton.setText(location.getMode().getName());
        }

        modeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(update) {
                    Intent intent = new Intent(v.getContext(), ModeListActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putBoolean("Update", update);
                    bundle.putString("Fixed", getIntent().getExtras().getString("Fixed"));
                    location.widenPolygon(widthSeekBar.getProgress());
                    location.heightenPolygon(heightSeekBar.getProgress());
                    location.setPolygonPoints(location.getPolygon());
                    bundle.putBoolean("FromLocation", true);
                    bundle.putParcelable("Location", location);
                    intent.putExtras(bundle);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(v.getContext(), ModeListActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putBoolean("Update", update);
                    bundle.putString("Fixed", getIntent().getExtras().getString("Fixed"));
                    location.widenPolygon(widthSeekBar.getProgress());
                    location.heightenPolygon(heightSeekBar.getProgress());
                    location.setPolygonPoints(location.getPolygon());
                    bundle.putBoolean("FromLocation", true);
                    bundle.putParcelable("Location", location);
                    intent.putExtras(bundle);
                    startActivity(intent);
                }
            }
        });
    }

    public void setDirtyState() {
        dirtyState.put("Text", editText.getText().toString());
        dirtyState.put("WidthSeekBar", widthSeekBar.getProgress());
        dirtyState.put("HeightSeekBar", heightSeekBar.getProgress());
        dirtyState.put("ReceiveNotification", checkbox.isChecked());
    }

    public boolean isDirty() {
        boolean tmp = true;
        tmp &= editText.getText().toString().equals(dirtyState.get("Text"));
        tmp &= widthSeekBar.getProgress() == ((Integer)dirtyState.get("WidthSeekBar")).intValue();
        tmp &= heightSeekBar.getProgress() == ((Integer)dirtyState.get("HeightSeekBar")).intValue();
        tmp &= checkbox.isChecked() == ((Boolean)dirtyState.get("ReceiveNotification")).booleanValue();
        tmp &= !markerSet;
        return !tmp;
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);

        if (checkLocationPermission())
            mMap.setMyLocationEnabled(true);
        else
            askLocationPermission();

        widthSeekBar = findViewById(R.id.widthSeekBar);
        widthSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(location != null && location.getLatlng() != null && location.getMap() != null)
                    location.widenPolygon(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        heightSeekBar = findViewById(R.id.heightSeekBar);
        heightSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(location != null && location.getLatlng() != null && location.getMap() != null)
                    location.heightenPolygon(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                location.setReceiveNotification(checkbox.isChecked());
            }
        });

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                location.setName(editText.getText().toString());
            }
        });

        if(update || modeSet) {
            location.setMap(mMap);  //map must be set before any other operations with the location
            location.setMarker(this);
            location.setPolygon();
            widthSeekBar.setProgress((int)((location.getPolygonPoints().get(0).longitude - location.getLatlng().longitude) / Constants.POLYGON_UNIT));
            heightSeekBar.setProgress((int)((location.getPolygonPoints().get(0).latitude - location.getLatlng().latitude) / Constants.POLYGON_UNIT));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(location.getLatlng()));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
            markerSet = true;
        } else {
            if (checkLocationPermission()) {
                LocationServices.getFusedLocationProviderClient(this).getLastLocation()
                        .addOnSuccessListener(this, new OnSuccessListener<android.location.Location>() {
                            @Override
                            public void onSuccess(android.location.Location location) {
                                if (location != null) {
                                    mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(), location.getLongitude())));
                                    mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
                                }
                            }
                        });
            }
            heightSeekBar.setEnabled(false);
            widthSeekBar.setEnabled(false);
        }
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {

            @Override
            public void onMapLongClick(LatLng point) {
                if(!markerSet) {
                    markerSet = true;
                    location.setMap(mMap);
                    location.setLatlng(point);
                    location.setMarker(getApplicationContext());
                    heightSeekBar.setEnabled(true);
                    heightSeekBar.setProgress(0);
                    widthSeekBar.setEnabled(true);
                    widthSeekBar.setProgress(0);
                }
            }
        });
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                marker.remove();
                location.removePolygon();
                location.setLatlng(null);
                heightSeekBar.setEnabled(false);
                widthSeekBar.setEnabled(false);
                markerSet = false;
                return false;
            }
        });

        setDirtyState();
    }

    // Check for permission to access Location
    private boolean checkLocationPermission() {
        // Ask for permission if it wasn't granted yet
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED);
    }

    // Asks for permission
    private void askLocationPermission() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_REQ_PERMISSION
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case LOCATION_REQ_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted
                    if (checkLocationPermission())
                        mMap.setMyLocationEnabled(true);
                    startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                } else {
                    Toast.makeText(this, "Application requires location permissions", Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }

    @Override
    public void onBackPressed() {
        update = this.getIntent().getExtras().getBoolean("Update");
        if (!update) {
            if (isDirty() || modeSet) {
                location.setName(editText.getText().toString());
                location.setReceiveNotification(checkbox.isChecked());
                switch (getIntent().getExtras().getString("Fixed")) {
                    case "Home": {
                        DBUtilities.saveHome(location);
                        break;
                    }
                    case "School": {
                        DBUtilities.saveSchool(location);
                        break;
                    }
                    case "Work": {
                        DBUtilities.saveWork(location);
                        break;
                    }
                    default: {
                        DBUtilities.saveLocation(location);
                        break;
                    }
                }

            }
        } else {
            switch (getIntent().getExtras().getString("Fixed")) {
            case "Home": {
                DBUtilities.updateHome(location);
                break;
            }
            case "School": {
                DBUtilities.updateSchool(location);
                break;
            }
            case "Work": {
                DBUtilities.updateWork(location);
                break;
            }
            default: {
                DBUtilities.updateLocation(location);
                break;
            }
            }
        }
        startActivity(new Intent(getApplicationContext(), MainActivity.class));
        //super.onBackPressed();
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (id == R.id.nav_howto) {

        } else if (id == R.id.nav_about) {
            startActivity(new Intent(this, AboutActivity.class));
        } else if (id == R.id.nav_exit) {
            notificationManager.cancel(Constants.APP_NOTIFICATION_ID);
            notificationManager.cancel(Constants.LOCATION_NOTIFICATION_ID);
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.putExtra("EXIT", true);
            startActivity(intent);
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
