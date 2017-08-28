package util;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.clientandeventmodel.Address;
import org.smartregister.clientandeventmodel.DateUtil;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;
import org.smartregister.commonregistry.AllCommonsRepository;
import org.smartregister.domain.ProfileImage;
import org.smartregister.growthmonitoring.domain.Weight;
import org.smartregister.growthmonitoring.repository.WeightRepository;
import org.smartregister.immunization.domain.Vaccine;
import org.smartregister.immunization.repository.VaccineRepository;
import org.smartregister.immunization.util.VaccinatorUtils;
import org.smartregister.path.R;
import org.smartregister.path.activity.PathJsonFormActivity;
import org.smartregister.path.application.VaccinatorApplication;
import org.smartregister.path.domain.Client;
import org.smartregister.path.domain.FormEntityConstants;
import org.smartregister.path.repository.LocationRepository;
import org.smartregister.path.repository.UniqueIdRepository;
import org.smartregister.path.sync.ECSyncUpdater;
import org.smartregister.path.sync.PathClientProcessor;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.repository.BaseRepository;
import org.smartregister.repository.EventClientRepository;
import org.smartregister.repository.ImageRepository;
import org.smartregister.sync.ClientProcessor;
import org.smartregister.util.AssetHandler;
import org.smartregister.util.FormUtils;
import org.smartregister.view.activity.DrishtiApplication;
import org.opensrp.api.domain.Location;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import id.zelory.compressor.Compressor;

/**
 * Created by keyman on 08/02/2017.
 */
public class JsonFormUtils extends org.smartregister.util.JsonFormUtils {
    private static final String TAG = "JsonFormUtils";

    public static final String OPENMRS_ENTITY = "openmrs_entity";
    public static final String OPENMRS_ENTITY_ID = "openmrs_entity_id";
    public static final String OPENMRS_ENTITY_PARENT = "openmrs_entity_parent";
    public static final String OPENMRS_CHOICE_IDS = "openmrs_choice_ids";
    public static final String OPENMRS_DATA_TYPE = "openmrs_data_type";
    public static final String MOTHER_DEFAULT_DOB = "01-01-1960";
    public static final String FATHER_DEFAULT_DOB = "01-01-1960";

    private static final String PERSON_ATTRIBUTE = "person_attribute";
    private static final String PERSON_INDENTIFIER = "person_identifier";
    private static final String PERSON_ADDRESS = "person_address";

    private static final String CONCEPT = "concept";
    private static final String ENCOUNTER = "encounter";
    public static final String VALUE = "value";
    private static final String VALUES = "values";
    public static final String FIELDS = "fields";
    public static final String KEY = "key";
    public static final String ENTITY_ID = "entity_id";
    public static final String RELATIONAL_ID = "relational_id";
    private static final String ENCOUNTER_TYPE = "encounter_type";
    public static final String CURRENT_ZEIR_ID = "current_zeir_id";
    public static final String STEP1 = "step1";
    public static final String SECTIONS = "sections";
    public static final String READ_ONLY = "read_only";
    private static final String METADATA = "metadata";
    public static final String KIP_ID = "KIP_ID";
    public static final String M_KIP_ID = "M_KIP_ID";
    public static final String attributes = "attributes";
    public static final String encounterType = "Update Birth Registration";
    private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static final String OPENMRS_ID = "OPENMRS_ID";
    /*
    There are some UUIDs shared across all OpenMRS implementations for metadata that are common across all implementations.
    This @UNIVERSAL_OPENMRS_RELATIONSHIP_TYPE_UUID is the UUID for relationship type "Parent/Child"
     */
    public static final String UNIVERSAL_OPENMRS_RELATIONSHIP_TYPE_UUID = "8d91a210-c2cc-11de-8d13-0010c6dffd0f";

    public static final String ZEIR_ID = "ZEIR_ID";
    private static final String M_ZEIR_ID = "M_ZEIR_ID";


    private static final String LOCATION_HIERARCHY = "locationsHierarchy";
    private static final String MAP = "map";


    public static final SimpleDateFormat dd_MM_yyyy = new SimpleDateFormat("dd-MM-yyyy");
    //public static Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").create();
    //2007-03-31T04:00:00.000Z
    public static final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").registerTypeAdapter(DateTime.class, new DateTimeTypeConverter()).create();


