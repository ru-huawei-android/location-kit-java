package com.huawei.dtse.locationv5.locationkitv5java;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.widget.Switch;
import android.widget.TextView;

import com.huawei.hmf.tasks.Task;
import com.huawei.hms.location.ActivityConversionData;
import com.huawei.hms.location.ActivityIdentification;
import com.huawei.hms.location.ActivityIdentificationData;
import com.huawei.hms.location.ActivityIdentificationService;
import com.huawei.hms.location.FusedLocationProviderClient;
import com.huawei.hms.location.LocationServices;

import java.util.ArrayList;
import java.util.Objects;

import static com.huawei.dtse.locationv5.locationkitv5java.LocationBroadcastReceiver.ACTION_DELIVER_LOCATION;
import static com.huawei.dtse.locationv5.locationkitv5java.LocationBroadcastReceiver.ACTION_PROCESS_LOCATION;
import static com.huawei.dtse.locationv5.locationkitv5java.LocationBroadcastReceiver.EXTRA_HMS_LOCATION_CONVERSION;
import static com.huawei.dtse.locationv5.locationkitv5java.LocationBroadcastReceiver.EXTRA_HMS_LOCATION_RECOGNITION;
import static com.huawei.dtse.locationv5.locationkitv5java.LocationBroadcastReceiver.EXTRA_HMS_LOCATION_RESULT;
import static com.huawei.dtse.locationv5.locationkitv5java.LocationBroadcastReceiver.REQUEST_PERIOD;
import static com.huawei.dtse.locationv5.locationkitv5java.util.util.log;

public class MainActivity extends AppCompatActivity {

    public static boolean isListenActivityIdentification = true;
    private static final int REQUEST_CODE_1 = 1;
    private static final int REQUEST_CODE_2 = 2;
    private static final int REQUEST_CODE_3 = 3;
    private static final int REQUEST_CODE_4 = 4;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private ActivityIdentificationService activityIdentificationService;
    private PendingIntent pendingIntent;

    private TextView tvPosition;
    private TextView btnCheckLocation;
    private TextView tvRecognition;
    private TextView tvConversion;
    private TextView tvLocations;
    private Switch toggleRecognition;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnCheckLocation = findViewById(R.id.btnCheckLocation);
        tvPosition = findViewById(R.id.tvPosition);
        tvRecognition = findViewById(R.id.tvRecognition);
        tvConversion = findViewById(R.id.tvConversion);
        tvLocations = findViewById(R.id.tvLocations);
        toggleRecognition = findViewById(R.id.toggleRecognition);

        Objects.requireNonNull(getSupportActionBar()).hide();
        requestPermission();

        pendingIntent = getPendingIntent();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        activityIdentificationService = ActivityIdentification.getService(this);

        btnCheckLocation.setOnClickListener(v -> requestLastLocation());

