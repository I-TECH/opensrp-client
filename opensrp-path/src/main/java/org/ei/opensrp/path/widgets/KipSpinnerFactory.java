package org.ei.opensrp.path.widgets;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import com.vijay.jsonwizard.customviews.MaterialSpinner;
import com.vijay.jsonwizard.fragments.JsonFormFragment;
import com.vijay.jsonwizard.interfaces.CommonListener;
import com.vijay.jsonwizard.widgets.SpinnerFactory;

import org.ei.opensrp.path.application.VaccinatorApplication;
import org.ei.opensrp.path.repository.LocationRepository;
import org.json.JSONException;
import org.json.JSONObject;
import org.opensrp.api.domain.Location;

import java.util.List;

/**
 * Created by amosl on 6/13/17.
 */

public class KipSpinnerFactory extends SpinnerFactory {

    private static final String TAG = KipSpinnerFactory.class.getCanonicalName();

    @Override
    public List<View> getViewsFromJson(final String stepName, final Context context, final JsonFormFragment formFragment, JSONObject jsonObject, final CommonListener listener) throws Exception {

        final List<View> views = super.getViewsFromJson(stepName, context, formFragment, jsonObject, listener);

        if (jsonObject.has("key")) {

            final String key = jsonObject.getString("key");

            final MaterialSpinner spinner = (MaterialSpinner) views.get(0);

            if (key.equalsIgnoreCase("Ce_County") || key.equalsIgnoreCase("Ce_Sub_County") || key.equalsIgnoreCase("Ce_Ward")) {

                views.remove(spinner);
                spinner.setTag(key);

                if (key.equalsIgnoreCase("Ce_County") || key.equalsIgnoreCase("Ce_Sub_County")) {

                    spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            if (position >= 0) {

                                String value = (String) parent.getItemAtPosition(position);
                                String parentKey = (String) parent.getTag(com.vijay.jsonwizard.R.id.key);
                                String openMrsEntityParent = (String) parent.getTag(com.vijay.jsonwizard.R.id.openmrs_entity_parent);
                                String openMrsEntity = (String) parent.getTag(com.vijay.jsonwizard.R.id.openmrs_entity);
                                String openMrsEntityId = (String) parent.getTag(com.vijay.jsonwizard.R.id.openmrs_entity_id);
                                try {
                                    formFragment.getJsonApi().writeValue(stepName, parentKey, value, openMrsEntityParent, openMrsEntity,
                                            openMrsEntityId);
                                } catch (JSONException e) {
                                    Log.e(TAG, e.getMessage(), e);
                                    e.printStackTrace();
                                }

                                MaterialSpinner childSpinner = null;
                                View v = (View) parent.getParent();

                                if (key.equalsIgnoreCase("Ce_County")) {
                                    childSpinner = (MaterialSpinner) v.findViewWithTag("Ce_Sub_County");
                                } else if (key.equalsIgnoreCase("Ce_Sub_County")) {
                                    childSpinner = (MaterialSpinner) v.findViewWithTag("Ce_Ward");
                                }

                                if (childSpinner != null) {
                                    LocationRepository locationRepository = VaccinatorApplication.getInstance().locationRepository();
                                    Location location = locationRepository.getLocationByName(value);
                                    ArrayAdapter<String> adapter;

                                    if (location != null) {
                                        Log.i(TAG, "Parent location is not null: " + location.toString());
                                        List<Location> locations = locationRepository.getChildLocations(location.getLocationId());
                                        int size = locations.size();
                                        String[] locs = new String[Math.max(1, size)];

                                        if (size > 0) {
                                            for (int n = 0; n < size; n++) {
                                                locs[n] = locations.get(n).getName();
                                            }
                                        } else {
                                            locs[0] = "Other";
                                        }
                                        adapter = new ArrayAdapter<>(context, com.vijay.jsonwizard.R.layout.simple_list_item_1, locs);
                                    } else {
                                        adapter = new ArrayAdapter<>(context, com.vijay.jsonwizard.R.layout.simple_list_item_1, new String[]{"Other"});
                                        Log.i(TAG, "Parent location is null");
                                    }

                                    childSpinner.setAdapter(adapter);

                                }
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> adapterView) {

                        }
                    });
                }
                views.add(spinner);
            }
        }

        return views;
    }

}
