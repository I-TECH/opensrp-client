package org.ei.opensrp.path.activity;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.ei.opensrp.commonregistry.AllCommonsRepository;
import org.ei.opensrp.commonregistry.CommonPersonObjectClient;
import org.ei.opensrp.domain.Alert;
import org.ei.opensrp.domain.Vaccine;
import org.ei.opensrp.domain.Weight;
import org.ei.opensrp.path.R;
import org.ei.opensrp.path.application.VaccinatorApplication;
import org.ei.opensrp.path.db.VaccineRepo;
import org.ei.opensrp.path.domain.Photo;
import org.ei.opensrp.path.domain.RegisterClickables;
import org.ei.opensrp.path.domain.VaccineWrapper;
import org.ei.opensrp.path.domain.WeightWrapper;
import org.ei.opensrp.path.fragment.RecordWeightDialogFragment;
import org.ei.opensrp.path.fragment.UndoVaccinationDialogFragment;
import org.ei.opensrp.path.fragment.VaccinationDialogFragment;
import org.ei.opensrp.path.listener.VaccinationActionListener;
import org.ei.opensrp.path.listener.WeightActionListener;
import org.ei.opensrp.path.repository.VaccineRepository;
import org.ei.opensrp.path.repository.WeightRepository;
import org.ei.opensrp.path.toolbar.LocationSwitcherToolbar;
import org.ei.opensrp.path.view.VaccineGroup;
import org.ei.opensrp.repository.DetailsRepository;
import org.ei.opensrp.service.AlertService;
import org.ei.opensrp.util.OpenSRPImageLoader;
import org.ei.opensrp.view.activity.DrishtiApplication;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.opensrp.api.constants.Gender;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import util.DateUtils;
import util.ImageUtils;
import util.JsonFormUtils;
import util.Utils;
import util.VaccinateActionUtils;
import util.VaccinatorUtils;

import static util.Utils.getName;
import static util.Utils.getValue;

/**
 * Created by Jason Rogena - jrogena@ona.io on 16/02/2017.
 */

public class ChildImmunizationActivity extends BaseActivity
        implements LocationSwitcherToolbar.OnLocationChangeListener, WeightActionListener, VaccinationActionListener {

    private static final String TAG = "ChildImmunoActivity";
    private static final String VACCINES_FILE = "vaccines.json";
    private static final String EXTRA_CHILD_DETAILS = "child_details";
    private static final String EXTRA_REGISTER_CLICKABLES = "register_clickables";
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");
    private static final String DIALOG_TAG = "ChildImmunoActivity_DIALOG_TAG";
    private ArrayList<VaccineGroup> vaccineGroups;

    // Views
    private LocationSwitcherToolbar toolbar;

    // Data
    private CommonPersonObjectClient childDetails;
    private RegisterClickables registerClickables;
    private DetailsRepository detailsRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        detailsRepository = org.ei.opensrp.Context.getInstance().detailsRepository();

        toolbar = (LocationSwitcherToolbar) getToolbar();
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ChildImmunizationActivity.this, ChildSmartRegisterActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });
        toolbar.setOnLocationChangeListener(this);
