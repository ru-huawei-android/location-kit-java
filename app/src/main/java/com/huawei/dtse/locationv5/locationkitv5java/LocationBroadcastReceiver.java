package com.huawei.dtse.locationv5.locationkitv5java;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Parcelable;

import com.huawei.hms.location.ActivityConversionData;
import com.huawei.hms.location.ActivityConversionResponse;
import com.huawei.hms.location.ActivityIdentificationData;
import com.huawei.hms.location.ActivityIdentificationResponse;
import com.huawei.hms.location.LocationAvailability;
import com.huawei.hms.location.LocationResult;

import java.util.ArrayList;
import java.util.List;

import static com.huawei.dtse.locationv5.locationkitv5java.util.util.log;

public class LocationBroadcastReceiver extends BroadcastReceiver {

    public static final String ACTION_PROCESS_LOCATION = "com.huawei.hms.location.ACTION_PROCESS_LOCATION";
    public static final String ACTION_DELIVER_LOCATION = "ACTION_DELIVER_LOCATION";
    public static final String EXTRA_HMS_LOCATION_RECOGNITION = "EXTRA_HMS_LOCATION_RECOGNITION";
    public static final String EXTRA_HMS_LOCATION_CONVERSION = "EXTRA_HMS_LOCATION_CONVERSION";
    public static final String EXTRA_HMS_LOCATION_RESULT = "EXTRA_HMS_LOCATION_RESULT";
    public static final String EXTRA_HMS_LOCATION_AVAILABILITY = "EXTRA_HMS_LOCATION_AVAILABILITY";
    public static final long REQUEST_PERIOD = 5000L;
    private static final int VEHICLE = 100;
    private static final int BIKE = 101;
    private static final int FOOT = 102;
    private static final int STILL = 103;
    private static final int OTHERS = 104;
    private static final int TILTING = 105;
    private static final int WALKING = 107;
    private static final int RUNNING = 108;

    public static String statusFromCode(int code) {
        switch (code) {
            case VEHICLE:
                return "VEHICLE";
            case BIKE:
                return "BIKE";
            case FOOT:
                return "FOOT";
            case STILL:
                return "STILL";
            case OTHERS:
                return "OTHERS";
            case TILTING:
                return "TILTING";
            case WALKING:
                return "WALKING";
            case RUNNING:
                return "RUNNING";
            default:
                return "UNDEFINED";
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        log("onReceive() hms location broadcast");

        Intent deliverIntent = new Intent(ACTION_DELIVER_LOCATION);
        String action = intent.getAction();

        if (action != null && action.equals(ACTION_PROCESS_LOCATION)) {

            ActivityConversionResponse activityConversionResult = ActivityConversionResponse.getDataFromIntent(intent);
            if (activityConversionResult != null) {
                List<ActivityConversionData> list =
                        activityConversionResult.getActivityConversionDatas();
                        deliverIntent.putParcelableArrayListExtra(EXTRA_HMS_LOCATION_CONVERSION, (ArrayList<? extends Parcelable>) list);
            }

            ActivityIdentificationResponse activityRecognitionResult = ActivityIdentificationResponse.getDataFromIntent(intent);
            if (activityRecognitionResult != null && MainActivity.isListenActivityIdentification) {
                List<ActivityIdentificationData> list =
                        activityRecognitionResult.getActivityIdentificationDatas();
                        deliverIntent.putParcelableArrayListExtra(EXTRA_HMS_LOCATION_RECOGNITION, (ArrayList<? extends Parcelable>) list);
            }

            if (LocationResult.hasResult(intent)) {
                LocationResult result = LocationResult.extractResult(intent);
                if (result != null) {
                    List<Location> list = result.getLocations();
                            deliverIntent.putParcelableArrayListExtra(EXTRA_HMS_LOCATION_RESULT, (ArrayList<? extends Parcelable>) list);
                }
            }

            if (LocationAvailability.hasLocationAvailability(intent)) {
                LocationAvailability locationAvailability = LocationAvailability.extractLocationAvailability(intent);
                deliverIntent.putExtra(EXTRA_HMS_LOCATION_AVAILABILITY, locationAvailability.isLocationAvailable());
            }
        }
        context.sendBroadcast(deliverIntent);
    }
}
