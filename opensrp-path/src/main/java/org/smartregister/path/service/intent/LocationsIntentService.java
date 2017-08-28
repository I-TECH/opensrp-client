package org.smartregister.path.service.intent;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opensrp.api.domain.Location;
import org.smartregister.path.application.VaccinatorApplication;
import org.smartregister.path.repository.LocationRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by amosl on 6/13/17.
 */

public class LocationsIntentService extends IntentService {
    private static final String TAG = LocationsIntentService.class.getCanonicalName();
    private LocationRepository locationRepository;

    public LocationsIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(TAG, "OnHandleIntent..start");
        String userInfo = intent.getExtras().getString("userInfo");

        List<Location> locations = new ArrayList<>();

        try {
            JSONObject userInfoJson = new JSONObject(userInfo);
            JSONObject userLocationJson = userInfoJson.has("userLocations") ? userInfoJson.getJSONObject("userLocations") : null;
            if(userLocationJson.has("userLocations")){
                JSONArray userLocations = userLocationJson.getJSONArray("userLocations");
                if(userLocations != null && userLocations.length() > 0){
                    Location l;
                    for (int i = 0; i < userLocations.length(); i++) {
                        l = new Gson().fromJson(userLocations.getString(i), Location.class);
                        locations.add(l);
                    }
                }
            }

            locationRepository.bulkInsertLocations(locations);
        } catch (JSONException e) {
            Log.e("LocationsTask", e.getMessage());
            e.printStackTrace();
        }

        Log.i(TAG, "OnHandleIntent..end");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        locationRepository = VaccinatorApplication.getInstance().locationRepository();
        return super.onStartCommand(intent, flags, startId);
    }
}
