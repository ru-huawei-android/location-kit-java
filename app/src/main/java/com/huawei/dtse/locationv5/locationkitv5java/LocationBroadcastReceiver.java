package com.huawei.dtse.locationv5.locationkitv5java;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Parcelable;

import com.huawei.hms.location.ActivityConversionData;
import com.huawei.hms.location.ActivityConversionInfo;
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
    public static final long REQUEST_PERIOD = 5000L;

    public static String statusFromCode(int code) {
        switch (code) {
            case ActivityIdentificationData.VEHICLE:
                return "VEHICLE";
            case ActivityIdentificationData.BIKE:
                return "BIKE";
            case ActivityIdentificationData.FOOT:
                return "FOOT";
            case ActivityIdentificationData.STILL:
                return "STILL";
            case ActivityIdentificationData.OTHERS:
                return "OTHERS";
            case ActivityIdentificationData.TILTING:
                return "TILTING";
            case ActivityIdentificationData.WALKING:
                return "WALKING";
            case ActivityIdentificationData.RUNNING:
                return "RUNNING";
            case ActivityConversionInfo.EXIT_ACTIVITY_CONVERSION:
                return "OUT FROM STILL ACTIVITY";
            case ActivityConversionInfo.ENTER_ACTIVITY_CONVERSION:
                return "IN STILL ACTIVITY";
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
        }
        context.sendBroadcast(deliverIntent);
    }
}
