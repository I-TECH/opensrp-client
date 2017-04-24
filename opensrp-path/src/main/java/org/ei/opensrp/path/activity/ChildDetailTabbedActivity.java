package org.ei.opensrp.path.activity;

import android.app.FragmentTransaction;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.vijay.jsonwizard.activities.JsonFormActivity;

import org.apache.commons.lang3.StringUtils;
import org.ei.opensrp.Context;
import org.ei.opensrp.clientandeventmodel.Event;
import org.ei.opensrp.commonregistry.CommonPersonObjectClient;
import org.ei.opensrp.domain.Vaccine;
import org.ei.opensrp.domain.Weight;
import org.ei.opensrp.path.R;
import org.ei.opensrp.path.application.VaccinatorApplication;
import org.ei.opensrp.path.domain.Photo;
import org.ei.opensrp.path.domain.VaccineWrapper;
import org.ei.opensrp.path.domain.WeightWrapper;
import org.ei.opensrp.path.fragment.EditWeightDialogFragment;
import org.ei.opensrp.path.listener.VaccinationActionListener;
import org.ei.opensrp.path.listener.WeightActionListener;
import org.ei.opensrp.path.repository.BaseRepository;
import org.ei.opensrp.path.repository.PathRepository;
import org.ei.opensrp.path.repository.VaccineRepository;
import org.ei.opensrp.path.repository.WeightRepository;
import org.ei.opensrp.path.sync.ECSyncUpdater;
import org.ei.opensrp.path.sync.PathClientProcessor;
import org.ei.opensrp.path.tabfragments.child_registration_data_fragment;
import org.ei.opensrp.path.tabfragments.child_under_five_fragment;
import org.ei.opensrp.path.toolbar.ChildDetailsToolbar;
import org.ei.opensrp.path.view.LocationPickerView;
import org.ei.opensrp.path.viewComponents.ImmunizationRowGroup;
import org.ei.opensrp.repository.AllSharedPreferences;
import org.ei.opensrp.repository.DetailsRepository;
import org.ei.opensrp.util.FormUtils;
import org.ei.opensrp.util.OpenSRPImageLoader;
import org.ei.opensrp.view.activity.DrishtiApplication;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opensrp.api.constants.Gender;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import util.DateUtils;
import util.ImageUtils;
import util.JsonFormUtils;
import util.Utils;
import util.VaccinatorUtils;

import static util.Utils.getName;
import static util.Utils.getValue;

/**
 * Created by raihan on 1/03/2017.
 */

public class ChildDetailTabbedActivity extends BaseActivity implements VaccinationActionListener, WeightActionListener {

    public Menu overflow;
    private ChildDetailsToolbar detailtoolbar;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private TextView saveButton;
    private static final int REQUEST_CODE_GET_JSON = 3432;
    static final int REQUEST_TAKE_PHOTO = 1;
    public static Gender gender;
    //////////////////////////////////////////////////
    private static final String TAG = "ChildDetails";
    private static final String VACCINES_FILE = "vaccines.json";
    public static final String EXTRA_CHILD_DETAILS = "child_details";
    private static final String EXTRA_REGISTER_CLICKABLES = "register_clickables";
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy");
    private child_registration_data_fragment child_data_fragment;
    private child_under_five_fragment child_under_five_Fragment;
    private static final String DIALOG_TAG = "ChildDetailActivity_DIALOG_TAG";

    private File currentfile;
    public String location_name = "";

    public CommonPersonObjectClient getChildDetails() {
        return childDetails;
    }

    // Data
    private CommonPersonObjectClient childDetails;
    private Map<String, String> detailmaps;
    AllSharedPreferences allSharedPreferences;
    ////////////////////////////////////////////////
    DetailsRepository detailsRepository;
    Map<String, String> details;
    private static final String inactive="inactive";
    private static final String lostToFollowUp="lost_to_follow_up";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = this.getIntent().getExtras();
        location_name = extras.getString("location_name");
        if (extras != null) {
            Serializable serializable = extras.getSerializable(EXTRA_CHILD_DETAILS);
            if (serializable != null && serializable instanceof CommonPersonObjectClient) {
                childDetails = (CommonPersonObjectClient) serializable;
            }
        }
        detailsRepository = org.ei.opensrp.Context.getInstance().detailsRepository();
        details = detailsRepository.getAllDetailsForClient(childDetails.entityId());
        details.putAll(childDetails.getColumnmaps());

        setContentView(R.layout.child_detail_activity_simple_tabs);

        child_data_fragment = new child_registration_data_fragment();

        child_data_fragment.setArguments(this.getIntent().getExtras());

        child_under_five_Fragment = new child_under_five_fragment();
        child_under_five_Fragment.setArguments(this.getIntent().getExtras());


        detailtoolbar = (ChildDetailsToolbar) findViewById(R.id.child_detail_toolbar);


        saveButton = (TextView) detailtoolbar.findViewById(R.id.save);
        saveButton.setVisibility(View.INVISIBLE);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                detailtoolbar.showOverflowMenu();
                for (int i = 0; i < overflow.size(); i++) {
                    overflow.getItem(i).setVisible(true);
                }
                child_under_five_Fragment.loadview(false,false);