    public static void saveForm(Context context, org.smartregister.path.context.Context openSrpContext,
                                String jsonString, String providerId) {
        try {
            JSONObject form = new JSONObject(jsonString);
            if (form.getString("encounter_type").equals("Out of Catchment Service")) {
                saveOutOfAreaService(context, openSrpContext, jsonString);
            } else if (form.getString("encounter_type").equals("Child Enrollment")) {
                saveChildEnrollment(context, openSrpContext, jsonString, providerId, "Child_Photo", "child");
            }
        } catch (JSONException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    public static void saveAdverseEvent(String jsonString, String locationId, String baseEntityId,
                                        String providerId) {
        org.smartregister.util.Utils.startAsyncTask(
                new SaveAdverseEventTask(jsonString, locationId, baseEntityId, providerId), null);
    }

    private static void saveChildEnrollment(Context context, org.smartregister.path.context.Context openSrpContext,
                                            String jsonString, String providerId, String imageKey, String bindType) {
        if (context == null || openSrpContext == null || StringUtils.isBlank(providerId)
                || StringUtils.isBlank(jsonString)) {
            return;
        }

        try {
            ECSyncUpdater ecUpdater = ECSyncUpdater.getInstance(context);
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            AllSharedPreferences allSharedPreferences = new AllSharedPreferences(preferences);

            JSONObject jsonForm = new JSONObject(jsonString);

            String entityId = getString(jsonForm, ENTITY_ID);
            if (StringUtils.isBlank(entityId)) {
                entityId = generateRandomUUIDString();
            }

            JSONArray fields = fields(jsonForm);
            if (fields == null) {
                return;
            }

            String encounterType = getString(jsonForm, ENCOUNTER_TYPE);

            JSONObject metadata = getJSONObject(jsonForm, METADATA);

            // Replace values for location questions with their corresponding location IDs
            for (int i = 0; i < fields.length(); i++) {
                String key = fields.getJSONObject(i).getString("key");
                if (key.equals("Home_Facility")) {
                    try {
                        String rawValue = fields.getJSONObject(i).getString("value");
                        JSONArray valueArray = new JSONArray(rawValue);
                        if (valueArray.length() > 0) {
                            String lastLocationName = valueArray.getString(valueArray.length() - 1);
                            String lastLocationId = getOpenMrsLocationId(openSrpContext, lastLocationName);
                            fields.getJSONObject(i).put("value", lastLocationId);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, Log.getStackTraceString(e));
                    }
                } else if (key.equals("Mother_Guardian_Date_Birth")) {
                    if (TextUtils.isEmpty(fields.getJSONObject(i).optString("value"))) {
                        fields.getJSONObject(i).put("value", MOTHER_DEFAULT_DOB);
                    }
                } else if (key.equals("Father_Guardian_Date_Birth")) {
                    if (TextUtils.isEmpty(fields.getJSONObject(i).optString("value"))) {
                        fields.getJSONObject(i).put("value", FATHER_DEFAULT_DOB);
                    }
                }
            }

            JSONObject lookUpJSONObject = getJSONObject(metadata, "look_up");
            String lookUpEntityId = "";
            String lookUpBaseEntityId = "";
            if (lookUpJSONObject != null) {
                lookUpEntityId = getString(lookUpJSONObject, "entity_id");
                lookUpBaseEntityId = getString(lookUpJSONObject, "value");
            }

            Client c = JsonFormUtils.createBaseClient(fields, entityId);
            Event e = JsonFormUtils.createEvent(openSrpContext, fields, metadata, entityId, encounterType, providerId, bindType);

            String relationships = AssetHandler.readFileFromAssetsFolder(FormUtils.ecClientRelationships, context);
            JSONArray relationshipsArray = new JSONArray(relationships);

            for (int i = 0; i < relationshipsArray.length(); i++) {
                Client s = null;
                Event se = null;

                JSONObject rObject = relationshipsArray.getJSONObject(i);
                String subBindType = rObject.getString("client_relationship");

                if (lookUpEntityId.equals(subBindType) && StringUtils.isNotBlank(lookUpBaseEntityId)) {
                    s = new Client(lookUpBaseEntityId);
                    addRelationship(s, c, subBindType, getRelationshipTypeId(openSrpContext, fields, bindType));
                } else {

                    if (StringUtils.isNotBlank(subBindType)) {
                        s = JsonFormUtils.createSubformClient(context, openSrpContext, fields, c, subBindType, null);
                    }

                    if (s != null && e != null) {
                        JSONObject subBindTypeJson = getJSONObject(jsonForm, subBindType);
                        if (subBindTypeJson != null) {
                            String subBindTypeEncounter = getString(subBindTypeJson, ENCOUNTER_TYPE);
                            if (StringUtils.isNotBlank(subBindTypeEncounter)) {
                                se = JsonFormUtils.createSubFormEvent(null, metadata, e, s.getBaseEntityId(), subBindTypeEncounter, providerId, subBindType);
                            }
                        }
                    }

                    if (s != null) {
                        JSONObject clientJson = new JSONObject(gson.toJson(s));

                        ecUpdater.addClient(s.getBaseEntityId(), clientJson);

                    }

                    if (se != null) {
                        JSONObject eventJson = new JSONObject(gson.toJson(se));
                        ecUpdater.addEvent(se.getBaseEntityId(), eventJson);
                    }
                }
            }

            if (c != null) {
                JSONObject clientJson = new JSONObject(gson.toJson(c));

                ecUpdater.addClient(c.getBaseEntityId(), clientJson);

            }

            if (e != null) {
                JSONObject eventJson = new JSONObject(gson.toJson(e));
                ecUpdater.addEvent(e.getBaseEntityId(), eventJson);
            }

            UniqueIdRepository uniqueIdRepo = VaccinatorApplication.getInstance().uniqueIdRepository();
            uniqueIdRepo.close(c.getIdentifier(OPENMRS_ID));

            String imageLocation = getFieldValue(fields, imageKey);
            saveImage(context, providerId, entityId, imageLocation);

            long lastSyncTimeStamp = allSharedPreferences.fetchLastUpdatedAtDate(0);
            Date lastSyncDate = new Date(lastSyncTimeStamp);
            PathClientProcessor.getInstance(context).processClient(ecUpdater.getEvents(lastSyncDate, BaseRepository.TYPE_Unsynced));
            allSharedPreferences.saveLastUpdatedAtDate(lastSyncDate.getTime());
        } catch (Exception e) {
            Log.e(TAG, "", e);
        }
    }

    public static void editsave(Context context, org.smartregister.path.context.Context openSrpContext, String jsonString, String providerId, String imageKey, String bindType, String subBindType) {
        if (context == null || StringUtils.isBlank(providerId) || StringUtils.isBlank(jsonString)) {
            return;
        }

        try {
            ECSyncUpdater ecUpdater = ECSyncUpdater.getInstance(context);

            JSONObject jsonForm = new JSONObject(jsonString);

            String entityId = getString(jsonForm, ENTITY_ID);
            String relationalId = getString(jsonForm, RELATIONAL_ID);

            if (StringUtils.isBlank(entityId)) {
                entityId = generateRandomUUIDString();
            }

            JSONArray fields = fields(jsonForm);
            if (fields == null) {
                return;
            }

            for (int i = 0; i < fields.length(); i++) {
                String key = fields.getJSONObject(i).getString("key");
                if (key.equals("Home_Facility")) {
                    try {
                        String rawValue = fields.getJSONObject(i).getString("value");
                        JSONArray valueArray = new JSONArray(rawValue);
                        if (valueArray.length() > 0) {
                            String lastLocationName = valueArray.getString(valueArray.length() - 1);
                            String lastLocationId = getOpenMrsLocationId(openSrpContext, lastLocationName);
                            fields.getJSONObject(i).put("value", lastLocationId);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, Log.getStackTraceString(e));
                    }
                } else if (key.equals("Mother_Guardian_Date_Birth")) {
                    if (TextUtils.isEmpty(fields.getJSONObject(i).optString("value"))) {
                        fields.getJSONObject(i).put("value", MOTHER_DEFAULT_DOB);
                    }
                } else if (key.equals("Father_Guardian_Date_Birth")) {
                    if (TextUtils.isEmpty(fields.getJSONObject(i).optString("value"))) {
                        fields.getJSONObject(i).put("value", FATHER_DEFAULT_DOB);
                    }
                }
            }

            JSONObject metadata = getJSONObject(jsonForm, METADATA);

            Client baseClient = JsonFormUtils.createBaseClient(fields, entityId);
            Event e = JsonFormUtils.createEvent(openSrpContext, fields, metadata, entityId, encounterType, providerId, bindType);

            JSONObject lookUpJSONObject = getJSONObject(metadata, "look_up");
            String lookUpEntityId = "";
            String lookUpBaseEntityId = "";
            if (lookUpJSONObject != null) {
                lookUpEntityId = getString(lookUpJSONObject, "entity_id");
                lookUpBaseEntityId = getString(lookUpJSONObject, "value");
            }

            String relationships = AssetHandler.readFileFromAssetsFolder(FormUtils.ecClientRelationships, context);
            JSONArray relationshipsArray = new JSONArray(relationships);

            for (int i = 0; i < relationshipsArray.length(); i++) {
                Client s = null;
                Event se = null;

                JSONObject rObject = relationshipsArray.getJSONObject(i);
                String _subBindType = rObject.getString("client_relationship");

                if (lookUpEntityId.equals(_subBindType) && StringUtils.isNotBlank(lookUpBaseEntityId)) {
                    s = new Client(lookUpBaseEntityId);
                    addRelationship(s, baseClient, _subBindType, getRelationshipTypeId(openSrpContext, fields, bindType));
                } else {

                    if (StringUtils.isNotBlank(_subBindType)) {
                        s = JsonFormUtils.createSubformClient(context, openSrpContext, fields, baseClient, _subBindType, relationalId);
                    }

                    if (s != null && e != null) {
                        JSONObject subBindTypeJson = getJSONObject(jsonForm, _subBindType);
                        if (subBindTypeJson != null) {
                            String subBindTypeEncounter = getString(subBindTypeJson, ENCOUNTER_TYPE);
                            if (StringUtils.isNotBlank(subBindTypeEncounter)) {
                                se = JsonFormUtils.createSubFormEvent(null, metadata, e, s.getBaseEntityId(), subBindTypeEncounter, providerId, _subBindType);
                            }
                        }
                    }

                    if (s != null) {
                        mergeAndSaveClient(context, s);
                    }

                    if (se != null) {
                        JSONObject eventJson = new JSONObject(gson.toJson(se));
                        ecUpdater.addEvent(se.getBaseEntityId(), eventJson);
                    }
                }
            }

            if (baseClient != null) {
                mergeAndSaveClient(context, baseClient);
            }
            if (e != null) {
                JSONObject eventJson = new JSONObject(gson.toJson(e));
                ecUpdater.addEvent(e.getBaseEntityId(), eventJson);
            }

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            AllSharedPreferences allSharedPreferences = new AllSharedPreferences(preferences);
            long lastSyncTimeStamp = allSharedPreferences.fetchLastUpdatedAtDate(0);
            Date lastSyncDate = new Date(lastSyncTimeStamp);
            PathClientProcessor.getInstance(context).processClient(ecUpdater.getEvents(lastSyncDate, BaseRepository.TYPE_Unsynced));
            allSharedPreferences.saveLastUpdatedAtDate(lastSyncDate.getTime());

            String imageLocation = getFieldValue(fields, imageKey);
            if (!TextUtils.isEmpty(imageLocation)) {
                saveImage(context, providerId, entityId, imageLocation);
            }

            // Unassign current id
            if (baseClient != null) {
                String newZeirId = baseClient.getIdentifier(KIP_ID).replace("-", "");
                String currentZeirId = getString(jsonForm, "current_zeir_id").replace("-", "");
                if (!newZeirId.equals(currentZeirId)) {
                    //ZEIR_ID was changed
                    VaccinatorApplication.getInstance().uniqueIdRepository().open(currentZeirId);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "", e);
        }
    }

    public static void mergeAndSaveClient(Context context, Client baseClient) throws Exception {
        ECSyncUpdater ecUpdater = ECSyncUpdater.getInstance(context);

        JSONObject updatedClientJson = new JSONObject(gson.toJson(baseClient));

        JSONObject originalClientJsonObject = ecUpdater.getClient(baseClient.getBaseEntityId());

        JSONObject mergedJson = merge(originalClientJsonObject, updatedClientJson);

        //TODO Save edit log

        //save the updated client (the one updated and generated from the form) as EditClient to keep an edit log of the client doc
        // originalClient.setType("PristineClient");
        //originalClient.setRev(null);
        //cloudantDataHandler.addClient(originalClient);

        ecUpdater.addClient(baseClient.getBaseEntityId(), mergedJson);


    }

    private static void saveOutOfAreaService(Context context, org.smartregister.Context openSrpContext,
                                             String jsonString) {
        SaveOutOfAreaServiceTask saveOutOfAreaServiceTask = new SaveOutOfAreaServiceTask(context,
                openSrpContext, jsonString);

        org.smartregister.util.Utils.startAsyncTask(saveOutOfAreaServiceTask, null);
    }

    /**
     * Constructs a weight object using the out of service area form
     *
     * @param openSrpContext The context to work with
     * @param outOfAreaForm  Out of area form to extract the weight form
     * @return A weight object if weight recorded in form, or {@code null} if weight not recorded
     * @throws Exception
     */
    private static Weight getWeightObject(org.smartregister.Context openSrpContext, JSONObject outOfAreaForm) throws Exception {
        Weight weight = null;
        JSONArray fields = outOfAreaForm.getJSONObject("step1").getJSONArray("fields");
        String serviceDate = null;
        String zeirId = null;

        int foundFields = 0;
        for (int i = 0; i < fields.length(); i++) {
            JSONObject curField = fields.getJSONObject(i);
            if (curField.getString("key").equals("Weight_Kg")) {
                foundFields++;
                if (StringUtils.isNotEmpty(curField.getString("value"))) {
                    weight = new Weight();
                    weight.setBaseEntityId("");
                    weight.setKg(Float.parseFloat(curField.getString("value")));
                    weight.setAnmId(openSrpContext.allSharedPreferences().fetchRegisteredANM());
                    weight.setLocationId(outOfAreaForm.getJSONObject("metadata")
                            .getString("encounter_location"));
                    weight.setUpdatedAt(null);
                }
            } else if (curField.getString("key").equals("OA_Service_Date")) {
                foundFields++;
                serviceDate = curField.getString("value");
            } else if (curField.getString("key").equals("KIP_ID")) {
                foundFields++;
                zeirId = formatChildUniqueId(curField.getString("value"));
            }

            if (foundFields == 3) {
                break;
            }
        }

        if (weight != null && serviceDate != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
            weight.setDate(dateFormat.parse(serviceDate));
        }

        if (weight != null && zeirId != null) {
            weight.setProgramClientId(zeirId);
        }

        return weight;
    }

    /**
     * Constructs a list of recorded vaccines from the out of area form provided
     *
     * @param openSrpContext The context to use
     * @param outOfAreaForm  Out of area form to extract recorded vaccines from
     * @return A list of recorded vaccines
     */
    private static ArrayList<Vaccine> getVaccineObjects(Context context,
                                                        org.smartregister.Context openSrpContext,
                                                        JSONObject outOfAreaForm) throws Exception {
        ArrayList<Vaccine> vaccines = new ArrayList<>();
        JSONArray fields = outOfAreaForm.getJSONObject("step1").getJSONArray("fields");
        String serviceDate = null;
        String zeirId = null;

        for (int i = 0; i < fields.length(); i++) {
            JSONObject curField = fields.getJSONObject(i);
            if (curField.has("is_vaccine_group")
                    && curField.getBoolean("is_vaccine_group")
                    && curField.getString("type").equals("check_box")) {
                JSONArray options = curField.getJSONArray("options");
                for (int j = 0; j < options.length(); j++) {
                    JSONObject curOption = options.getJSONObject(j);
                    if (curOption.getString("value").equalsIgnoreCase(Boolean.TRUE.toString())) {
                        Vaccine curVaccine = new Vaccine();
                        curVaccine.setBaseEntityId("");
                        curVaccine.setName(curOption.getString("key"));
                        curVaccine.setAnmId(openSrpContext.allSharedPreferences().fetchRegisteredANM());
                        curVaccine.setLocationId(outOfAreaForm.getJSONObject("metadata")
                                .getString("encounter_location"));
                        curVaccine.setCalculation(VaccinatorUtils.getVaccineCalculation(context,
                                curVaccine.getName()));
                        curVaccine.setUpdatedAt(null);

                        vaccines.add(curVaccine);
                    }
                }
            } else if (curField.getString("key").equals("OA_Service_Date")) {
                serviceDate = curField.getString("value");
            } else if (curField.getString("key").equals("KIP_ID")) {
                zeirId = formatChildUniqueId(curField.getString("value"));
            }
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        for (Vaccine curVaccine : vaccines) {
            if (serviceDate != null) {
                curVaccine.setDate(dateFormat.parse(serviceDate));
            }

            if (zeirId != null) {
                curVaccine.setProgramClientId(zeirId);
            }
        }

        return vaccines;
    }

    public static void saveImage(Context context, String providerId, String entityId, String imageLocation) {
        if (StringUtils.isBlank(imageLocation)) {
            return;
        }


        File file = new File(imageLocation);

        if (!file.exists()) {
            return;
        }

        Bitmap compressedImageFile = Compressor.getDefault(context).compressToBitmap(file);
        saveStaticImageToDisk(compressedImageFile, providerId, entityId);

    }

    private static void saveStaticImageToDisk(Bitmap image, String providerId, String entityId) {
        if (image == null || StringUtils.isBlank(providerId) || StringUtils.isBlank(entityId)) {
            return;
        }
        OutputStream os = null;
        try {

            if (entityId != null && !entityId.isEmpty()) {
                final String absoluteFileName = DrishtiApplication.getAppDir() + File.separator + entityId + ".JPEG";

                File outputFile = new File(absoluteFileName);
                os = new FileOutputStream(outputFile);
                Bitmap.CompressFormat compressFormat = Bitmap.CompressFormat.JPEG;
                if (compressFormat != null) {
                    image.compress(compressFormat, 100, os);
                } else {
                    throw new IllegalArgumentException("Failed to save static image, could not retrieve image compression format from name "
                            + absoluteFileName);
                }
                // insert into the db
                ProfileImage profileImage = new ProfileImage();
                profileImage.setImageid(UUID.randomUUID().toString());
                profileImage.setAnmId(providerId);
                profileImage.setEntityID(entityId);
                profileImage.setFilepath(absoluteFileName);
                profileImage.setFilecategory("profilepic");
                profileImage.setSyncStatus(ImageRepository.TYPE_Unsynced);
                ImageRepository imageRepo = VaccinatorApplication.getInstance().context().imageRepository();
                imageRepo.add(profileImage);
            }

        } catch (FileNotFoundException e) {
            Log.e(TAG, "Failed to save static image to disk");
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    Log.e(TAG, "Failed to close static images output stream after attempting to write image");
                }
            }
        }

    }

    private static Client createBaseClient(JSONArray fields, String entityId) {

        String firstName = getFieldValue(fields, FormEntityConstants.Person.first_name);
        String middleName = getFieldValue(fields, FormEntityConstants.Person.middle_name);
        String lastName = getFieldValue(fields, FormEntityConstants.Person.last_name);
        String bd = getFieldValue(fields, FormEntityConstants.Person.birthdate);
        Date birthdate = formatDate(bd, true);
        String dd = getFieldValue(fields, FormEntityConstants.Person.deathdate);
        Date deathdate = formatDate(dd, true);
        String aproxbd = getFieldValue(fields, FormEntityConstants.Person.birthdate_estimated);
        Boolean birthdateApprox = false;
        if (!StringUtils.isEmpty(aproxbd) && NumberUtils.isNumber(aproxbd)) {
            int bde = 0;
            try {
                bde = Integer.parseInt(aproxbd);
            } catch (Exception e) {
                Log.e(TAG, e.toString(), e);
            }
            birthdateApprox = bde > 0;
        }
        String aproxdd = getFieldValue(fields, FormEntityConstants.Person.deathdate_estimated);
        Boolean deathdateApprox = false;
        if (!StringUtils.isEmpty(aproxdd) && NumberUtils.isNumber(aproxdd)) {
            int dde = 0;
            try {
                dde = Integer.parseInt(aproxdd);
            } catch (Exception e) {
                Log.e(TAG, e.toString(), e);
            }
            deathdateApprox = dde > 0;
        }
        String gender = getFieldValue(fields, FormEntityConstants.Person.gender);

        List<Address> addresses = new ArrayList<>(extractAddresses(fields).values());

        Client c = (Client) new Client(entityId)
                .withFirstName(firstName)
                .withMiddleName(middleName)
                .withLastName(lastName)
                .withBirthdate((birthdate != null ? birthdate : null), birthdateApprox)
                .withDeathdate(deathdate != null ? deathdate : null, deathdateApprox)
                .withGender(gender).withDateCreated(new Date());

        c.withAddresses(addresses)
                .withAttributes(extractAttributes(fields))
                .withIdentifiers(extractIdentifiers(fields));
        return c;

    }

    private static Event createEvent(org.smartregister.Context openSrpContext,
                                     JSONArray fields, JSONObject metadata, String entityId,
                                     String encounterType, String providerId, String bindType) {

        String encounterDateField = getFieldValue(fields, FormEntityConstants.Encounter.encounter_date);
        String encounterLocation = null;

        Date encounterDate = new Date();
        if (StringUtils.isNotBlank(encounterDateField)) {
            Date dateTime = formatDate(encounterDateField, false);
            if (dateTime != null) {
                encounterDate = dateTime;
            }
        }
        try {
            encounterLocation = metadata.getString("encounter_location");
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
        }

        Event e = (Event) new Event()
                .withBaseEntityId(entityId)//should be different for main and subform
                .withEventDate(encounterDate)
                .withEventType(encounterType)
                .withLocationId(encounterLocation)
                .withProviderId(providerId)
                .withEntityType(bindType)
                .withFormSubmissionId(generateRandomUUIDString())
                .withDateCreated(new Date());

        for (int i = 0; i < fields.length(); i++) {
            JSONObject jsonObject = getJSONObject(fields, i);
            String value = getString(jsonObject, VALUE);
            if (StringUtils.isNotBlank(value)) {
                addObservation(e, jsonObject);
            }
        }

        if (metadata != null) {
            Iterator<?> keys = metadata.keys();

            while (keys.hasNext()) {
                String key = (String) keys.next();
                JSONObject jsonObject = getJSONObject(metadata, key);
                String value = getString(jsonObject, VALUE);
                if (StringUtils.isNotBlank(value)) {
                    String entityVal = getString(jsonObject, OPENMRS_ENTITY);
                    if (entityVal != null) {
                        if (entityVal.equals(CONCEPT)) {
                            addToJSONObject(jsonObject, KEY, key);
                            addObservation(e, jsonObject);
                        } else if (entityVal.equals(ENCOUNTER)) {
                            String entityIdVal = getString(jsonObject, OPENMRS_ENTITY_ID);
                            if (entityIdVal.equals(FormEntityConstants.Encounter.encounter_date.name())) {
                                Date eDate = formatDate(value, false);
                                if (eDate != null) {
                                    e.setEventDate(eDate);
                                }
                            }
                        }
                    }
                }
            }
        }

        return e;

    }

    public static void fillIdentifiers(Map<String, String> pids, JSONObject jsonObject) {

        String value = getString(jsonObject, VALUE);
        if (StringUtils.isBlank(value)) {
            return;
        }

        if (StringUtils.isNotBlank(getString(jsonObject, ENTITY_ID))) {
            return;
        }

        String entity = PERSON_INDENTIFIER;
        String entityVal = getString(jsonObject, OPENMRS_ENTITY);

        if (entityVal != null && entityVal.equals(entity)) {
            String entityIdVal = getString(jsonObject, OPENMRS_ENTITY_ID);

            if (entityIdVal.equals(KIP_ID)) {
                value = formatChildUniqueId(value);
            }

            pids.put(entityIdVal, value);
        }


    }

    /**
     * This method formats the child unique id obtained from a JSON Form to something that is useable
     *
     * @param unformattedId The unformatted unique identifier
     * @return A formatted ID or the original id if method is unable to format
     */
    private static String formatChildUniqueId(String unformattedId) {
        if (StringUtils.isNotBlank(unformattedId) && !unformattedId.contains("-")) {
            StringBuilder stringBuilder = new StringBuilder(unformattedId);
            stringBuilder.insert(unformattedId.length() - 1, '-');
            unformattedId = stringBuilder.toString();
        }

        return unformattedId;
    }

    // Helper functions

    public static JSONArray fields(JSONObject jsonForm) {
        try {

            JSONObject step1 = jsonForm.has(STEP1) ? jsonForm.getJSONObject(STEP1) : null;
            if (step1 == null) {
                return null;
            }

            return step1.has(FIELDS) ? step1.getJSONArray(FIELDS) : null;

        } catch (JSONException e) {
            Log.e(TAG, "", e);
        }
        return null;
    }

    /**
     * return field values that are in sections e.g for the hia2 monthly draft form which has sections
     *
     * @param jsonForm
     * @return
     */
    public static Map<String, String> sectionFields(JSONObject jsonForm) {
        try {

            JSONObject step1 = jsonForm.has(STEP1) ? jsonForm.getJSONObject(STEP1) : null;
            if (step1 == null) {
                return null;
            }

            JSONArray sections = step1.has(SECTIONS) ? step1.getJSONArray(SECTIONS) : null;
            if (sections == null) {
                return null;
            }

            Map<String, String> result = new HashMap<>();
            for (int i = 0; i < sections.length(); i++) {
                JSONObject sectionsJSONObject = sections.getJSONObject(i);
                if (sectionsJSONObject.has(FIELDS)) {
                    JSONArray fieldsArray = sectionsJSONObject.getJSONArray(FIELDS);
                    for (int j = 0; j < fieldsArray.length(); j++) {
                        JSONObject fieldJsonObject = fieldsArray.getJSONObject(j);
                        String key = fieldJsonObject.getString(KEY);
                        String value = fieldJsonObject.getString(VALUE);
                        result.put(key, value);

                    }
                }

            }
            return result;

        } catch (JSONException e) {
            Log.e(TAG, "", e);
            return null;
        }

    }

    private static String getFieldValue(JSONArray jsonArray, FormEntityConstants.Person person) {
        if (jsonArray == null || jsonArray.length() == 0) {
            return null;
        }

        if (person == null) {
            return null;
        }

        return value(jsonArray, person.entity(), person.entityId());
    }

    private static String getFieldValue(JSONArray jsonArray, FormEntityConstants.Encounter encounter) {
        if (jsonArray == null || jsonArray.length() == 0) {
            return null;
        }

        if (encounter == null) {
            return null;
        }

        return value(jsonArray, encounter.entity(), encounter.entityId());
    }

    public static String getFieldValue(JSONArray jsonArray, String key) {
        if (jsonArray == null || jsonArray.length() == 0) {
            return null;
        }

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = getJSONObject(jsonArray, i);
            String keyVal = getString(jsonObject, KEY);
            if (keyVal != null && keyVal.equals(key)) {
                return getString(jsonObject, VALUE);
            }
        }
        return null;
    }

    public static JSONArray getJSONArray(JSONObject jsonObject, String field) {
        if (jsonObject == null || jsonObject.length() == 0) {
            return null;
        }

        try {
            return jsonObject.getJSONArray(field);
        } catch (JSONException e) {
            return null;

        }
    }

    public static JSONObject getJSONObject(JSONObject jsonObject, String field) {
        if (jsonObject == null || jsonObject.length() == 0) {
            return null;
        }

        try {
            return jsonObject.getJSONObject(field);
        } catch (JSONException e) {
            return null;

        }
    }

    public static String getString(JSONObject jsonObject, String field) {
        if (jsonObject == null) {
            return null;
        }

        try {
            return jsonObject.has(field) ? jsonObject.getString(field) : null;
        } catch (JSONException e) {
            return null;

        }
    }

    public static Long getLong(JSONObject jsonObject, String field) {
        if (jsonObject == null) {
            return null;
        }

        try {
            return jsonObject.has(field) ? jsonObject.getLong(field) : null;
        } catch (JSONException e) {
            return null;

        }
    }

    public static String generateRandomUUIDString() {
        return UUID.randomUUID().toString();
    }

    public static void addToJSONObject(JSONObject jsonObject, String key, String value) {
        try {
            if (jsonObject == null) {
                return;
            }

            jsonObject.put(key, value);
        } catch (JSONException e) {
            Log.e(TAG, "", e);
        }
    }

    public static Client createSubformClient(Context context, org.smartregister.path.context.Context openSrpContext, JSONArray fields, Client parent, String bindType, String relationalId) throws ParseException {

        if (StringUtils.isBlank(bindType)) {
            return null;
        }

        String entityId = relationalId == null ? generateRandomUUIDString() : relationalId;
        String firstName = getSubFormFieldValue(fields, FormEntityConstants.Person.first_name, bindType);
        String gender = getSubFormFieldValue(fields, FormEntityConstants.Person.gender, bindType);
        String bb = getSubFormFieldValue(fields, FormEntityConstants.Person.birthdate, bindType);

        Map<String, String> idents = extractIdentifiers(fields, bindType);
        String parentIdentifier = parent.getIdentifier(OPENMRS_ID);
        if (StringUtils.isNotBlank(parentIdentifier)) {
            String identifier = parentIdentifier.concat("_").concat(bindType);
            idents.put(M_KIP_ID, identifier);
        }

        String middleName = getSubFormFieldValue(fields, FormEntityConstants.Person.middle_name, bindType);
        String lastName = getSubFormFieldValue(fields, FormEntityConstants.Person.last_name, bindType);
        // Get full name of father/guardian and set first and last names
        String fullName = getSubFormFieldValue(fields, FormEntityConstants.Person.full_name, bindType);
        if (StringUtils.isNotBlank(fullName) && StringUtils.isBlank(firstName)
                && StringUtils.isBlank(lastName) && StringUtils.isBlank(middleName)) {
            String[] tokens = fullName.split("\\s+");
            firstName = StringUtils.isBlank(tokens[0]) ? firstName : tokens[0];
            lastName = StringUtils.isBlank(tokens[1]) ? lastName : tokens[1];
        }

        if (StringUtils.isBlank(fullName) && StringUtils.isBlank(firstName)
                && StringUtils.isBlank(lastName) && StringUtils.isBlank(middleName))
            return null;

        Date birthdate = formatDate(bb, true);
        String dd = getSubFormFieldValue(fields, FormEntityConstants.Person.deathdate, bindType);
        Date deathdate = formatDate(dd, true);
        String aproxbd = getSubFormFieldValue(fields, FormEntityConstants.Person.birthdate_estimated, bindType);
        Boolean birthdateApprox = false;
        if (!StringUtils.isEmpty(aproxbd) && NumberUtils.isNumber(aproxbd)) {
            int bde = 0;
            try {
                bde = Integer.parseInt(aproxbd);
            } catch (Exception e) {
                Log.e(TAG, e.toString(), e);
            }
            birthdateApprox = bde > 0 ? true : false;
        }
        String aproxdd = getSubFormFieldValue(fields, FormEntityConstants.Person.deathdate_estimated, bindType);
        Boolean deathdateApprox = false;
        if (!StringUtils.isEmpty(aproxdd) && NumberUtils.isNumber(aproxdd)) {
            int dde = 0;
            try {
                dde = Integer.parseInt(aproxdd);
            } catch (Exception e) {
                Log.e(TAG, e.toString(), e);
            }
            deathdateApprox = dde > 0 ? true : false;
        }

        List<Address> addresses = new ArrayList<>(extractAddresses(fields, bindType).values());

        Client c = (Client) new Client(entityId)
                .withFirstName(firstName)
                .withMiddleName(middleName)
                .withLastName(lastName)
                .withBirthdate(birthdate, birthdateApprox)
                .withDeathdate(deathdate, deathdateApprox)
                .withGender(gender).withDateCreated(new Date());

        c.withAddresses(addresses)
                .withAttributes(extractAttributes(fields, bindType))
                .withIdentifiers(idents);

        if (addresses.isEmpty()) {
            c.withAddresses(parent.getAddresses());
        }


        String relationshipTypeId = getRelationshipTypeId(openSrpContext, fields, bindType);

        addRelationship(c, parent, bindType, relationshipTypeId);

        return c;
    }

    private static String getRelationshipTypeId(org.smartregister.path.context.Context openSrpContext, JSONArray fields, String bindType) {
        String relationshipType = getSubFormFieldValue(fields, FormEntityConstants.Relationship.relationship, bindType);

        if (StringUtils.isBlank(relationshipType))
            return JsonFormUtils.UNIVERSAL_OPENMRS_RELATIONSHIP_TYPE_UUID;

        JSONArray relationshipTypes;
        JSONObject rType;
        String relationshipTypeId = "";
        try {
            relationshipTypes = new JSONObject(openSrpContext.allSettings().fetchRelationshipTypes()).getJSONArray("relationshipTypes");

            if (relationshipTypes != null && relationshipTypes.length() > 0) {
                for (int n = 0; n < relationshipTypes.length(); n++) {
                    rType = new JSONObject(relationshipTypes.getString(n));
                    if (rType.has("name") && rType.getString("name").equals(relationshipType)) {
                        relationshipTypeId = rType.getString("key");
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (StringUtils.isBlank(relationshipTypeId)) {
            //return the universal for parent/child
            relationshipTypeId = JsonFormUtils.UNIVERSAL_OPENMRS_RELATIONSHIP_TYPE_UUID;
        }

        return relationshipTypeId;
    }

    public static String getRelationshipType(org.smartregister.path.context.Context openSrpContext, String relationshipTypeId) {
        JSONArray relationshipTypes;
        JSONObject rType;
        String relationshipType = "";
        try {
            relationshipTypes = new JSONObject(openSrpContext.allSettings().fetchRelationshipTypes()).getJSONArray("relationshipTypes");

            if (relationshipTypes != null && relationshipTypes.length() > 0) {
                for (int n = 0; n < relationshipTypes.length(); n++) {
                    rType = new JSONObject(relationshipTypes.getString(n));
                    if (rType.has("key") && rType.getString("key").equals(relationshipTypeId)) {
                        relationshipType = rType.has("name") ? rType.getString("name") : "";
                        break;
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return relationshipType;
    }

    public static JSONObject merge(JSONObject original, JSONObject updated) {
        JSONObject mergedJSON = new JSONObject();
        try {
            mergedJSON = new JSONObject(original, getNames(original));
            for (String key : getNames(updated)) {
                mergedJSON.put(key, updated.get(key));
            }

        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
        }
        return mergedJSON;
    }

    public static Event createSubFormEvent(JSONArray fields, JSONObject metadata, Event parent, String entityId, String encounterType, String providerId, String bindType) {


        Event e = (Event) new Event()
                .withBaseEntityId(entityId)//should be different for main and subform
                .withEventDate(parent.getEventDate())
                .withEventType(encounterType)
                .withLocationId(parent.getLocationId())
                .withProviderId(providerId)
                .withEntityType(bindType)
                .withFormSubmissionId(generateRandomUUIDString())
                .withDateCreated(new Date());

        if (fields != null && fields.length() != 0)
            for (int i = 0; i < fields.length(); i++) {
                JSONObject jsonObject = getJSONObject(fields, i);
                String value = getString(jsonObject, VALUE);
                if (StringUtils.isNotBlank(value)) {
                    addObservation(e, jsonObject);
                }
            }

        if (metadata != null) {
            Iterator<?> keys = metadata.keys();

            while (keys.hasNext()) {
                String key = (String) keys.next();
                JSONObject jsonObject = getJSONObject(metadata, key);
                String value = getString(jsonObject, VALUE);
                if (StringUtils.isNotBlank(value)) {
                    String entityVal = getString(jsonObject, OPENMRS_ENTITY);
                    if (entityVal != null) {
                        if (entityVal.equals(CONCEPT)) {
                            addToJSONObject(jsonObject, KEY, key);
                            addObservation(e, jsonObject);
                        } else if (entityVal.equals(ENCOUNTER)) {
                            String entityIdVal = getString(jsonObject, OPENMRS_ENTITY_ID);
                            if (entityIdVal.equals(FormEntityConstants.Encounter.encounter_date.name())) {
                                Date eDate = formatDate(value, false);
                                if (eDate != null) {
                                    e.setEventDate(eDate);
                                }
                            }
                        }
                    }
                }
            }
        }

        return e;

    }


    public static Map<String, String> extractIdentifiers(JSONArray fields) {
        Map<String, String> pids = new HashMap<>();
        for (int i = 0; i < fields.length(); i++) {
            JSONObject jsonObject = getJSONObject(fields, i);
            fillIdentifiers(pids, jsonObject);
        }
        return pids;
    }


    public static Map<String, String> extractIdentifiers(JSONArray fields, String bindType) {
        Map<String, String> pids = new HashMap<>();
        for (int i = 0; i < fields.length(); i++) {
            JSONObject jsonObject = getJSONObject(fields, i);
            fillSubFormIdentifiers(pids, jsonObject, bindType);
        }
        return pids;
    }


    private static void addRelationship(Client parent, Client child, String
            bindType, String relationshipType) {
        child.addRelationship(bindType, parent.getBaseEntityId(), relationshipType);
    }

    private static String getSubFormFieldValue(JSONArray jsonArray, FormEntityConstants.Person person, String bindType) {
        if (jsonArray == null || jsonArray.length() == 0) {
            return null;
        }

        if (person == null) {
            return null;
        }

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = getJSONObject(jsonArray, i);
            String bind = getString(jsonObject, ENTITY_ID);
            if (bind == null || !bind.equals(bindType)) {
                continue;
            }
            String entityVal = getString(jsonObject, OPENMRS_ENTITY);
            String entityIdVal = getString(jsonObject, OPENMRS_ENTITY_ID);
            if (entityVal != null && entityVal.equals(person.entity()) && entityIdVal != null && entityIdVal.equals(person.name())) {
                return getString(jsonObject, VALUE);
            }

        }
        return null;
    }

    private static String getSubFormFieldValue(JSONArray
                                                       jsonArray, FormEntityConstants.Relationship relationship, String bindType) {
        if (jsonArray == null || jsonArray.length() == 0) {
            return null;
        }

        if (relationship == null) {
            return null;
        }

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = getJSONObject(jsonArray, i);
            String bind = getString(jsonObject, ENTITY_ID);
            if (bind == null || !bind.equals(bindType)) {
                continue;
            }
            String entityVal = getString(jsonObject, OPENMRS_ENTITY);
            String entityIdVal = getString(jsonObject, OPENMRS_ENTITY_ID);
            if (entityVal != null && entityVal.equals(relationship.entity()) && entityIdVal != null && entityIdVal.equals(relationship.name())) {
                return getString(jsonObject, VALUE);
            }

        }
        return null;
    }

    public static JSONArray generateLocationHierarchyTree(org.smartregister.Context context,
                                                          boolean withOtherOption, ArrayList<String> allowedLevels) {
        JSONArray array = new JSONArray();
        try {
            JSONObject locationData = new JSONObject(context.anmLocationController().get());
            if (locationData.has("locationsHierarchy")
                    && locationData.getJSONObject("locationsHierarchy").has("map")) {
                JSONObject map = locationData.getJSONObject("locationsHierarchy").getJSONObject("map");
                Iterator<String> keys = map.keys();
                while (keys.hasNext()) {
                    String curKey = keys.next();
                    getFormJsonData(array, map.getJSONObject(curKey), allowedLevels);
                }
            }

            array = sortTreeViewQuestionOptions(array);
        } catch (JSONException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }

        if (withOtherOption) {
            try {
                JSONObject other = new JSONObject();
                other.put("name", "Other");
                other.put("key", "Other");
                other.put("level", "");
                array.put(other);
            } catch (JSONException e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
        }
        return array;
    }

    public static JSONArray generateDefaultLocationHierarchy(org.smartregister.Context
                                                                     context, ArrayList<String> allowedLevels) {
        try {
            String defaultLocationUuid = context.allSharedPreferences()
                    .fetchDefaultLocalityId(context.allSharedPreferences().fetchRegisteredANM());
            JSONObject locationData = new JSONObject(context.anmLocationController().get());
            if (locationData.has("locationsHierarchy")
                    && locationData.getJSONObject("locationsHierarchy").has("map")) {
                JSONObject map = locationData.getJSONObject("locationsHierarchy").getJSONObject("map");
                Iterator<String> keys = map.keys();
                while (keys.hasNext()) {
                    String curKey = keys.next();
                    JSONArray curResult = getDefaultLocationHierarchy(defaultLocationUuid, map.getJSONObject(curKey), new JSONArray(), allowedLevels);
                    if (curResult != null) {
                        return curResult;
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
        return null;
    }

    private static JSONArray getDefaultLocationHierarchy(String
                                                                 defaultLocationUuid, JSONObject openMrsLocationData, JSONArray
                                                                 parents, ArrayList<String> allowedLevels) throws JSONException {
        if (allowedLevels.contains(openMrsLocationData.getJSONObject("node").getJSONArray("tags").getString(0))) {
            parents.put(openMrsLocationData.getJSONObject("node").getString("name"));
        }

        if (openMrsLocationData.getJSONObject("node").getString("locationId").equals(defaultLocationUuid)) {
            return parents;
        }

        if (openMrsLocationData.has("children")) {
            Iterator<String> childIterator = openMrsLocationData.getJSONObject("children").keys();
            while (childIterator.hasNext()) {
                String curChildKey = childIterator.next();
                JSONArray curResult = getDefaultLocationHierarchy(defaultLocationUuid, openMrsLocationData.getJSONObject("children").getJSONObject(curChildKey), new JSONArray(parents.toString()), allowedLevels);
                if (curResult != null) return curResult;
            }
        }

        return null;
    }

    private static void getFormJsonData(JSONArray allLocationData, JSONObject
            openMrsLocationData, ArrayList<String> allowedLevels) throws JSONException {
        JSONObject jsonFormObject = new JSONObject();
        String name = openMrsLocationData.getJSONObject("node").getString("name");
        jsonFormObject.put("name", getOpenMrsReadableName(name));
        jsonFormObject.put("key", name);
        String level = "";
        try {
            level = openMrsLocationData.getJSONObject("node").getJSONArray("tags").getString(0);
        } catch (JSONException e) {
            Log.e(JsonFormUtils.class.getCanonicalName(), e.getMessage());
        }
        jsonFormObject.put("level", "");
        JSONArray children = new JSONArray();
        if (openMrsLocationData.has("children")) {
            Iterator<String> childIterator = openMrsLocationData.getJSONObject("children").keys();
            while (childIterator.hasNext()) {
                String curChildKey = childIterator.next();
                getFormJsonData(children, openMrsLocationData.getJSONObject("children").getJSONObject(curChildKey), allowedLevels);
            }
            if (allowedLevels.contains(level)) {
                jsonFormObject.put("nodes", children);
            } else {
                for (int i = 0; i < children.length(); i++) {
                    allLocationData.put(children.getJSONObject(i));
                }
            }
        }
        if (allowedLevels.contains(level)) {
            allLocationData.put(jsonFormObject);
        }
    }

    public static String getOpenMrsReadableName(String name) {
        String readableName = new String(name);

        Pattern prefixPattern = Pattern.compile("^[a-z]{2} (.*)$");
        Matcher prefixMatcher = prefixPattern.matcher(readableName);
        if (prefixMatcher.find()) {
            readableName = prefixMatcher.group(1);
        }

        if (readableName.contains(":")) {
            String[] splitName = readableName.split(":");
            readableName = splitName[splitName.length - 1].trim();
        }

        return readableName;
    }

    public static void addChildRegLocHierarchyQuestions(JSONObject form,
                                                        org.smartregister.Context context) {
        try {
            JSONArray questions = form.getJSONObject("step1").getJSONArray("fields");
            ArrayList<String> allLevels = new ArrayList<>();
            allLevels.add("Country");
            allLevels.add("County");
            allLevels.add("Sub County");
            allLevels.add("Ward");
            allLevels.add("Health Facility");

            JSONArray defaultLocation = generateDefaultLocationHierarchy(context, allLevels);
            JSONArray defaultFacility = generateDefaultLocationHierarchy(context, new ArrayList<>(allLevels.subList(0, 5)));
            JSONArray upToFacilities = generateLocationHierarchyTree(context, false, new ArrayList<>(allLevels.subList(0, 5)));
            JSONArray entireTree = generateLocationHierarchyTree(context, true, allLevels);
            JSONArray counties = generateLocationArray("County", context, true, new ArrayList<>(allLevels.subList(1, 2)));
            JSONArray subCounties = generateLocationArray("Sub County", context, true, new ArrayList<>(allLevels.subList(2, 3)));
            JSONArray wards = generateLocationArray("Ward", context, true, new ArrayList<>(allLevels.subList(3, 4)));

            for (int i = 0; i < questions.length(); i++) {
                if (questions.getJSONObject(i).getString("key").equals("Home_Facility")) {
                    questions.getJSONObject(i).put("tree", new JSONArray(upToFacilities.toString()));
                    if (defaultFacility != null) {
                        questions.getJSONObject(i).put("default", defaultFacility.toString());
                    }
                } else if (questions.getJSONObject(i).getString("key").equals("Ce_County")) {
                    questions.getJSONObject(i).remove(JsonFormUtils.VALUES);
                    questions.getJSONObject(i).put("values", counties);

                } else if (questions.getJSONObject(i).getString("key").equals("Ce_Sub_County")) {
                    questions.getJSONObject(i).remove(JsonFormUtils.VALUES);
                    questions.getJSONObject(i).put("values", subCounties);

                } else if (questions.getJSONObject(i).getString("key").equals("Ce_Ward")) {
                    questions.getJSONObject(i).remove(JsonFormUtils.VALUES);
                    questions.getJSONObject(i).put("values", wards);

                }
            }
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private static JSONArray generateLocationArray(String locationTag, org.smartregister.Context context, boolean b, ArrayList<String> strings) throws
            JSONException {

        JSONArray jsonArray = new JSONArray();
        LocationRepository locationRepository = VaccinatorApplication.getInstance().locationRepository();

        List<Location> locations = locationRepository.getLocationsByTag(locationTag);

        if (locations != null && locations.size() > 0) {
            for (Location l : locations) {
                if (l.getTags() != null && l.getTags().contains(locationTag))
                    jsonArray.put(l.getName());
            }
            jsonArray.put("Other");
        } else {
            JSONArray array = generateLocationHierarchyTree(context, b, strings);
            for (int i = 0; i < array.length(); i++) {
                JSONObject jo = array.getJSONObject(i);
                if (jo.has("name") && StringUtils.isNotBlank(jo.getString("name"))) {
                    jsonArray.put(jo.getString("name"));
                }
            }
        }

        return jsonArray;
    }

    public static void addAddAvailableVaccines(Context context, JSONObject form) {
        String supportedVaccinesString = VaccinatorUtils.getSupportedVaccines(context);
        if (StringUtils.isNotEmpty(supportedVaccinesString) && form != null) {
            // For each of the vaccine groups, create a checkbox question
            try {
                JSONArray questionList = form.getJSONObject("step1").getJSONArray("fields");
                JSONObject vaccinationLabel = new JSONObject();
                vaccinationLabel.put("key", "Vaccines_Provided_Label");
                vaccinationLabel.put("type", "label");
                vaccinationLabel.put("text", "Which vaccinations were provided?");
                vaccinationLabel.put("openmrs_entity_parent", "-");
                vaccinationLabel.put("openmrs_entity", "-");
                vaccinationLabel.put("openmrs_entity_id", "-");
                questionList.put(vaccinationLabel);
                JSONArray supportedVaccines = new JSONArray(supportedVaccinesString);

                HashMap<String, ArrayList<JSONObject>> vaccineTypeConstraints = new HashMap<>();
                for (int i = 0; i < supportedVaccines.length(); i++) {
                    JSONObject curVaccineGroup = supportedVaccines.getJSONObject(i);
                    JSONArray vaccines = curVaccineGroup.getJSONArray("vaccines");
                    for (int j = 0; j < vaccines.length(); j++) {
                        JSONObject curVaccine = vaccines.getJSONObject(j);
                        if (!vaccineTypeConstraints.containsKey(curVaccine.getString("type"))) {
                            vaccineTypeConstraints.put(curVaccine.getString("type"),
                                    new ArrayList<JSONObject>());
                        }
                        ArrayList<String> vaccineNamesDefined = new ArrayList<>();
                        if (curVaccine.has("vaccine_separator")) {
                            String unsplitNames = curVaccine.getString("name");
                            String separator = curVaccine.getString("vaccine_separator");
                            String[] splitValues = unsplitNames.split(separator);
                            for (String splitValue : splitValues) {
                                vaccineNamesDefined.add(splitValue);
                            }
                        } else {
                            vaccineNamesDefined.add(curVaccine.getString("name"));
                        }

                        for (String curVaccineName : vaccineNamesDefined) {
                            JSONObject curConstraint = new JSONObject();
                            curConstraint.put("vaccine", curVaccineName);
                            curConstraint.put("type", "array");
                            curConstraint.put("ex",
                                    "notEqualTo(step1:" + curVaccineGroup.getString("id") + ", \"[\"" + curVaccineName + "\"]\")");
                            curConstraint.put("err", "Cannot be given with the other " + curVaccine.getString("type") + " dose");
                            vaccineTypeConstraints.get(curVaccine.getString("type")).add(curConstraint);
                        }
                    }
                }

                for (int i = 0; i < supportedVaccines.length(); i++) {
                    JSONObject curVaccineGroup = supportedVaccines.getJSONObject(i);
                    JSONObject curQuestion = new JSONObject();
                    curQuestion.put("key", curVaccineGroup.getString("id"));
                    curQuestion.put("type", "check_box");
                    curQuestion.put("is_vaccine_group", true);
                    curQuestion.put("label", curVaccineGroup.getString("name"));
                    curQuestion.put("openmrs_entity_parent", "-");
                    curQuestion.put("openmrs_entity", "-");
                    curQuestion.put("openmrs_entity_id", "-");

                    JSONArray vaccines = curVaccineGroup.getJSONArray("vaccines");
                    JSONArray options = new JSONArray();
                    for (int j = 0; j < vaccines.length(); j++) {
                        ArrayList<String> definedVaccineNames = new ArrayList<>();
                        if (vaccines.getJSONObject(j).has("vaccine_separator")) {
                            String rawNames = vaccines.getJSONObject(j).getString("name");
                            String separator = vaccines.getJSONObject(j).getString("vaccine_separator");
                            String[] split = rawNames.split(separator);
                            for (String aSplit : split) {
                                definedVaccineNames.add(aSplit);
                            }
                        } else {
                            definedVaccineNames.add(vaccines.getJSONObject(j).getString("name"));
                        }

                        for (String curVaccineName : definedVaccineNames) {
                            JSONObject curVaccines = new JSONObject();
                            curVaccines.put("key", curVaccineName);
                            curVaccines.put("text", curVaccineName);
                            curVaccines.put("value", "false");
                            JSONArray constraints = new JSONArray();

                            // Add the constraints
                            if (vaccineTypeConstraints.containsKey(vaccines.getJSONObject(j).getString("type"))) {
                                for (JSONObject curConstraint : vaccineTypeConstraints.get(vaccines.getJSONObject(j).getString("type"))) {
                                    if (!curConstraint.getString("vaccine")
                                            .equals(curVaccineName)) {
                                        JSONObject constraintClone = new JSONObject(curConstraint.toString());
                                        constraintClone.remove("vaccine");
                                        constraints.put(constraintClone);
                                    }
                                }
                            }

                            if (constraints.length() > 0) {
                                curVaccines.put("constraints", constraints);
                            }

                            options.put(curVaccines);
                        }
                    }

                    curQuestion.put("options", options);
                    questionList.put(curQuestion);
                }
            } catch (JSONException e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
        }
    }

    public static String getOpenMrsLocationId(org.smartregister.Context context,
                                              String locationName) throws JSONException {
        String response = locationName;

        if (locationName != null) {
            JSONObject locationData = new JSONObject(context.anmLocationController().get());
            if (locationData.has("locationsHierarchy")
                    && locationData.getJSONObject("locationsHierarchy").has("map")) {
                JSONObject map = locationData.getJSONObject("locationsHierarchy").getJSONObject("map");
                Iterator<String> keys = map.keys();
                while (keys.hasNext()) {
                    String curKey = keys.next();
                    String curResult = getOpenMrsLocationId(locationName, map.getJSONObject(curKey));

                    if (curResult != null) {
                        response = curResult;
                        break;
                    }
                }
            }
        }

        return response;
    }

    private static String getOpenMrsLocationId(String locationName, JSONObject
            openMrsLocations)
            throws JSONException {
        String name = openMrsLocations.getJSONObject("node").getString("name");

        if (locationName.equals(name)) {
            return openMrsLocations.getJSONObject("node").getString("locationId");
        }

        if (openMrsLocations.has("children")) {
            Iterator<String> childIterator = openMrsLocations.getJSONObject("children").keys();
            while (childIterator.hasNext()) {
                String curChildKey = childIterator.next();
                String curResult = getOpenMrsLocationId(locationName,
                        openMrsLocations.getJSONObject("children").getJSONObject(curChildKey));
                if (curResult != null) {
                    return curResult;
                }
            }
        }

        return null;
    }

    /**
     * This method returns the name hierarchy of a location given it's id
     *
     * @param context
     * @param locationId The ID for the location we want the hierarchy for
     * @return The name hierarchy (starting with the top-most parent) for the location or {@code NULL} if location id is not found
     */
    public static JSONArray getOpenMrsLocationHierarchy(org.smartregister.Context context,
                                                        String locationId) {
        JSONArray response = null;

        try {
            if (locationId != null) {
                JSONObject locationData = new JSONObject(context.anmLocationController().get());
                Log.d(TAG, "Location data is " + locationData);
                if (locationData.has(LOCATION_HIERARCHY)
                        && locationData.getJSONObject(LOCATION_HIERARCHY).has(MAP)) {
                    JSONObject map = locationData.getJSONObject(LOCATION_HIERARCHY).getJSONObject(MAP);
                    Iterator<String> keys = map.keys();
                    while (keys.hasNext()) {
                        String curKey = keys.next();
                        JSONArray curResult = getOpenMrsLocationHierarchy(locationId, map.getJSONObject(curKey), new JSONArray());

                        if (curResult != null) {
                            response = curResult;
                            break;
                        }
                    }
                } else {
                    Log.e(TAG, "locationData doesn't have locationHierarchy");
                }
            } else {
                Log.e(TAG, "Location id is null");
            }
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }

        return response;
    }

    private static JSONArray getOpenMrsLocationHierarchy(String locationId,
                                                         JSONObject openMrsLocation,
                                                         JSONArray parents) throws JSONException {
        JSONArray hierarchy = new JSONArray(parents.toString());
        hierarchy.put(openMrsLocation.getJSONObject("node").getString("name"));
        String id = openMrsLocation.getJSONObject("node").getString("locationId");
        Log.d(TAG, "Current location id is " + id);
        if (locationId.equals(id)) {
            return hierarchy;
        }

        if (openMrsLocation.has("children")) {
            Iterator<String> childIterator = openMrsLocation.getJSONObject("children").keys();
            while (childIterator.hasNext()) {
                String curChildKey = childIterator.next();
                JSONArray curResult = getOpenMrsLocationHierarchy(locationId,
                        openMrsLocation.getJSONObject("children").getJSONObject(curChildKey),
                        hierarchy);
                if (curResult != null) return curResult;
            }
        } else {
            Log.d(TAG, id + " does not have children");
        }

        return null;
    }

    public static String getOpenMrsLocationName(org.smartregister.Context context,
                                                String locationId) {
        String response = locationId;
        try {
            if (locationId != null) {
                JSONObject locationData = new JSONObject(context.anmLocationController().get());
                Log.d(TAG, "Location data is " + locationData);
                if (locationData.has(LOCATION_HIERARCHY)
                        && locationData.getJSONObject(LOCATION_HIERARCHY).has(MAP)) {
                    JSONObject map = locationData.getJSONObject(LOCATION_HIERARCHY).getJSONObject(MAP);
                    Iterator<String> keys = map.keys();
                    while (keys.hasNext()) {
                        String curKey = keys.next();
                        String curResult = getOpenMrsLocationName(locationId, map.getJSONObject(curKey));

                        if (curResult != null) {
                            response = curResult;
                            break;
                        }
                    }
                } else {
                    Log.e(TAG, "locationData doesn't have locationHierarchy");
                }
            } else {
                Log.e(TAG, "Location id is null");
            }
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }

        return response;
    }

    private static String getOpenMrsLocationName(String locationId, JSONObject
            openMrsLocations)
            throws JSONException {
        String id = openMrsLocations.getJSONObject("node").getString("locationId");
        Log.d(TAG, "Current location id is " + id);
        if (locationId.equals(id)) {
            return openMrsLocations.getJSONObject("node").getString("name");
        }

        if (openMrsLocations.has("children")) {
            Iterator<String> childIterator = openMrsLocations.getJSONObject("children").keys();
            while (childIterator.hasNext()) {
                String curChildKey = childIterator.next();
                String curResult = getOpenMrsLocationName(locationId,
                        openMrsLocations.getJSONObject("children").getJSONObject(curChildKey));
                if (curResult != null) {
                    return curResult;
                }
            }
        } else {
            Log.d(TAG, id + " does not have children");
        }

        return null;
    }

    public static void saveReportDeceased(Context context, org.smartregister.Context
            openSrpContext,
                                          String jsonString, String providerId, String locationId, String entityId) {

        try {
            EventClientRepository db = VaccinatorApplication.getInstance().eventClientRepository();

            JSONObject jsonForm = new JSONObject(jsonString);

            JSONArray fields = fields(jsonForm);
            if (fields == null) {
                return;
            }

            String encounterDateField = getFieldValue(fields, "Date_of_Death");

            String encounterType = getString(jsonForm, ENCOUNTER_TYPE);
            JSONObject metadata = getJSONObject(jsonForm, METADATA);

            Date encounterDate = new Date();
            String encounterDateTimeString = null;
            if (StringUtils.isNotBlank(encounterDateField)) {
                encounterDateTimeString = formatDate(encounterDateField);
                Date dateTime = formatDate(encounterDateField, false);
                if (dateTime != null) {
                    encounterDate = dateTime;
                }
            }

            Event event = (Event) new Event()
                    .withBaseEntityId(entityId) //should be different for main and subform
                    .withEventDate(encounterDate)
                    .withEventType(encounterType)
                    .withLocationId(locationId)
                    .withProviderId(providerId)
                    .withEntityType(PathConstants.EntityType.CHILD)
                    .withFormSubmissionId(generateRandomUUIDString())
                    .withDateCreated(new Date());

            for (int i = 0; i < fields.length(); i++) {
                JSONObject jsonObject = getJSONObject(fields, i);
                String value = getString(jsonObject, VALUE);
                if (StringUtils.isNotBlank(value)) {
                    addObservation(event, jsonObject);
                }
            }

            if (metadata != null) {
                Iterator<?> keys = metadata.keys();

                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    JSONObject jsonObject = getJSONObject(metadata, key);
                    String value = getString(jsonObject, VALUE);
                    if (StringUtils.isNotBlank(value)) {
                        String entityVal = getString(jsonObject, OPENMRS_ENTITY);
                        if (entityVal != null) {
                            if (entityVal.equals(CONCEPT)) {
                                addToJSONObject(jsonObject, KEY, key);
                                addObservation(event, jsonObject);
                            } else if (entityVal.equals(ENCOUNTER)) {
                                String entityIdVal = getString(jsonObject, OPENMRS_ENTITY_ID);
                                if (entityIdVal.equals(FormEntityConstants.Encounter.encounter_date.name())) {
                                    Date eDate = formatDate(value, false);
                                    if (eDate != null) {
                                        event.setEventDate(eDate);
                                    }
                                }
                            }
                        }
                    }
                }
            }


            if (event != null) {
                JSONObject eventJson = new JSONObject(JsonFormUtils.gson.toJson(event));

                //After saving, Unsync(remove) this event's details
                //List<JSONObject> jsonEvents = new ArrayList<>();
                ///jsonEvents.add(eventJson);

                //Update client to deceased
                JSONObject client = db.getClientByBaseEntityId(eventJson.getString(ClientProcessor.baseEntityIdJSONKey));
                client.put("deathdate", encounterDateTimeString);
                client.put("deathdateApprox", false);
                db.addorUpdateClient(entityId, client);

                //Add Death Event for child to flag for Server delete
                db.addEvent(event.getBaseEntityId(), eventJson);

                //Update Child Entity to include death date
                Event updateChildDetailsEvent = (Event) new Event()
                        .withBaseEntityId(entityId) //should be different for main and subform
                        .withEventDate(encounterDate)
                        .withEventType(JsonFormUtils.encounterType)
                        .withLocationId(locationId)
                        .withProviderId(providerId)
                        .withEntityType(PathConstants.EntityType.CHILD)
                        .withFormSubmissionId(generateRandomUUIDString())
                        .withDateCreated(new Date());
                JsonFormUtils.addMetaData(context, updateChildDetailsEvent, new Date());
                JSONObject eventJsonUpdateChildEvent = new JSONObject(JsonFormUtils.gson.toJson(updateChildDetailsEvent));

                db.addEvent(entityId, eventJsonUpdateChildEvent); //Add event to flag server update

                //Update REGISTER and FTS Tables
                String tableName = PathConstants.CHILD_TABLE_NAME;
                AllCommonsRepository allCommonsRepository = openSrpContext.allCommonsRepositoryobjects(tableName);
                if (allCommonsRepository != null) {
                    ContentValues values = new ContentValues();
                    values.put(PathConstants.EC_CHILD_TABLE.DOD, PathConstants.DEFAULT_DATE_STRING);
                    allCommonsRepository.update(tableName, values, entityId);
                    allCommonsRepository.updateSearch(entityId);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "", e);
        }
    }

    /**
     * Starts an instance of JsonFormActivity with the provided form details
     *
     * @param context                     The activity form is being launched from
     * @param openSrpContext              Current OpenSRP context
     * @param jsonFormActivityRequestCode The request code to be used to launch {@link PathJsonFormActivity}
     * @param formName                    The name of the form to launch
     * @param entityId                    The unique entity id for the form (e.g child's ZEIR id)
     * @param metaData                    The form's meta data
     * @param currentLocationId           OpenMRS id for the current device's location
     * @throws Exception
     */
    public static void startForm(Activity context, org.smartregister.path.context.Context openSrpContext,
                                 int jsonFormActivityRequestCode,
                                 String formName, String entityId, String metaData,
                                 String currentLocationId) throws Exception {
        Intent intent = new Intent(context, PathJsonFormActivity.class);

        JSONObject form = FormUtils.getInstance(context).getFormJson(formName);
        if (form != null) {
            form.getJSONObject("metadata").put("encounter_location", currentLocationId);

            if (formName.equals("kip_child_enrollment")) {
                if (StringUtils.isBlank(entityId)) {
                    UniqueIdRepository uniqueIdRepo = VaccinatorApplication.getInstance().uniqueIdRepository();
                    entityId = uniqueIdRepo.getNextUniqueId() != null ? uniqueIdRepo.getNextUniqueId().getOpenmrsId() : "";

                    if (entityId.isEmpty() || uniqueIdRepo.countUnUsedIds() < 3) {
                        // If entityId is empty or if unused < 3, to cater for child, mother & guardian
                        Toast.makeText(context, context.getString(R.string.no_openmrs_id), Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                JsonFormUtils.addChildRegLocHierarchyQuestions(form, openSrpContext);

                JsonFormUtils.addRelationshipTypesQuestions(form, openSrpContext);

                // Inject zeir id into the form
                JSONObject stepOne = form.getJSONObject(JsonFormUtils.STEP1);
                JSONArray jsonArray = stepOne.getJSONArray(JsonFormUtils.FIELDS);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    switch (jsonObject.getString(JsonFormUtils.KEY)) {
                        case JsonFormUtils.KIP_ID:
                            jsonObject.remove(JsonFormUtils.VALUE);
                            jsonObject.put(JsonFormUtils.VALUE, entityId);
                            continue;
                        default:
                    }
                }
            } else if (formName.equals("out_of_catchment_service")) {
                if (StringUtils.isNotBlank(entityId)) {
                    entityId = entityId.replace("-", "");
                } else {
                    JSONArray fields = form.getJSONObject("step1").getJSONArray("fields");
                    for (int i = 0; i < fields.length(); i++) {
                        if (fields.getJSONObject(i).getString("key").equals("KIP_ID")) {
                            fields.getJSONObject(i).put(READ_ONLY, false);
                            break;
                        }
                    }
                }

                JSONObject stepOne = form.getJSONObject(JsonFormUtils.STEP1);
                JSONArray jsonArray = stepOne.getJSONArray(JsonFormUtils.FIELDS);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    switch (jsonObject.getString(JsonFormUtils.KEY)) {
                        case JsonFormUtils.KIP_ID:
                            jsonObject.remove(JsonFormUtils.VALUE);
                            jsonObject.put(JsonFormUtils.VALUE, entityId);
                            continue;
                        default:
                    }
                }

                JsonFormUtils.addAddAvailableVaccines(context, form);
            } else {
                Log.w(TAG, "Unsupported form requested for launch " + formName);
            }

            intent.putExtra("json", form.toString());
            Log.d(TAG, "form is " + form.toString());
            context.startActivityForResult(intent, jsonFormActivityRequestCode);
        }
    }

    /**
     * Popul
     *
     * @param form
     * @param context
     */
    public static void addRelationshipTypesQuestions(JSONObject
                                                             form, org.smartregister.path.context.Context context) {
        try {
            JSONArray questions = form.getJSONObject("step1").getJSONArray("fields");
            JSONArray relationshipTypes = new JSONObject(context.allSettings().fetchRelationshipTypes()).getJSONArray("relationshipTypes");

            for (int i = 0; i < questions.length(); i++) {
                if (questions.getJSONObject(i).getString("key").equals("Mother_Guardian_Relationship")
                        || questions.getJSONObject(i).getString("key").equals("Father_Guardian_Relationship")) {

                    JSONArray values = new JSONArray();
                    String value = "";

                    if (relationshipTypes != null && relationshipTypes.length() > 0) {
                        for (int n = 0; n < relationshipTypes.length(); n++) {
                            JSONObject rType = new JSONObject(relationshipTypes.getString(n));
                            values.put(rType.getString("name"));
                            if (rType.has("key") && rType.get("key").equals(JsonFormUtils.UNIVERSAL_OPENMRS_RELATIONSHIP_TYPE_UUID)) {
                                value = rType.getString("name");
                            }
                        }
                    }

                    questions.getJSONObject(i).remove(JsonFormUtils.VALUES);
                    questions.getJSONObject(i).put(JsonFormUtils.VALUES, values);
                    // Set the default relationship type.
                    questions.getJSONObject(i).remove(JsonFormUtils.VALUE);
                    questions.getJSONObject(i).put(JsonFormUtils.VALUE, value);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    public static Event addMetaData(Context context, Event event, Date start) throws
            JSONException {
        Map<String, String> metaFields = new HashMap<>();
        metaFields.put("deviceid", "163149AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        metaFields.put("end", "163138AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        metaFields.put("start", "163137AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        Calendar calendar = Calendar.getInstance();

        String end = DATE_TIME_FORMAT.format(calendar.getTime());

        Obs obs = new Obs();
        obs.setFieldCode("163137AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        obs.setValue(DATE_TIME_FORMAT.format(start));
        obs.setFieldType("concept");
        obs.setFieldDataType("start");
        event.addObs(obs);


        obs.setFieldCode("163137AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        obs.setValue(end);
        obs.setFieldDataType("end");
        event.addObs(obs);

        TelephonyManager mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        String deviceId = mTelephonyManager.getSimSerialNumber();

        obs.setFieldCode("163137AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        obs.setValue(deviceId);
        obs.setFieldDataType("deviceid");
        event.addObs(obs);
        return event;
    }

    /**
     * This method sorts the options provided for a native form tree view question
     *
     * @return The sorted options
     */
    private static JSONArray sortTreeViewQuestionOptions(JSONArray treeViewOptions) throws
            JSONException {
        JSONArray sortedTree = new JSONArray();

        HashMap<String, JSONObject> sortMap = new HashMap<>();
        for (int i = 0; i < treeViewOptions.length(); i++) {
            sortMap.put(treeViewOptions.getJSONObject(i).getString("name"), treeViewOptions.getJSONObject(i));
        }

        ArrayList<String> sortedKeys = new ArrayList<>(sortMap.keySet());
        Collections.sort(sortedKeys);

        for (String curOptionName : sortedKeys) {
            JSONObject curOption = sortMap.get(curOptionName);
            if (curOption.has("nodes")) {
                curOption.put("nodes", sortTreeViewQuestionOptions(curOption.getJSONArray("nodes")));
            }

            sortedTree.put(curOption);
        }

        return sortedTree;
    }

    public static Event createMoveToCatchmentEvent(Context context, org.smartregister.domain.db.Event referenceEvent, String fromLocationId, String
            toProviderId, String toLocationId) {

        try {
            final String FORM_SUBMISSION_FIELD = "formsubmissionField";
            final String DATA_TYPE = "text";

            Event event = (Event) new Event()
                    .withBaseEntityId(referenceEvent.getBaseEntityId())
                    .withEventDate(new Date())
                    .withEventType(MoveToMyCatchmentUtils.MOVE_TO_CATCHMENT_EVENT)
                    .withLocationId(fromLocationId)
                    .withProviderId(referenceEvent.getProviderId())
                    .withEntityType("child")
                    .withFormSubmissionId(JsonFormUtils.generateRandomUUIDString())
                    .withDateCreated(new Date());


            String formSubmissionField = "From_ProviderId";
            List<Object> vall = new ArrayList<>();
            vall.add(referenceEvent.getProviderId());
            event.addObs(new Obs(FORM_SUBMISSION_FIELD, DATA_TYPE, formSubmissionField,
                    "", vall, new ArrayList<>(), null, formSubmissionField));

            formSubmissionField = "From_LocationId";
            vall = new ArrayList<>();
            vall.add(fromLocationId);
            event.addObs(new Obs(FORM_SUBMISSION_FIELD, DATA_TYPE, formSubmissionField,
                    "", vall, new ArrayList<>(), null, formSubmissionField));

            formSubmissionField = "To_ProviderId";
            vall = new ArrayList<>();
            vall.add(toProviderId);
            event.addObs(new Obs(FORM_SUBMISSION_FIELD, DATA_TYPE, formSubmissionField,
                    "", vall, new ArrayList<>(), null, formSubmissionField));

            formSubmissionField = "To_LocationId";
            vall = new ArrayList<>();
            vall.add(toLocationId);
            event.addObs(new Obs(FORM_SUBMISSION_FIELD, DATA_TYPE, formSubmissionField,
                    "", vall, new ArrayList<>(), null, formSubmissionField));

            addMetaData(context, event, new Date());

            return event;

        } catch (JSONException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return null;
        }
    }

    ////////////////////////////////////////////////////////////////
    // Inner classes
    ////////////////////////////////////////////////////////////////
    private static class SaveAdverseEventTask extends AsyncTask<Void, Void, Void> {
        private final String jsonString;
        private final String locationId;
        private final String baseEntityId;
        private final String providerId;

        private SaveAdverseEventTask(String jsonString, String locationId, String baseEntityId,
                                     String providerId) {
            this.jsonString = jsonString;
            this.locationId = locationId;
            this.baseEntityId = baseEntityId;
            this.providerId = providerId;
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                EventClientRepository db = VaccinatorApplication.getInstance().eventClientRepository();

                JSONObject jsonForm = new JSONObject(jsonString);

                JSONArray fields = fields(jsonForm);
                if (fields == null) {
                    return null;
                }

                String bindType = "child";
                String encounterDateField = getFieldValue(fields, "Date_Reaction");

                String encounterType = getString(jsonForm, ENCOUNTER_TYPE);
                JSONObject metadata = getJSONObject(jsonForm, METADATA);

                Date encounterDate = new Date();
                if (StringUtils.isNotBlank(encounterDateField)) {
                    Date dateTime = formatDate(encounterDateField, false);
                    if (dateTime != null) {
                        encounterDate = dateTime;
                    }
                }

                Event event = (Event) new Event()
                        .withBaseEntityId(baseEntityId) //should be different for main and subform
                        .withEventDate(encounterDate)
                        .withEventType(encounterType)
                        .withLocationId(locationId)
                        .withProviderId(providerId)
                        .withEntityType(bindType)
                        .withFormSubmissionId(generateRandomUUIDString())
                        .withDateCreated(new Date());

                for (int i = 0; i < fields.length(); i++) {
                    JSONObject jsonObject = getJSONObject(fields, i);
                    String value = getString(jsonObject, VALUE);
                    if (StringUtils.isNotBlank(value)) {
                        addObservation(event, jsonObject);
                    }
                }

                if (metadata != null) {
                    Iterator<?> keys = metadata.keys();

                    while (keys.hasNext()) {
                        String key = (String) keys.next();
                        JSONObject jsonObject = getJSONObject(metadata, key);
                        String value = getString(jsonObject, VALUE);
                        if (StringUtils.isNotBlank(value)) {
                            String entityVal = getString(jsonObject, OPENMRS_ENTITY);
                            if (entityVal != null) {
                                if (entityVal.equals(CONCEPT)) {
                                    addToJSONObject(jsonObject, KEY, key);
                                    addObservation(event, jsonObject);
                                } else if (entityVal.equals(ENCOUNTER)) {
                                    String entityIdVal = getString(jsonObject, OPENMRS_ENTITY_ID);
                                    if (entityIdVal.equals(
                                            FormEntityConstants.Encounter.encounter_date.name())) {
                                        Date eDate = formatDate(value, false);
                                        if (eDate != null) {
                                            event.setEventDate(eDate);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }


                if (event != null) {
                    JSONObject eventJson = new JSONObject(JsonFormUtils.gson.toJson(event));
                    db.addEvent(event.getBaseEntityId(), eventJson);

                }

            } catch (Exception e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }

            return null;
        }
    }

    private static class SaveOutOfAreaServiceTask extends AsyncTask<Void, Void, Void> {

        private final Context context;
        private final org.smartregister.Context openSrpContext;
        private final String formString;

        private SaveOutOfAreaServiceTask(Context context, org.smartregister.Context openSrpContext, String formString) {
            this.context = context;
            this.openSrpContext = openSrpContext;
            this.formString = formString;
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                JSONObject form = new JSONObject(formString);

                // Create a weight object if weight was recorded
                Weight weight = getWeightObject(openSrpContext, form);
                if (weight != null) {
                    WeightRepository weightRepository = VaccinatorApplication.getInstance().weightRepository();
                    weightRepository.add(weight);
                }

                // Create a vaccine object for all recorded vaccines
                ArrayList<Vaccine> vaccines = getVaccineObjects(context, openSrpContext, form);
                if (vaccines.size() > 0) {
                    VaccineRepository vaccineRepository = VaccinatorApplication.getInstance().vaccineRepository();
                    for (Vaccine curVaccine : vaccines) {
                        vaccineRepository.add(curVaccine);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
            return null;
        }
    }
}