        toggleRecognition.setOnCheckedChangeListener((compoundButton, enabled) -> {
            requestActivityRecognitionPermission(this.getApplicationContext());
            if (enabled) {
                startUserActivityTracking();
            } else {
                stopUserActivityTracking();
            }
        });
    }

    private void requestLastLocation() {
        try {
            tvPosition.setText(getString(R.string.get_last_searching));
            Task<Location> lastLocation = fusedLocationProviderClient.getLastLocation();
            lastLocation.addOnSuccessListener(location -> {
                if (location == null) {
                    tvPosition.setText(getString(R.string.get_last_failed));
                    log("location is null - did you grant the required permission?");
                    requestPermission();
                    return;
                }
                tvPosition.setText(location.getLongitude() + ", " + location.getLatitude());
            }).addOnFailureListener(e -> {
                tvPosition.setText(getString(R.string.get_last_null));
                log("failed: " + e.getMessage());
            });
        } catch (Exception e) {
            log("exception: " + e.getMessage());
        }
    }

    private BroadcastReceiver gpsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Objects.equals(intent.getAction(), ACTION_DELIVER_LOCATION)) {
                updateActivityIdentificationUI(Objects.requireNonNull(intent.getExtras()).getParcelableArrayList(EXTRA_HMS_LOCATION_RECOGNITION));
                updateActivityConversionUI(intent.getExtras().getParcelableArrayList(EXTRA_HMS_LOCATION_CONVERSION));
                updateLocationsUI(intent.getExtras().getParcelableArrayList(EXTRA_HMS_LOCATION_RESULT));
            }
        }
    };

    private PendingIntent getPendingIntent() {
        Intent intent = new Intent(this, LocationBroadcastReceiver.class);
        intent.setAction(ACTION_PROCESS_LOCATION);
        return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        toggleRecognition.setChecked(false);
    }

    public void updateActivityIdentificationUI(ArrayList<ActivityIdentificationData> statuses) {
        if (statuses != null) {
            StringBuilder out = new StringBuilder();
            statuses.forEach(item -> {
                out.append(LocationBroadcastReceiver.statusFromCode(item.getIdentificationActivity()));
                out.append(" ");
            });
            tvRecognition.setText(out.toString());
        } else {
            tvRecognition.setText(getString(R.string.str_activity_recognition_failed));
        }
    }

    public void updateActivityConversionUI(ArrayList<ActivityConversionData> statuses) {
        if (statuses != null) {
            StringBuilder out = new StringBuilder();
            statuses.forEach( item -> {
                out.append(LocationBroadcastReceiver.statusFromCode(item.getConversionType()));
                out.append(" ");
            });
            tvConversion.setText(out.toString());
        } else {
            tvConversion.setText(getString(R.string.str_activity_conversion_failed));
        }
    }

    public void updateLocationsUI(ArrayList<Location> locations) {
        if (locations != null) {
            StringBuilder out = new StringBuilder();
            locations.forEach(item -> {
                out.append(item.toString());
                out.append(" ");
            });
            tvLocations.setText(out.toString());
        } else {
            tvLocations.setText(getString(R.string.str_activity_locations_failed));
        }
    }

    private void startUserActivityTracking() {
        registerReceiver(gpsReceiver, new IntentFilter(ACTION_DELIVER_LOCATION));
        requestActivityUpdates(REQUEST_PERIOD);
    }

    private void stopUserActivityTracking() {
        unregisterReceiver(gpsReceiver);
        removeActivityUpdates();
    }

    private void requestActivityUpdates(long detectionIntervalMillis) {
        try {
            if (pendingIntent != null) removeActivityUpdates();
            pendingIntent = getPendingIntent();
            isListenActivityIdentification = true;
            activityIdentificationService.createActivityIdentificationUpdates(detectionIntervalMillis, pendingIntent)
                    .addOnSuccessListener(aVoid -> {
                        log("createActivityIdentificationUpdates onSuccess");
                    }).addOnFailureListener(e -> {
                log("createActivityIdentificationUpdates onFailure:" + e.getMessage());
            });
        } catch (java.lang.Exception e) {
            log("createActivityIdentificationUpdates exception:" + e.getMessage());
        }
    }

    private void removeActivityUpdates() {
        try {
            isListenActivityIdentification = false;
            log("start to removeActivityUpdates");
            activityIdentificationService.deleteActivityIdentificationUpdates(pendingIntent)
                    .addOnSuccessListener(aVoid -> log("deleteActivityIdentificationUpdates onSuccess"))
                    .addOnFailureListener(e -> log("deleteActivityIdentificationUpdates onFailure:" + e.getMessage()));
        } catch (java.lang.Exception e) {
            log("removeActivityUpdates exception:" + e.getMessage());
        }
    }

    //-------------------------------------------
    private void requestPermission() {
        // You must have the ACCESS_COARSE_LOCATION or ACCESS_FINE_LOCATION permission.
        // Otherwise, the location service is unavailable.
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            log("sdk < 28 Q");
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
            ) {
                String[] strings = new String[] {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
                ActivityCompat.requestPermissions(this, strings, REQUEST_CODE_1);
            }
        } else {
            log("sdk >= 28 Q");
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED
            ) {
                String[] strings = new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION};
                ActivityCompat.requestPermissions(this, strings, REQUEST_CODE_2);
            }
        }
    }

    private void requestActivityRecognitionPermission(Context context) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            if (ActivityCompat.checkSelfPermission(this,
                    "com.huawei.hms.permission.ACTIVITY_RECOGNITION") != PackageManager.PERMISSION_GRANTED) {
                String[] permissions = new String[] {"com.huawei.hms.permission.ACTIVITY_RECOGNITION"};
                ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_3);
                log("requestActivityRecognitionPermission: apply permission");
            }
        } else {
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                String[] permissions = new String[] {Manifest.permission.ACTIVITY_RECOGNITION};
                ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_4);
                log("requestActivityRecognitionPermission: apply permission");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_1) {
            if (grantResults.length > 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                log("onRequestPermissionsResult: apply LOCATION PERMISSION successful");
                requestLastLocation();
            } else {
                log("onRequestPermissionsResult: apply LOCATION PERMISSION failed");
            }
        }
        if (requestCode == REQUEST_CODE_2) {
            if (grantResults.length > 2 && grantResults[2] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                log("onRequestPermissionsResult: apply ACCESS_BACKGROUND_LOCATION successful");
                requestLastLocation();
            } else {
                log("onRequestPermissionsResult: apply ACCESS_BACKGROUND_LOCATION  failed");
            }
        }
        if (requestCode == REQUEST_CODE_3) {
            if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                log("onRequestPermissionsResult: apply com.huawei.hms.permission.ACTIVITY_RECOGNITION successful");
                startUserActivityTracking();
            } else {
                log("onRequestPermissionsResult: apply com.huawei.hms.permission.ACTIVITY_RECOGNITION  failed");
            }
        }
        if (requestCode == REQUEST_CODE_4 && Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                log("onRequestPermissionsResult: apply " + Manifest.permission.ACTIVITY_RECOGNITION + " successful");
                startUserActivityTracking();
            } else {
                log("onRequestPermissionsResult: apply " + Manifest.permission.ACTIVITY_RECOGNITION + " failed");
            }
        }
    }
}