                saveButton.setVisibility(View.INVISIBLE);
            }
        });

        detailtoolbar.showOverflowMenu();

        setSupportActionBar(detailtoolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        tabLayout = (TabLayout) findViewById(R.id.tabs);
//        getSupportActionBar().
        initiallization();
        viewPager = (ViewPager) findViewById(R.id.viewpager);
        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (position == 0) {
                    saveButton.setVisibility(View.INVISIBLE);
                    for (int i = 0; i < overflow.size(); i++) {
                        overflow.getItem(i).setVisible(true);
                    }
                    child_under_five_Fragment.loadview(false,false);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        setupViewPager(viewPager);

        detailtoolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        detailtoolbar.setTitle(updateActivityTitle());


        tabLayout.setupWithViewPager(viewPager);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        allSharedPreferences = new AllSharedPreferences(preferences);

    }

    private void initiallization() {

        DetailsRepository detailsRepository = getOpenSRPContext().detailsRepository();
        detailmaps = detailsRepository.getAllDetailsForClient(childDetails.entityId());
        profileWidget();
        ((TextView) detailtoolbar.findViewById(R.id.title)).setText(updateActivityTitle());

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
       // super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.menu_child_detail_settings, menu);
        overflow = menu;
        VaccineRepository vaccineRepository = VaccinatorApplication.getInstance().vaccineRepository();
        List <Vaccine> vaccineList = vaccineRepository.findByEntityId(childDetails.entityId());
        boolean all_synced = true;
        for(int i = 0;i < vaccineList.size();i++){
           if(vaccineList.get(i).getSyncStatus().equalsIgnoreCase(VaccineRepository.TYPE_Unsynced)){
               all_synced = false;
           }
        }
        if(vaccineList.size() ==0 || all_synced){
            overflow.getItem(2).setEnabled(false);

        }
        WeightRepository wp =  VaccinatorApplication.getInstance().weightRepository();
        List <Weight> weightlist =  wp.findLast5(childDetails.entityId());
        if(weightlist.size() ==0){
            overflow.getItem(1).setEnabled(false);

        }
        return true;
    }
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        //super.onPrepareOptionsMenu(menu);
        //getMenuInflater().inflate(R.menu.menu_child_detail_settings, menu);

        if (details.containsKey(lostToFollowUp) && details.get(lostToFollowUp).equalsIgnoreCase(Boolean.TRUE.toString())) {
            menu.findItem(R.id.mark_as_lost_to_followup).setTitle(getResources().getString(R.string.mark_as_not_lost_to_followup));
        }else{
            menu.findItem(R.id.mark_as_lost_to_followup).setTitle(getResources().getString(R.string.mark_as_lost_to_followup));

        }

        if (details.containsKey(inactive) && details.get(inactive).equalsIgnoreCase(Boolean.TRUE.toString())) {
            menu.findItem(R.id.mark_inactive).setTitle(getResources().getString(R.string.mark_active));
        }else{
            menu.findItem(R.id.mark_inactive).setTitle(getResources().getString(R.string.mark_inactive));
        }

        return true;
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case R.id.registration_data:
                String formmetadata = getmetaDataForEditForm();
                startFormActivity("child_enrollment", childDetails.entityId(), formmetadata);
                // User chose the "Settings" item, show the app settings UI...
                return true;
            case R.id.immunization_data:
                viewPager.setCurrentItem(1);
                child_under_five_Fragment.loadview(true,false);
                saveButton.setVisibility(View.VISIBLE);
                for (int i = 0; i < overflow.size(); i++) {
                    overflow.getItem(i).setVisible(false);
                }
//                detailtoolbar.hideOverflowMenu();
                return true;
            case R.id.weight_data:
//                showWeightDialog();
                viewPager.setCurrentItem(1);
                child_under_five_Fragment.loadview(false,true);
                saveButton.setVisibility(View.VISIBLE);
                for (int i = 0; i < overflow.size(); i++) {
                    overflow.getItem(i).setVisible(false);
                }
                return true;

            case R.id.report_deceased:
                String reportDeceasedMetadata = getReportDeceasedMetadata();
                startFormActivity("report_deceased", childDetails.entityId(), reportDeceasedMetadata);
                return true;
            case R.id.mark_inactive:
                if (details.containsKey(inactive) && details.get(inactive).equalsIgnoreCase(Boolean.TRUE.toString())) {
                    updateClientAttribute(inactive, false);

                } else {
                    updateClientAttribute(inactive, true);

                }
                updateStatus();
                return true;
            case R.id.mark_as_lost_to_followup:
                if (details.containsKey(lostToFollowUp) && details.get(lostToFollowUp).equalsIgnoreCase(Boolean.TRUE.toString())) {
                    updateClientAttribute(lostToFollowUp, false);
                } else {
                    updateClientAttribute(lostToFollowUp, true);

                }
                updateStatus();
                return true;
            case R.id.report_adverse_event:
                return launchAdverseEventForm();
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    private boolean launchAdverseEventForm() {
        LaunchAdverseEventFormTask task = new LaunchAdverseEventFormTask();
        task.execute();
        return true;
    }

    private String getmetaDataForEditForm() {
        Context context = getOpenSRPContext();
        try {
            JSONObject form = FormUtils.getInstance(getApplicationContext()).getFormJson("child_enrollment");
            LocationPickerView lpv = new LocationPickerView(getApplicationContext());
            lpv.init(context);
            JsonFormUtils.addChildRegLocHierarchyQuestions(form, context);
            Log.d(TAG, "Form is "+form.toString());
            if (form != null) {
                form.put("entity_id", childDetails.entityId());
                form.put("relational_id", childDetails.getColumnmaps().get("relational_id"));

                Intent intent = new Intent(getApplicationContext(), JsonFormActivity.class);
                //inject zeir id into the form
                JSONObject stepOne = form.getJSONObject(JsonFormUtils.STEP1);
                JSONArray jsonArray = stepOne.getJSONArray(JsonFormUtils.FIELDS);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    if (jsonObject.getString(JsonFormUtils.KEY).equalsIgnoreCase("First_Name")) {
                        jsonObject.put(JsonFormUtils.VALUE, Utils.getValue(childDetails.getColumnmaps(), "first_name", true));
                    }
                    if (jsonObject.getString(JsonFormUtils.KEY).equalsIgnoreCase("Last_Name")) {
                        jsonObject.put(JsonFormUtils.VALUE, Utils.getValue(childDetails.getColumnmaps(), "last_name", true));
                    }
                    if (jsonObject.getString(JsonFormUtils.KEY).equalsIgnoreCase("Sex")) {
                        jsonObject.put(JsonFormUtils.READ_ONLY, true);
                        jsonObject.put(JsonFormUtils.VALUE, Utils.getValue(childDetails.getColumnmaps(), "gender", true));
                    }
                    if (jsonObject.getString(JsonFormUtils.KEY).equalsIgnoreCase(JsonFormUtils.ZEIR_ID)) {
                        jsonObject.put(JsonFormUtils.READ_ONLY, true);
                        jsonObject.put(JsonFormUtils.VALUE, Utils.getValue(childDetails.getColumnmaps(), "zeir_id", true).replace("-", ""));
                    }
                    if (jsonObject.getString(JsonFormUtils.KEY).equalsIgnoreCase("Child_Register_Card_Number")) {
                        jsonObject.put(JsonFormUtils.VALUE, Utils.getValue(detailmaps, "Child_Register_Card_Number", true));
                    }
                    if (jsonObject.getString(JsonFormUtils.KEY).equalsIgnoreCase("Child_Birth_Certificate")) {
                        jsonObject.put(JsonFormUtils.READ_ONLY, true);
                        jsonObject.put(JsonFormUtils.VALUE, Utils.getValue(detailmaps, "Child_Birth_Certificate", true));
                    }
                    if (jsonObject.getString(JsonFormUtils.KEY).equalsIgnoreCase("Mother_Guardian_First_Name")) {
                        jsonObject.put(JsonFormUtils.VALUE, (Utils.getValue(childDetails.getColumnmaps(), "mother_first_name", true).isEmpty()?Utils.getValue(childDetails.getDetails(), "mother_first_name", true):Utils.getValue(childDetails.getColumnmaps(), "mother_first_name", true)));

                    }
                    if (jsonObject.getString(JsonFormUtils.KEY).equalsIgnoreCase("Mother_Guardian_Last_Name")) {
                        jsonObject.put(JsonFormUtils.VALUE, (Utils.getValue(childDetails.getColumnmaps(), "mother_last_name", true).isEmpty() ? Utils.getValue(childDetails.getDetails(), "mother_last_name", true) : Utils.getValue(childDetails.getColumnmaps(), "mother_last_name", true)));
                    }
                    if (jsonObject.getString(JsonFormUtils.KEY).equalsIgnoreCase("Mother_Guardian_Date_Birth")) {

                        if(Utils.getValue(childDetails.getColumnmaps(), "mother_dob", true)!=null || Utils.getValue(childDetails.getColumnmaps(), "mother_dob", true).equalsIgnoreCase(""))
                        {
                            try {
                                jsonObject.put(JsonFormUtils.READ_ONLY, true);
                                DateTime dateTime = new DateTime(Utils.getValue(childDetails.getColumnmaps(), "mother_dob", true));
                                Date dob = dateTime.toDate();
                                jsonObject.put(JsonFormUtils.VALUE, DATE_FORMAT.format(dob));
                            }catch (Exception e){

                            }
                        }
                    }
                    if (jsonObject.getString(JsonFormUtils.KEY).equalsIgnoreCase("Mother_Guardian_NRC")) {
                        jsonObject.put(JsonFormUtils.READ_ONLY, true);
                        jsonObject.put(JsonFormUtils.VALUE, Utils.getValue(childDetails.getColumnmaps(), "mother_nrc_number", true));
                    }
                    if (jsonObject.getString(JsonFormUtils.KEY).equalsIgnoreCase("Mother_Guardian_Number")) {
                        jsonObject.put(JsonFormUtils.VALUE, Utils.getValue(detailmaps, "Mother_Guardian_Number", true));
                    }
                    if (jsonObject.getString(JsonFormUtils.KEY).equalsIgnoreCase("Father_Guardian_Name")) {
                        jsonObject.put(JsonFormUtils.READ_ONLY, true);
                        jsonObject.put(JsonFormUtils.VALUE, Utils.getValue(detailmaps, "Father_Guardian_Name", true));
                    }
                    if (jsonObject.getString(JsonFormUtils.KEY).equalsIgnoreCase("Father_Guardian_NRC")) {
                        jsonObject.put(JsonFormUtils.READ_ONLY, true);
                        jsonObject.put(JsonFormUtils.VALUE, Utils.getValue(detailmaps, "Father_NRC_Number", true));
                    }
                    if (jsonObject.getString(JsonFormUtils.KEY).equalsIgnoreCase("First_Health_Facility_Contact")) {
                        jsonObject.put(JsonFormUtils.READ_ONLY, true);
                        jsonObject.put(JsonFormUtils.VALUE, Utils.getValue(detailmaps, "First_Health_Facility_Contact", true));
                    }
                    if (jsonObject.getString(JsonFormUtils.KEY).equalsIgnoreCase("Date_Birth")) {
                        jsonObject.put(JsonFormUtils.READ_ONLY, true);
                        DateTime dateTime = new DateTime(Utils.getValue(childDetails.getColumnmaps(), "dob", true));
                        Date dob = dateTime.toDate();
                        jsonObject.put(JsonFormUtils.VALUE, DATE_FORMAT.format(dob));
                    }
                    if (jsonObject.getString(JsonFormUtils.KEY).equalsIgnoreCase("Birth_Weight")) {
                        jsonObject.put(JsonFormUtils.READ_ONLY, true);
                        jsonObject.put(JsonFormUtils.VALUE, Utils.getValue(detailmaps, "Birth_Weight", true));
                    }
                    if (jsonObject.getString(JsonFormUtils.KEY).equalsIgnoreCase("Place_Birth")) {
                        jsonObject.put(JsonFormUtils.READ_ONLY, true);

                        String placeofnearth_Choice = Utils.getValue(detailmaps,"Place_Birth",true);
                        if(placeofnearth_Choice.equalsIgnoreCase("Health facility")){
                            placeofnearth_Choice = "Health facility";
                        }
                        if(placeofnearth_Choice.equalsIgnoreCase("Home")){
                            placeofnearth_Choice = "Home";
                        }
                        jsonObject.put(JsonFormUtils.VALUE, placeofnearth_Choice);

//                        jsonObject.put(JsonFormUtils.VALUE, Utils.getValue(detailmaps, "Place_Birth", true));
                    }
                    if (jsonObject.getString(JsonFormUtils.KEY).equalsIgnoreCase("Birth_Facility_Name")) {
                        jsonObject.put(JsonFormUtils.READ_ONLY, true);
                        jsonObject.put(JsonFormUtils.VALUE, Utils.getValue(detailmaps, "Birth_Facility_Name", true));
                    }
                    if (jsonObject.getString(JsonFormUtils.KEY).equalsIgnoreCase("Residential_Area")) {
                        jsonObject.put(JsonFormUtils.VALUE, Utils.getValue(detailmaps, "Residential_Area", true));
                    }
                    if (jsonObject.getString(JsonFormUtils.KEY).equalsIgnoreCase("Residential_Address")) {
                        jsonObject.put(JsonFormUtils.VALUE, Utils.getValue(detailmaps, "address2", true));
                    }
                    if (jsonObject.getString(JsonFormUtils.KEY).equalsIgnoreCase("Physical_Landmark")) {
                        jsonObject.put(JsonFormUtils.VALUE, Utils.getValue(detailmaps, "address1", true));
                    }
                    if (jsonObject.getString(JsonFormUtils.KEY).equalsIgnoreCase("CHW_Name")) {
                        jsonObject.put(JsonFormUtils.VALUE, Utils.getValue(detailmaps, "CHW_Name", true));
                    }
                    if (jsonObject.getString(JsonFormUtils.KEY).equalsIgnoreCase("CHW_Phone_Number")) {
                        jsonObject.put(JsonFormUtils.VALUE, Utils.getValue(detailmaps, "CHW_Phone_Number", true));
                    }
                    if (jsonObject.getString(JsonFormUtils.KEY).equalsIgnoreCase("PMTCT_Status")) {
                        jsonObject.put(JsonFormUtils.READ_ONLY, true);
                        jsonObject.put(JsonFormUtils.VALUE, Utils.getValue(detailmaps, "pmtct_status", true));
                    }

                }
//            intent.putExtra("json", form.toString());
//            startActivityForResult(intent, REQUEST_CODE_GET_JSON);
                return form.toString();
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        return "";
    }

    public void startFormActivity(String formName, String entityId, String metaData) {
        Context context = getOpenSRPContext();

        Intent intent = new Intent(getApplicationContext(), JsonFormActivity.class);

        intent.putExtra("json", metaData);
        startActivityForResult(intent, REQUEST_CODE_GET_JSON);


    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_GET_JSON) {
            if (resultCode == RESULT_OK) {

                try {
                    String jsonString = data.getStringExtra("json");
                    Log.d("JSONResult", jsonString);

                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                    AllSharedPreferences allSharedPreferences = new AllSharedPreferences(preferences);

                    JSONObject form = new JSONObject(jsonString);
                    if (form.getString("encounter_type").equals("Death")) {
                        JsonFormUtils.saveReportDeceased(this, getOpenSRPContext(), jsonString, allSharedPreferences.fetchRegisteredANM(), location_name, childDetails.entityId());
                    } else if (form.getString("encounter_type").equals("Birth Registration")) {
                        JsonFormUtils.editsave(this, getOpenSRPContext(), jsonString, allSharedPreferences.fetchRegisteredANM(), "Child_Photo", "child", "mother");
                    } else if (form.getString("encounter_type").equals("AEFI")) {
                        JsonFormUtils.saveAdverseEvent(jsonString, location_name,
                                childDetails.entityId(), allSharedPreferences.fetchRegisteredANM());
                    }
                child_data_fragment.LoadData();
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        } else if (requestCode == REQUEST_TAKE_PHOTO) {
            if (resultCode == RESULT_OK) {
                String imageLocation = currentfile.getAbsolutePath();
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                AllSharedPreferences allSharedPreferences = new AllSharedPreferences(preferences);

                JsonFormUtils.saveImage(this, allSharedPreferences.fetchRegisteredANM(), childDetails.entityId(), imageLocation);
                updateProfilePicture(gender);
            }
        }
    }

    @Override
    protected int getContentView() {
        return R.layout.child_detail_activity_simple_tabs;
    }

    @Override
    protected int getDrawerLayoutId() {
        return R.id.drawer_layout;
    }

    @Override
    protected int getToolbarId() {
        return R.id.child_detail_toolbar;
    }

    @Override
    protected Class onBackActivity() {
        return ChildImmunizationActivity.class;
    }

    private void profileWidget() {
        TextView profilename = (TextView) findViewById(R.id.name);
        TextView profileZeirID = (TextView) findViewById(R.id.idforclient);
        TextView profileage = (TextView) findViewById(R.id.ageforclient);
        String name = "";
        String childId = "";
        String dobString = "";
        String formattedAge = "";
        String formattedDob = "";
        if (isDataOk()) {
            name = Utils.getValue(childDetails.getColumnmaps(), "first_name", true)
                    + " " + Utils.getValue(childDetails.getColumnmaps(), "last_name", true);
            childId = Utils.getValue(childDetails.getColumnmaps(), "zeir_id", false);
            if (StringUtils.isNotBlank(childId)) {
                childId = childId.replace("-", "");
            }
            dobString = Utils.getValue(childDetails.getColumnmaps(), "dob", false);
            if (!TextUtils.isEmpty(dobString)) {
                DateTime dateTime = new DateTime(dobString);
                Date dob = dateTime.toDate();
                formattedDob = DATE_FORMAT.format(dob);
                long timeDiff = Calendar.getInstance().getTimeInMillis() - dob.getTime();

                if (timeDiff >= 0) {
                    formattedAge = DateUtils.getDuration(timeDiff);
                }
            }
        }


        profileage.setText(String.format("%s: %s", getString(R.string.age), formattedAge));
        profileZeirID.setText(String.format("%s: %s", getString(R.string.label_zeir), childId));
        profilename.setText(name);
        updateGenderViews();
        Gender gender = Gender.UNKNOWN;
        if (isDataOk()) {
            String genderString = Utils.getValue(childDetails, "gender", false);
            if (genderString != null && genderString.toLowerCase().equals("female")) {
                gender = Gender.FEMALE;
            } else if (genderString != null && genderString.toLowerCase().equals("male")) {
                gender = Gender.MALE;
            }
        }
        updateProfilePicture(gender);
        updateStatus();
    }

    public void updateStatus() {
        ImageView statusImage = (ImageView)findViewById(R.id.statusimage);
        TextView status_name = (TextView)findViewById(R.id.statusname);
        TextView status = (TextView)findViewById(R.id.status);
        if (details.containsKey(inactive) && details.get(inactive).equalsIgnoreCase(Boolean.TRUE.toString())) {
            statusImage.clearColorFilter();
            statusImage.setColorFilter(Color.TRANSPARENT);
            statusImage.setImageResource(R.drawable.ic_icon_status_inactive);
            status_name.setText("Inactive");
            status_name.setTextColor(getResources().getColor(R.color.dark_grey));
            status_name.setVisibility(View.VISIBLE);
            status.setText("status");
        }
        else if (details.containsKey(lostToFollowUp) && details.get(lostToFollowUp).equalsIgnoreCase(Boolean.TRUE.toString())) {
            statusImage.clearColorFilter();
            statusImage.setImageResource(R.drawable.ic_icon_status_losttofollowup);
            statusImage.setColorFilter(Color.TRANSPARENT);
//            status_name.setText("Lost to");
            status_name.setVisibility(View.GONE);
            status.setText("Lost to\nFollow-Up");
        }
//        if (!((details.containsKey(lostToFollowUp) && details.get(lostToFollowUp).equalsIgnoreCase(Boolean.TRUE.toString()))||(details.containsKey(inactive) && details.get(inactive).equalsIgnoreCase(Boolean.TRUE.toString())))){
//            statusImage.setImageResource(R.drawable.ic_icon_status_active);
//            statusImage.setColorFilter(getResources().getColor(R.color.alert_completed));
//            status_name.setText("Active");
//            status_name.setTextColor(getResources().getColor(R.color.alert_completed));
//            status_name.setVisibility(View.VISIBLE);
//            status.setText("status");
//        }
    }

    private String updateActivityTitle() {
        String name = "";
        if (isDataOk()) {
            name = Utils.getValue(childDetails.getColumnmaps(), "first_name", true)
                    + " " + Utils.getValue(childDetails.getColumnmaps(), "last_name", true);
        }
        return String.format("%s's %s", name, "Health Details");
    }

    private void updateProfilePicture(Gender gender) {
        this.gender = gender;
        if (isDataOk()) {
            ImageView profileImageIV = (ImageView) findViewById(R.id.profile_image_iv);

            if (childDetails.entityId() != null) {//image already in local storage most likey ):
                //set profile image by passing the client id.If the image doesn't exist in the image repository then download and save locally
                profileImageIV.setTag(org.ei.opensrp.R.id.entity_id, childDetails.entityId());
                DrishtiApplication.getCachedImageLoaderInstance().getImageByClientId(childDetails.entityId(), OpenSRPImageLoader.getStaticImageListener((ImageView) profileImageIV, ImageUtils.profileImageResourceByGender(gender), ImageUtils.profileImageResourceByGender(gender)));

            }
            profileImageIV.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dispatchTakePictureIntent();
                }
            });
        }
    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());

        adapter.addFragment(child_data_fragment, "Registration Data");
        adapter.addFragment(child_under_five_Fragment, "Under Five History");
        viewPager.setAdapter(adapter);
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File

            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                currentfile = photoFile;
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
//        mCurrentPhotoPath = "file:" + image.getAbsolutePath();
        return image;
    }

    private void updateGenderViews() {
        Gender gender = Gender.UNKNOWN;
        if (isDataOk()) {
            String genderString = Utils.getValue(childDetails, "gender", false);
            if (genderString != null && genderString.toLowerCase().equals("female")) {
                gender = Gender.FEMALE;
            } else if (genderString != null && genderString.toLowerCase().equals("male")) {
                gender = Gender.MALE;
            }
        }
        int[] colors = updateGenderViews(gender);
        int darkShade = colors[0];
        int normalShade = colors[1];
        int lightSade = colors[2];
        detailtoolbar.setBackground(new ColorDrawable(getResources().getColor(normalShade)));
        tabLayout.setTabTextColors(getResources().getColor(R.color.dark_grey), getResources().getColor(normalShade));
//        tabLayout.setSelectedTabIndicatorColor(getResources().getColor(normalShade));
        try {
            Field field = TabLayout.class.getDeclaredField("mTabStrip");
            field.setAccessible(true);
            Object ob = field.get(tabLayout);
            Class<?> c = Class.forName("android.support.design.widget.TabLayout$SlidingTabStrip");
            Method method = c.getDeclaredMethod("setSelectedIndicatorColor", int.class);
            method.setAccessible(true);
            method.invoke(ob, getResources().getColor(normalShade));//now its ok
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }


    }

    private boolean isDataOk() {
        return childDetails != null && childDetails.getDetails() != null;
    }

    @Override
    public void onVaccinateToday(ArrayList<VaccineWrapper> tags, View view) {
        if (tags != null && !tags.isEmpty()) {
            saveVaccine(tags, view);
        }
    }

    @Override
    public void onVaccinateEarlier(ArrayList<VaccineWrapper> tags, View view) {
        if (tags != null && !tags.isEmpty()) {
            saveVaccine(tags, view);
        }
    }

    @Override
    public void onUndoVaccination(VaccineWrapper tag, View view) {
        if (tag != null) {

            if (tag.getDbKey() != null) {
                final VaccineRepository vaccineRepository = VaccinatorApplication.getInstance().vaccineRepository();
                Long dbKey = tag.getDbKey();
                vaccineRepository.deleteVaccine(dbKey);

                tag.setUpdatedVaccineDate(null, false);
                tag.setRecordedDate(null);
                tag.setDbKey(null);


                List<Vaccine> vaccineList = vaccineRepository.findByEntityId(childDetails.entityId());

                ArrayList<VaccineWrapper> wrappers = new ArrayList<>();
                wrappers.add(tag);
                updateVaccineGroupViews(view, wrappers, vaccineList, true);
            }
        }
    }

    @Override
    public void onWeightTaken(WeightWrapper tag) {
        if (tag != null) {
            WeightRepository weightRepository = VaccinatorApplication.getInstance().weightRepository();
            Weight weight = new Weight();
            if (tag.getDbKey() != null) {
                weight = weightRepository.find(tag.getDbKey());
            }
            weight.setBaseEntityId(childDetails.entityId());
            weight.setKg(tag.getWeight());
            weight.setDate(tag.getUpdatedWeightDate().toDate());
            weight.setAnmId(getOpenSRPContext().allSharedPreferences().fetchRegisteredANM());
            if (StringUtils.isNotBlank(location_name)) {
                weight.setLocationId(location_name);
            }

            weightRepository.add(weight);

            tag.setDbKey(weight.getId());
            child_under_five_Fragment.loadview(false,true);
//            updateRecordWeightView(tag);
//            setLastModified(true);
        } else {
            child_under_five_Fragment.loadview(false,false);
        }
    }

    private void showWeightDialog() {
        FragmentTransaction ft = this.getFragmentManager().beginTransaction();
        android.app.Fragment prev = this.getFragmentManager().findFragmentByTag(DIALOG_TAG);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);


        String childName = constructChildName();
        String gender = getValue(childDetails.getColumnmaps(), "gender", true) + " " + getValue(childDetails, "gender", true);
        String motherFirstName = getValue(childDetails.getColumnmaps(), "mother_first_name", true);
        if (StringUtils.isBlank(childName) && StringUtils.isNotBlank(motherFirstName)) {
            childName = "B/o " + motherFirstName.trim();
        }
        String zeirId = getValue(childDetails.getColumnmaps(), "zeir_id", false);
        String duration = "";
        String dobString = getValue(childDetails.getColumnmaps(), "dob", false);
        if (StringUtils.isNotBlank(dobString)) {
            DateTime dateTime = new DateTime(getValue(childDetails.getColumnmaps(), "dob", false));
            duration = DateUtils.getDuration(dateTime);
        }

        Photo photo = ImageUtils.profilePhotoByClient(childDetails);

        WeightWrapper weightWrapper = new WeightWrapper();
        weightWrapper.setId(childDetails.entityId());
        WeightRepository wp = VaccinatorApplication.getInstance().weightRepository();
        List<Weight> weightlist = wp.findLast5(childDetails.entityId());
        if (weightlist.size() > 0) {
            weightWrapper.setWeight(weightlist.get(0).getKg());
            weightWrapper.setUpdatedWeightDate(new DateTime(weightlist.get(0).getDate()), false);
//            weightWrapper.setWeight(weight.getKg());
            weightWrapper.setDbKey(weightlist.get(0).getId());
        }
        weightWrapper.setGender(gender.toString());
        weightWrapper.setPatientName(childName);
        weightWrapper.setPatientNumber(zeirId);
        weightWrapper.setPatientAge(duration);
        weightWrapper.setPhoto(photo);
        weightWrapper.setPmtctStatus(getValue(childDetails.getColumnmaps(), "pmtct_status", false));

        EditWeightDialogFragment editWeightDialogFragment = EditWeightDialogFragment.newInstance(this, weightWrapper);
        editWeightDialogFragment.show(ft, DIALOG_TAG);

    }
    public void showWeightDialog(int i) {
        FragmentTransaction ft = this.getFragmentManager().beginTransaction();
        android.app.Fragment prev = this.getFragmentManager().findFragmentByTag(DIALOG_TAG);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);


        String childName = constructChildName();
        String gender = getValue(childDetails.getColumnmaps(), "gender", true) + " " + getValue(childDetails, "gender", true);
        String motherFirstName = getValue(childDetails.getColumnmaps(), "mother_first_name", true);
        if (StringUtils.isBlank(childName) && StringUtils.isNotBlank(motherFirstName)) {
            childName = "B/o " + motherFirstName.trim();
        }
        String zeirId = getValue(childDetails.getColumnmaps(), "zeir_id", false);
        String duration = "";
        String dobString = getValue(childDetails.getColumnmaps(), "dob", false);
        if (StringUtils.isNotBlank(dobString)) {
            DateTime dateTime = new DateTime(getValue(childDetails.getColumnmaps(), "dob", false));
            duration = DateUtils.getDuration(dateTime);
        }

        Photo photo = ImageUtils.profilePhotoByClient(childDetails);

        WeightWrapper weightWrapper = new WeightWrapper();
        weightWrapper.setId(childDetails.entityId());
        WeightRepository wp = VaccinatorApplication.getInstance().weightRepository();
        List<Weight> weightlist = wp.findLast5(childDetails.entityId());