//       View view= toolbar.findViewById(R.id.immunization_separator);
//        view.setBackground(R.drawable.vertical_seperator_female);

        // Get child details from bundled data
        Bundle extras = this.getIntent().getExtras();
        if (extras != null) {
            Serializable serializable = extras.getSerializable(EXTRA_CHILD_DETAILS);
            if (serializable != null && serializable instanceof CommonPersonObjectClient) {
                childDetails = (CommonPersonObjectClient) serializable;
            }

            serializable = extras.getSerializable(EXTRA_REGISTER_CLICKABLES);
            if (serializable != null && serializable instanceof RegisterClickables) {
                registerClickables = (RegisterClickables) serializable;
            }
        }

        toolbar.init(this);
        setLastModified(false);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(EXTRA_CHILD_DETAILS, childDetails);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        Serializable serializable = savedInstanceState.getSerializable(EXTRA_CHILD_DETAILS);
        if (serializable != null && serializable instanceof CommonPersonObjectClient) {
            childDetails = (CommonPersonObjectClient) serializable;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(vaccineGroups!=null){
            LinearLayout vaccineGroupCanvasLL = (LinearLayout) findViewById(R.id.vaccine_group_canvas_ll);
            vaccineGroupCanvasLL.removeAllViews();
            vaccineGroups = null;
        }
        updateViews();
    }

    private boolean isDataOk() {
        return childDetails != null && childDetails.getDetails() != null;
    }

    private void updateViews() {
        ((LinearLayout)findViewById(R.id.profile_name_layout)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchDetailActivity(ChildImmunizationActivity.this, childDetails, null);
            }
        });
        // TODO: update all views using child data
        Map<String, String> details = detailsRepository.getAllDetailsForClient(childDetails.entityId());
        //details.putAll(childDetails.getColumnmaps());
        //):( prrrr
        childDetails.getColumnmaps().putAll(details);
        updateGenderViews();
        toolbar.setTitle(updateActivityTitle());
        updateAgeViews();
        updateChildIdViews();

        WeightRepository weightRepository = VaccinatorApplication.getInstance().weightRepository();

        VaccineRepository vaccineRepository =  VaccinatorApplication.getInstance().vaccineRepository();

        AlertService alertService = getOpenSRPContext().alertService();

        UpdateViewTask updateViewTask = new UpdateViewTask();
        updateViewTask.setWeightRepository(weightRepository);
        updateViewTask.setVaccineRepository(vaccineRepository);
        updateViewTask.setAlertService(alertService);
        updateViewTask.setRegisterClickables(registerClickables);
        Utils.startAsyncTask(updateViewTask, null);
    }

    private void updateProfilePicture(Gender gender) {
        if (isDataOk()) {
            ImageView profileImageIV = (ImageView) findViewById(R.id.profile_image_iv);

            if (childDetails.entityId() != null) {//image already in local storage most likey ):
                //set profile image by passing the client id.If the image doesn't exist in the image repository then download and save locally
                profileImageIV.setTag(org.ei.opensrp.R.id.entity_id, childDetails.entityId());
                DrishtiApplication.getCachedImageLoaderInstance().getImageByClientId(childDetails.entityId(), OpenSRPImageLoader.getStaticImageListener((ImageView) profileImageIV, ImageUtils.profileImageResourceByGender(gender), ImageUtils.profileImageResourceByGender(gender)));

            }
        }
    }

    private void updateChildIdViews() {
        String name = "";
        String childId = "";
        if (isDataOk()) {
            name = constructChildName();
            childId = Utils.getValue(childDetails.getColumnmaps(), "zeir_id", false);
        }

        TextView nameTV = (TextView) findViewById(R.id.name_tv);
        nameTV.setText(name);
        TextView childIdTV = (TextView) findViewById(R.id.child_id_tv);
        childIdTV.setText(String.format("%s: %s", getString(R.string.label_zeir), childId));
    }

    private void updateAgeViews() {
        String dobString = "";
        String formattedAge = "";
        String formattedDob = "";
        if (isDataOk()) {
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
        TextView dobTV = (TextView) findViewById(R.id.dob_tv);
        dobTV.setText(String.format("%s: %s", getString(R.string.birthdate), formattedDob));
        TextView ageTV = (TextView) findViewById(R.id.age_tv);
        ageTV.setText(String.format("%s: %s", getString(R.string.age), formattedAge));
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
        updateGenderViews(gender);
    }

    @Override
    protected int[] updateGenderViews(Gender gender) {
        int[] selectedColor = super.updateGenderViews(gender);

        String identifier = getString(R.string.neutral_sex_id);
        int toolbarResource = 0;
        if (gender.equals(Gender.FEMALE)) {
            toolbarResource = R.drawable.vertical_separator_female;
            identifier = getString(R.string.female_sex_id);
        } else if (gender.equals(Gender.MALE)) {
            toolbarResource = R.drawable.vertical_separator_male;
            identifier = getString(R.string.male_sex_id);
        }
        toolbar.updateSeparatorView(toolbarResource);

        TextView childSiblingsTV = (TextView) findViewById(R.id.child_siblings_tv);
        childSiblingsTV.setText(
                String.format(getString(R.string.child_siblings), identifier).toUpperCase());
        updateProfilePicture(gender);

        return selectedColor;
    }

    private void updateVaccinationViews(List<Vaccine> vaccineList, List<Alert> alerts) {
        if (vaccineGroups == null) {
            vaccineGroups = new ArrayList<>();
            LinearLayout vaccineGroupCanvasLL = (LinearLayout) findViewById(R.id.vaccine_group_canvas_ll);
            String supportedVaccinesString = VaccinatorUtils.getSupportedVaccines(this);
            try {
                JSONArray supportedVaccines = new JSONArray(supportedVaccinesString);
                for (int i = 0; i < supportedVaccines.length(); i++) {
                    VaccineGroup curGroup = new VaccineGroup(this);
                    curGroup.setData(supportedVaccines.getJSONObject(i), childDetails, vaccineList, alerts);
                    curGroup.setOnRecordAllClickListener(new VaccineGroup.OnRecordAllClickListener() {
                        @Override
                        public void onClick(VaccineGroup vaccineGroup, ArrayList<VaccineWrapper> dueVaccines) {
                            addVaccinationDialogFragment(dueVaccines, vaccineGroup);
                        }
                    });
                    curGroup.setOnVaccineClickedListener(new VaccineGroup.OnVaccineClickedListener() {
                        @Override
                        public void onClick(VaccineGroup vaccineGroup, VaccineWrapper vaccine) {
                            ArrayList<VaccineWrapper> vaccineWrappers = new ArrayList<VaccineWrapper>();
                            vaccineWrappers.add(vaccine);
                            addVaccinationDialogFragment(vaccineWrappers, vaccineGroup);
                        }
                    });
                    curGroup.setOnVaccineUndoClickListener(new VaccineGroup.OnVaccineUndoClickListener() {
                        @Override
                        public void onUndoClick(VaccineGroup vaccineGroup, VaccineWrapper vaccine) {
                            addVaccineUndoDialogFragment(vaccineGroup, vaccine);
                        }
                    });
                    vaccineGroupCanvasLL.addView(curGroup);
                    vaccineGroups.add(curGroup);
                }
            } catch (JSONException e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
        }

        if (!VaccinateActionUtils.hasVaccine(vaccineList, VaccineRepo.Vaccine.bcg2)) {
            if (VaccinateActionUtils.hasAlert(alerts, VaccineRepo.Vaccine.bcg2)) {
                Alert alert = VaccinateActionUtils.getAlert(alerts, VaccineRepo.Vaccine.bcg2);
                if (!alert.isComplete()) {
                    showCheckBcgScarNotification(alert);
                }
            }
        }
    }

    private void addVaccineUndoDialogFragment(VaccineGroup vaccineGroup, VaccineWrapper vaccineWrapper) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag(DIALOG_TAG);
        if (prev != null) {
            ft.remove(prev);
        }

        ft.addToBackStack(null);
        vaccineGroup.setModalOpen(true);

        UndoVaccinationDialogFragment undoVaccinationDialogFragment = UndoVaccinationDialogFragment.newInstance(vaccineWrapper);
        undoVaccinationDialogFragment.show(ft, DIALOG_TAG);
    }

    private void updateRecordWeightView(Weight weight) {

        String childName = constructChildName();
        String gender = getValue(childDetails.getColumnmaps(), "gender", true);
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
        weightWrapper.setGender(gender);
        weightWrapper.setPatientName(childName);
        weightWrapper.setPatientNumber(zeirId);
        weightWrapper.setPatientAge(duration);
        weightWrapper.setPhoto(photo);
        weightWrapper.setPmtctStatus(getValue(childDetails.getColumnmaps(), "pmtct_status", false));

        if (weight != null) {
            weightWrapper.setWeight(weight.getKg());
            weightWrapper.setDbKey(weight.getId());
            weightWrapper.setUpdatedWeightDate(new DateTime(weight.getDate()), false);
        }

        updateRecordWeightView(weightWrapper);
    }

    private void updateRecordWeightView(WeightWrapper weightWrapper) {
        View recordWeight = findViewById(R.id.record_weight);
        recordWeight.setClickable(true);
        recordWeight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showWeightDialog(view);
            }
        });

        recordWeight.setBackground(getResources().getDrawable(R.drawable.record_weight_bg));
        if (weightWrapper.getDbKey() != null && weightWrapper.getWeight() != null) {
            String weight = weightWrapper.getWeight().toString();
            TextView recordWeightText = (TextView) findViewById(R.id.record_weight_text);
            recordWeightText.setText(weight.trim() + " kg");

            ImageView recordWeightCheck = (ImageView) findViewById(R.id.record_weight_check);
            recordWeightCheck.setVisibility(View.VISIBLE);

            if (weightWrapper.getUpdatedWeightDate() != null) {
                long timeDiff = Calendar.getInstance().getTimeInMillis()
                        - weightWrapper.getUpdatedWeightDate().getMillis();

                if (timeDiff < TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS)) {
                    //disable the button
                    recordWeight.setClickable(false);
                    recordWeight.setBackground(new ColorDrawable(getResources()
                            .getColor(android.R.color.transparent)));
                }
            }
        }

        recordWeight.setTag(weightWrapper);

    }

    private void showWeightDialog(View view) {
        FragmentTransaction ft = this.getFragmentManager().beginTransaction();
        Fragment prev = this.getFragmentManager().findFragmentByTag(DIALOG_TAG);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        WeightWrapper weightWrapper = (WeightWrapper) view.getTag();
        RecordWeightDialogFragment recordWeightDialogFragment = RecordWeightDialogFragment.newInstance(weightWrapper);
        recordWeightDialogFragment.show(ft, DIALOG_TAG);

    }

    private String readAssetContents(String path) {
        String fileContents = null;
        try {
            InputStream is = getAssets().open(path);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            fileContents = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            android.util.Log.e(TAG, ex.toString(), ex);
        }

        return fileContents;
    }

    public static void launchActivity(Context fromContext, CommonPersonObjectClient childDetails, RegisterClickables registerClickables) {
        Intent intent = new Intent(fromContext, ChildImmunizationActivity.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable(EXTRA_CHILD_DETAILS, childDetails);
        bundle.putSerializable(EXTRA_REGISTER_CLICKABLES, registerClickables);
        intent.putExtras(bundle);

        fromContext.startActivity(intent);
    }
    public void launchDetailActivity(Context fromContext, CommonPersonObjectClient childDetails, RegisterClickables registerClickables) {
        Intent intent = new Intent(fromContext, ChildDetailTabbedActivity.class);
        Bundle bundle = new Bundle();
        try {
            bundle.putString("location_name", JsonFormUtils.getOpenMrsLocationId(getOpenSRPContext(), toolbar.getCurrentLocation()));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        bundle.putSerializable(EXTRA_CHILD_DETAILS, childDetails);
        bundle.putSerializable(EXTRA_REGISTER_CLICKABLES, registerClickables);
        intent.putExtras(bundle);

        fromContext.startActivity(intent);
    }

    private String updateActivityTitle() {
        String name = "";
        if (isDataOk()) {
            name = constructChildName();
        }
        return String.format("%s > %s", getString(R.string.app_name), name.trim());
    }

    @Override
    public void onLocationChanged(final String newLocation) {
        // TODO: Do whatever needs to be done when the location is changed
    }

    @Override
    protected int getContentView() {
        return R.layout.activity_child_immunization;
    }

    @Override
    protected int getDrawerLayoutId() {
        return R.id.drawer_layout;
    }

    @Override
    protected int getToolbarId() {
        return LocationSwitcherToolbar.TOOLBAR_ID;
    }

    @Override
    protected Class onBackActivity() {
        return ChildSmartRegisterActivity.class;
    }

    @Override
    public void onWeightTaken(WeightWrapper tag) {
        if (tag != null) {
            final WeightRepository weightRepository = VaccinatorApplication.getInstance().weightRepository();
            Weight weight = new Weight();
            if (tag.getDbKey() != null) {
                weight = weightRepository.find(tag.getDbKey());
            }
            weight.setBaseEntityId(childDetails.entityId());
            weight.setKg(tag.getWeight());
            weight.setDate(tag.getUpdatedWeightDate().toDate());
            weight.setAnmId(getOpenSRPContext().allSharedPreferences().fetchRegisteredANM());
            try {
                weight.setLocationId(JsonFormUtils.getOpenMrsLocationId(getOpenSRPContext(),
                        toolbar.getCurrentLocation()));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            weightRepository.add(weight);

            tag.setDbKey(weight.getId());
            updateRecordWeightView(tag);
            setLastModified(true);
        }
    }

    @Override
    public void onVaccinateToday(ArrayList<VaccineWrapper> tags, View v) {
        if (tags != null && !tags.isEmpty()) {
            View view = getLastOpenedView();
            saveVaccine(tags, view);
        }
    }

    @Override
    public void onVaccinateEarlier(ArrayList<VaccineWrapper> tags, View v) {
        if (tags != null && !tags.isEmpty()) {
            View view = getLastOpenedView();
            saveVaccine(tags, view);
        }
    }

    @Override
    public void onUndoVaccination(VaccineWrapper tag, View v) {
        if (tag != null) {

            if (tag.getDbKey() != null) {
                final VaccineRepository vaccineRepository = VaccinatorApplication.getInstance().vaccineRepository();
                Long dbKey = tag.getDbKey();
                vaccineRepository.deleteVaccine(dbKey);

                tag.setUpdatedVaccineDate(null, false);
                tag.setRecordedDate(null);
                tag.setDbKey(null);

                View view = getLastOpenedView();

                List<Vaccine> vaccineList = vaccineRepository.findByEntityId(childDetails.entityId());

                ArrayList<VaccineWrapper> wrappers = new ArrayList<>();
                wrappers.add(tag);
                updateVaccineGroupViews(view, wrappers, vaccineList, true);
            }
        }
    }

    public void addVaccinationDialogFragment(ArrayList<VaccineWrapper> vaccineWrappers, VaccineGroup vaccineGroup) {

        FragmentTransaction ft = this.getFragmentManager().beginTransaction();
        Fragment prev = this.getFragmentManager().findFragmentByTag(DIALOG_TAG);
        if (prev != null) {
            ft.remove(prev);
        }

        ft.addToBackStack(null);
        vaccineGroup.setModalOpen(true);

        VaccinationDialogFragment vaccinationDialogFragment = VaccinationDialogFragment.newInstance(vaccineWrappers);
        vaccinationDialogFragment.show(ft, DIALOG_TAG);
    }

    public void performRegisterActions(RegisterClickables registerClickables) {
        if (registerClickables != null) {
            if (registerClickables.isRecordWeight()) {
                final View recordWeight = findViewById(R.id.record_weight);
                recordWeight.post(new Runnable() {
                    @Override
                    public void run() {
                        recordWeight.performClick();
                    }
                });
            } else if (registerClickables.isRecordAll()) {
                performRecordAllClick(0);
            }
        }
    }

    private void performRecordAllClick(final int index) {
        if (vaccineGroups != null && vaccineGroups.size() > index) {
            final VaccineGroup vaccineGroup = vaccineGroups.get(index);
            vaccineGroup.post(new Runnable() {
                @Override
                public void run() {
                    ArrayList<VaccineWrapper> vaccineWrappers = vaccineGroup.getDueVaccines();
                    if (!vaccineWrappers.isEmpty()) {
                        final TextView recordAllTV = (TextView) vaccineGroup.findViewById(R.id.record_all_tv);
                        recordAllTV.post(new Runnable() {
                            @Override
                            public void run() {
                                recordAllTV.performClick();
                            }
                        });
                    } else {
                        performRecordAllClick(index + 1);
                    }
                }
            });
        }
    }

    private void saveVaccine(ArrayList<VaccineWrapper> tags, final View view) {
        if (tags.isEmpty()) {
            return;
        }

        VaccineRepository vaccineRepository = VaccinatorApplication.getInstance().vaccineRepository();

        VaccineWrapper[] arrayTags = tags.toArray(new VaccineWrapper[tags.size()]);
        SaveVaccinesTask backgroundTask = new SaveVaccinesTask();
        backgroundTask.setVaccineRepository(vaccineRepository);
        backgroundTask.setView(view);
        Utils.startAsyncTask(backgroundTask, arrayTags);

    }

    private void saveVaccine(VaccineRepository vaccineRepository, VaccineWrapper tag) {
        if (tag.getUpdatedVaccineDate() == null) {
            return;
        }



        Vaccine vaccine = new Vaccine();
        if (tag.getDbKey() != null) {
            vaccine = vaccineRepository.find(tag.getDbKey());
        }
        vaccine.setBaseEntityId(childDetails.entityId());
        vaccine.setName(tag.getName());
        vaccine.setDate(tag.getUpdatedVaccineDate().toDate());
        vaccine.setAnmId(getOpenSRPContext().allSharedPreferences().fetchRegisteredANM());
        try {
            vaccine.setLocationId(JsonFormUtils.getOpenMrsLocationId(getOpenSRPContext(),
                    toolbar.getCurrentLocation()));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String lastChar = vaccine.getName().substring(vaccine.getName().length() - 1);
        if (StringUtils.isNumeric(lastChar)) {
            vaccine.setCalculation(Integer.valueOf(lastChar));
        } else {
            vaccine.setCalculation(-1);
        }
        vaccineRepository.add(vaccine);
        tag.setDbKey(vaccine.getId());
        setLastModified(true);
    }

    private void updateVaccineGroupViews(View view, final ArrayList<VaccineWrapper> wrappers, List<Vaccine> vaccineList) {
        updateVaccineGroupViews(view, wrappers, vaccineList, false);
    }

    private void updateVaccineGroupViews(View view, final ArrayList<VaccineWrapper> wrappers, final List<Vaccine> vaccineList, final boolean undo) {
        if (view == null || !(view instanceof VaccineGroup)) {
            return;
        }
        final VaccineGroup vaccineGroup = (VaccineGroup) view;
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

    private void showRecordWeightNotification() {
        showNotification(R.string.record_weight_notification, R.drawable.ic_weight_notification,
                R.string.record_weight,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        View recordWeight = findViewById(R.id.record_weight);
                        showWeightDialog(recordWeight);
                        hideNotification();
                    }
                }, R.string.cancel, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        hideNotification();
                    }
                }, null);
    }

    private void showCheckBcgScarNotification(Alert alert) {
        showNotification(R.string.check_child_bcg_scar, R.drawable.ic_check_bcg_scar,
                R.string.ok_button_label, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        hideNotification();
                        Alert alert = (Alert) v.getTag();
                        if(alert != null) {
                            new MarkAlertAsDoneTask(getOpenSRPContext().alertService())
                                    .execute(alert);
                        }
                    }
                }, 0, null, alert);
    }

    private class MarkAlertAsDoneTask extends AsyncTask<Alert, Void, Void> {
        private final AlertService alertService;

        public MarkAlertAsDoneTask(AlertService alertService) {
            this.alertService = alertService;
        }

        @Override
        protected Void doInBackground(Alert... params) {
            for (int i = 0; i < params.length; i++) {
                alertService.changeAlertStatusToComplete(params[i].caseId(), params[i].visitCode());
            }
            return null;
        }
    }

    private class SaveVaccinesTask extends AsyncTask<VaccineWrapper, Void, Pair<ArrayList<VaccineWrapper>, List<Vaccine>>> {

        private View view;
        private VaccineRepository vaccineRepository;

        public void setView(View view) {
            this.view = view;
        }

        public void setVaccineRepository(VaccineRepository vaccineRepository) {
            this.vaccineRepository = vaccineRepository;
        }

        @Override
        protected void onPreExecute() {
            showProgressDialog();
        }

        @Override
        protected void onPostExecute(Pair<ArrayList<VaccineWrapper>, List<Vaccine>> pair) {
            hideProgressDialog();
            updateVaccineGroupViews(view, pair.first, pair.second);
            View recordWeight = findViewById(R.id.record_weight);
            WeightWrapper weightWrapper = (WeightWrapper) recordWeight.getTag();
            if (weightWrapper == null || weightWrapper.getWeight() == null) {
                showRecordWeightNotification();
            }
        }

        @Override
        protected Pair<ArrayList<VaccineWrapper>, List<Vaccine>> doInBackground(VaccineWrapper... vaccineWrappers) {

            ArrayList<VaccineWrapper> list = new ArrayList<>();
            if(vaccineRepository != null) {
                for (VaccineWrapper tag : vaccineWrappers) {
                    saveVaccine(vaccineRepository, tag);
                    list.add(tag);
                }
            }

            List<Vaccine> vaccineList = vaccineRepository.findByEntityId(childDetails.entityId());
            Pair<ArrayList<VaccineWrapper>, List<Vaccine>> pair =  new Pair<>(list, vaccineList);
            return pair;
        }
    }

    private String constructChildName() {
        String firstName = Utils.getValue(childDetails.getColumnmaps(), "first_name", true);
        String lastName = Utils.getValue(childDetails.getColumnmaps(), "last_name", true);
        return getName(firstName, lastName).trim();
    }

    @Override
    public void finish() {
        if (isLastModified()) {
            String tableName = "ec_child";
            AllCommonsRepository allCommonsRepository = getOpenSRPContext().allCommonsRepositoryobjects(tableName);
            ContentValues contentValues = new ContentValues();
            contentValues.put("last_interacted_with", (new Date()).getTime());
            allCommonsRepository.update(tableName, contentValues, childDetails.entityId());
            allCommonsRepository.updateSearch(childDetails.entityId());
        }
        super.finish();
    }

    private boolean isLastModified() {
        VaccinatorApplication application = (VaccinatorApplication) getApplication();
        return application.isLastModified();
    }

    private void setLastModified(boolean lastModified) {
        VaccinatorApplication application = (VaccinatorApplication) getApplication();
        if (lastModified != application.isLastModified()) {
            application.setLastModified(lastModified);
        }
    }

    private VaccineGroup getLastOpenedView() {
        if (vaccineGroups == null) {
            return null;
        }

        for (VaccineGroup vaccineGroup : vaccineGroups) {
            if (vaccineGroup.isModalOpen()) {
                return vaccineGroup;
            }
        }

        return null;
    }

    private class UpdateViewTask extends AsyncTask<Void, Void, Triple<Weight, List<Vaccine>, List<Alert>>> {

        private VaccineRepository vaccineRepository;
        private WeightRepository weightRepository;
        private AlertService alertService;
        private RegisterClickables registerClickables;

        public void setVaccineRepository(VaccineRepository vaccineRepository) {
            this.vaccineRepository = vaccineRepository;
        }

        public void setWeightRepository(WeightRepository weightRepository) {
            this.weightRepository = weightRepository;
        }

        public void setAlertService(AlertService alertService) {
            this.alertService = alertService;
        }

        public void setRegisterClickables(RegisterClickables registerClickables) {
            this.registerClickables = registerClickables;
        }

        @Override
        protected void onPreExecute() {
            showProgressDialog(getString(R.string.updating_dialog_title), null);
        }

        @Override
        protected void onPostExecute(Triple<Weight, List<Vaccine>, List<Alert>> triple) {
            hideProgressDialog();
            updateRecordWeightView(triple.getLeft());
            updateVaccinationViews(triple.getMiddle(), triple.getRight());
            performRegisterActions(registerClickables);
        }

        @Override
        protected Triple<Weight, List<Vaccine>, List<Alert>> doInBackground(Void... voids) {
            List<Vaccine> vaccineList = new ArrayList<>();
            Weight weight = null;
            List<Alert> alertList = new ArrayList<>();
            if (vaccineRepository != null) {
                vaccineList = vaccineRepository.findByEntityId(childDetails.entityId());

            }
            if (weightRepository != null) {
                weight = weightRepository.findUnSyncedByEntityId(childDetails.entityId());
            }

            if (alertService != null) {
                alertList = alertService.findByEntityIdAndAlertNames(childDetails.entityId(),
                        VaccinateActionUtils.allAlertNames("child"));
            }

            Triple<Weight, List<Vaccine>, List<Alert>> triple = Triple.of(weight, vaccineList, alertList);
            return triple;
        }
    }
}