//        if (weightlist.size() > i) {
            weightWrapper.setWeight(weightlist.get(i).getKg());
            weightWrapper.setUpdatedWeightDate(new DateTime(weightlist.get(i).getDate()), false);
//            weightWrapper.setWeight(weight.getKg());
            weightWrapper.setDbKey(weightlist.get(i).getId());
//        }
        weightWrapper.setGender(gender.toString());
        weightWrapper.setPatientName(childName);
        weightWrapper.setPatientNumber(zeirId);
        weightWrapper.setPatientAge(duration);
        weightWrapper.setPhoto(photo);
        weightWrapper.setPmtctStatus(getValue(childDetails.getColumnmaps(), "pmtct_status", false));

        EditWeightDialogFragment editWeightDialogFragment = EditWeightDialogFragment.newInstance(this, weightWrapper);
        editWeightDialogFragment.show(ft, DIALOG_TAG);

    }


    private String constructChildName() {
        String firstName = Utils.getValue(childDetails.getColumnmaps(), "first_name", true);
        String lastName = Utils.getValue(childDetails.getColumnmaps(), "last_name", true);
        return getName(firstName, lastName).trim();
    }

    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }


        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }

    private void saveVaccine(List<VaccineWrapper> tags, final View view) {
        if (tags.isEmpty()) {
            return;
        } else if (tags.size() == 1) {
            saveVaccine(tags.get(0));
            updateVaccineGroupViews(view);
        } else {
            VaccineWrapper[] arrayTags = tags.toArray(new VaccineWrapper[tags.size()]);
            SaveVaccinesTask backgroundTask = new SaveVaccinesTask();
            backgroundTask.setView(view);
            backgroundTask.execute(arrayTags);
        }
    }

    private class LaunchAdverseEventFormTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {
            try {
                JSONObject form = FormUtils.getInstance(getApplicationContext())
                        .getFormJson("adverse_event");
                if (form != null) {
                    JSONArray fields = form.getJSONObject("step1").getJSONArray("fields");
                    for (int i = 0; i < fields.length(); i++) {
                        if (fields.getJSONObject(i).getString("key").equals("Reaction_Vaccine")) {
                            boolean result = insertVaccinesGivenAsOptions(fields.getJSONObject(i));
                            if (result == false) {
                                return null;
                            }
                        }
                    }
                    return form.toString();
                }

            } catch (Exception e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
            return null;
        }

        @Override
        protected void onPostExecute(String metaData) {
            super.onPostExecute(metaData);
            if (metaData != null) {
                startFormActivity("adverse_event", childDetails.entityId(), metaData);
            } else {
                Toast.makeText(ChildDetailTabbedActivity.this, R.string.no_vaccine_record_found,
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private class SaveVaccinesTask extends AsyncTask<VaccineWrapper, Void, Void> {

        private View view;

        public void setView(View view) {
            this.view = view;
        }

        @Override
        protected void onPreExecute() {
            showProgressDialog();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            hideProgressDialog();
            updateVaccineGroupViews(view);
        }

        @Override
        protected Void doInBackground(VaccineWrapper... vaccineWrappers) {
            for (VaccineWrapper tag : vaccineWrappers) {
                saveVaccine(tag);
            }
            return null;
        }

    }

    private void updateVaccineGroupViews(View view) {
        if (view == null || !(view instanceof ImmunizationRowGroup)) {
            return;
        }
        final ImmunizationRowGroup vaccineGroup = (ImmunizationRowGroup) view;

        if (Looper.myLooper() == Looper.getMainLooper()) {
            vaccineGroup.updateViews();
        } else {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    vaccineGroup.updateViews();
                }
            });
        }
    }
    private void updateVaccineGroupViews(View view, final ArrayList<VaccineWrapper> wrappers, final List<Vaccine> vaccineList, final boolean undo) {
        if (view == null || !(view instanceof ImmunizationRowGroup)) {
            return;
        }
        final ImmunizationRowGroup vaccineGroup = (ImmunizationRowGroup) view;
        vaccineGroup.setModalOpen(false);

        if (Looper.myLooper() == Looper.getMainLooper()) {
            if(undo){
                vaccineGroup.setVaccineList(vaccineList);
                vaccineGroup.updateWrapperStatus(wrappers);
            }
            vaccineGroup.updateViews(wrappers);

        } else {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if(undo){
                        vaccineGroup.setVaccineList(vaccineList);
                        vaccineGroup.updateWrapperStatus(wrappers);
                    }
                    vaccineGroup.updateViews(wrappers);
                }
            });
        }
    }

    private void saveVaccine(VaccineWrapper tag) {
        VaccineRepository vaccineRepository = VaccinatorApplication.getInstance().vaccineRepository();

        Vaccine vaccine = new Vaccine();
        if (tag.getDbKey() != null) {
            vaccine = vaccineRepository.find(tag.getDbKey());
        }
        vaccine.setBaseEntityId(childDetails.entityId());
        vaccine.setName(tag.getName());
        vaccine.setDate(tag.getUpdatedVaccineDate().toDate());
        vaccine.setAnmId(getOpenSRPContext().allSharedPreferences().fetchRegisteredANM());
        if (StringUtils.isNotBlank(location_name)) {
            vaccine.setLocationId(location_name);
        }

        String lastChar = vaccine.getName().substring(vaccine.getName().length() - 1);
        if (StringUtils.isNumeric(lastChar)) {
            vaccine.setCalculation(Integer.valueOf(lastChar));
        } else {
            vaccine.setCalculation(-1);
        }
        vaccineRepository.add(vaccine);
        tag.setDbKey(vaccine.getId());
    }

    private String getReportDeceasedMetadata() {
        try {
            JSONObject form = FormUtils.getInstance(getApplicationContext()).getFormJson("report_deceased");
            if (form != null) {
                //inject zeir id into the form
                JSONObject stepOne = form.getJSONObject(JsonFormUtils.STEP1);
                JSONArray jsonArray = stepOne.getJSONArray(JsonFormUtils.FIELDS);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    if (jsonObject.getString(JsonFormUtils.KEY).equalsIgnoreCase("Date_Birth")) {
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy");
                        DateTime dateTime = new DateTime(Utils.getValue(childDetails.getColumnmaps(), "dob", true));
                        Date dob = dateTime.toDate();
                        jsonObject.put(JsonFormUtils.VALUE, simpleDateFormat.format(dob));
                        break;
                    }
                }
            }
            return form == null ? null : form.toString();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        return "";
    }

    private boolean insertVaccinesGivenAsOptions(JSONObject question) throws JSONException {
        VaccineRepository vaccineRepository = VaccinatorApplication.getInstance().vaccineRepository();
        JSONObject omrsChoicesTemplate = question.getJSONObject("openmrs_choice_ids");
        JSONObject omrsChoices = new JSONObject();
        JSONArray choices = new JSONArray();
        List<Vaccine> vaccineList = vaccineRepository.findByEntityId(childDetails.entityId());

        boolean ok = false;
        if (vaccineList != null && vaccineList.size() > 0) {
            ok = true;
            for (int i = vaccineList.size() - 1; i >= 0; i--) {
                Vaccine curVaccine = vaccineList.get(i);
                String name = VaccinatorUtils.getVaccineDisplayName(this, curVaccine.getName())
                        + " (" + DATE_FORMAT.format(curVaccine.getDate()) + ")";
                choices.put(name);

                Iterator<String> vaccineGroupNames = omrsChoicesTemplate.keys();
                while (vaccineGroupNames.hasNext()) {
                    String curGroupName = vaccineGroupNames.next();
                    if (curVaccine.getName().toLowerCase().contains(curGroupName.toLowerCase())) {
                        omrsChoices.put(name, omrsChoicesTemplate.getString(curGroupName));
                        break;
                    }
                }
            }
        }

        question.put("values", choices);
        question.put("openmrs_choice_ids", omrsChoices);

        return ok;
    }

    private void updateClientAttribute(String attributeName, Object attributeValue) {
        try {
            Date date= new Date();
            PathRepository db = (PathRepository) VaccinatorApplication.getInstance().getRepository();
            ECSyncUpdater ecUpdater = ECSyncUpdater.getInstance(this);

            JSONObject client = db.getClientByBaseEntityId(childDetails.entityId());
            JSONObject attributes = client.getJSONObject(JsonFormUtils.attributes);
            attributes.put(attributeName, attributeValue);
            client.remove(JsonFormUtils.attributes);
            client.put(JsonFormUtils.attributes, attributes);
            db.addorUpdateClient(childDetails.entityId(), client);


            detailsRepository.add(childDetails.entityId(), attributeName, attributeValue.toString(), new Date().getTime());
            ContentValues contentValues = new ContentValues();
            //Add the base_entity_id
            contentValues.put(attributeName.toLowerCase(), attributeValue.toString());
            int id = db.getWritableDatabase().update("ec_child", contentValues, "base_entity_id" + "=?", new String[]{childDetails.entityId()});


            Event event = (Event) new Event()
                    .withBaseEntityId(childDetails.entityId())
                    .withEventDate(new Date())
                    .withEventType(JsonFormUtils.encounterType)
                    .withLocationId(allSharedPreferences.fetchCurrentLocality())
                    .withProviderId(allSharedPreferences.fetchRegisteredANM())
                    .withEntityType("child")
                    .withFormSubmissionId(JsonFormUtils.generateRandomUUIDString())
                    .withDateCreated(new Date());

           JsonFormUtils.addMetaData(this, event, date);
            JSONObject eventJson = new JSONObject(JsonFormUtils.gson.toJson(event));
            db.addEvent(childDetails.entityId(),eventJson);
            long lastSyncTimeStamp = allSharedPreferences.fetchLastUpdatedAtDate(0);
            Date lastSyncDate = new Date(lastSyncTimeStamp);
            PathClientProcessor.getInstance(this).processClient(ecUpdater.getEvents(lastSyncDate, BaseRepository.TYPE_Unsynced));
            allSharedPreferences.saveLastUpdatedAtDate(lastSyncDate.getTime());

            //update details
            details = detailsRepository.getAllDetailsForClient(childDetails.entityId());
            if(childDetails.getColumnmaps().containsKey(attributeName)){
                childDetails.getColumnmaps().put(attributeName,attributeValue.toString());
            }
            details.putAll(childDetails.getColumnmaps());



        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        details = detailsRepository.getAllDetailsForClient(childDetails.entityId());
        //details.putAll(childDetails.getColumnmaps());
        //):( prrrr
        childDetails.getColumnmaps().putAll(details);
        updateActivityTitle();
        initiallization();
    }
}
