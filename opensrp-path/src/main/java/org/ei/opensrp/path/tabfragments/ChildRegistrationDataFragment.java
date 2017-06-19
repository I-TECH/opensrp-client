package org.ei.opensrp.path.tabfragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import org.ei.opensrp.Context;
import org.ei.opensrp.commonregistry.CommonPersonObjectClient;
import org.ei.opensrp.path.R;
import org.ei.opensrp.path.activity.ChildDetailTabbedActivity;
import org.ei.opensrp.path.viewComponents.WidgetFactory;
import org.ei.opensrp.repository.DetailsRepository;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import util.DateUtils;
import util.JsonFormUtils;
import util.Utils;


public class ChildRegistrationDataFragment extends Fragment {
    public CommonPersonObjectClient childDetails;
    public Map<String, String> detailsMap;
    private LayoutInflater inflater;
    private ViewGroup container;
    private LinearLayout layout;

    public ChildRegistrationDataFragment() {
        // Required empty public constructor

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = this.getArguments();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (this.getArguments() != null) {
            Serializable serializable = getArguments().getSerializable(ChildDetailTabbedActivity.EXTRA_CHILD_DETAILS);
            if (serializable != null && serializable instanceof CommonPersonObjectClient) {
                childDetails = (CommonPersonObjectClient) serializable;
            }
        }
        View fragmentview = inflater.inflate(R.layout.child_registration_data_fragment, container, false);
        LinearLayout layout = (LinearLayout) fragmentview.findViewById(R.id.rowholder);
        this.inflater = inflater;
        this.container = container;
        this.layout = layout;

        return fragmentview;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    public void loadData() {
        if (layout != null && container != null && inflater != null) {
            if (layout.getChildCount() > 0) {
                layout.removeAllViews();
            }

            DetailsRepository detailsRepository = org.ei.opensrp.Context.getInstance().detailsRepository();
            detailsMap = detailsRepository.getAllDetailsForClient(childDetails.entityId());

            WidgetFactory wd = new WidgetFactory();

            layout.addView(wd.createTableRow(inflater, container, "Child's home health facility", JsonFormUtils.getOpenMrsReadableName(JsonFormUtils.getOpenMrsLocationName(Context.getInstance(), Utils.getValue(detailsMap, "Home_Facility", false)))));
            layout.addView(wd.createTableRow(inflater, container, "First name", Utils.getValue(childDetails.getColumnmaps(), "first_name", true)));
            layout.addView(wd.createTableRow(inflater, container, "Last name", Utils.getValue(childDetails.getColumnmaps(), "last_name", true)));
            layout.addView(wd.createTableRow(inflater, container, "Child's DOB", ChildDetailTabbedActivity.DATE_FORMAT.format(new DateTime(Utils.getValue(childDetails.getColumnmaps(), "dob", true)).toDate())));

            String formattedAge = "";
            String dobString = Utils.getValue(childDetails.getColumnmaps(), "dob", false);
            if (!TextUtils.isEmpty(dobString)) {
                DateTime dateTime = new DateTime(dobString);
                Date dob = dateTime.toDate();
                long timeDiff = Calendar.getInstance().getTimeInMillis() - dob.getTime();

                if (timeDiff >= 0) {
                    formattedAge = DateUtils.getDuration(timeDiff);
                }
            }
            layout.addView(wd.createTableRow(inflater, container, "Age", formattedAge));

            layout.addView(wd.createTableRow(inflater, container, "Gender", Utils.getValue(childDetails.getColumnmaps(), "gender", true)));
            layout.addView(wd.createTableRow(inflater, container, "Permanent Register number", Utils.getValue(childDetails.getColumnmaps(), "epi_card_number", false)));
            layout.addView(wd.createTableRow(inflater, container, "Child's KIP ID", Utils.getValue(childDetails.getColumnmaps(), "zeir_id", false)));
            layout.addView(wd.createTableRow(inflater, container, "NUPI", Utils.getValue(childDetails.getColumnmaps(), "nupi_number", false)));
            layout.addView(wd.createTableRow(inflater, container, "CWC number", Utils.getValue(childDetails.getColumnmaps(), "cwc_number", false)));
            layout.addView(wd.createTableRow(inflater, container, "HDSS number", Utils.getValue(childDetails.getColumnmaps(), "hdss_number", false)));
            layout.addView(wd.createTableRow(inflater, container, "Child's birth notification number", Utils.getValue(detailsMap, "Child_Birth_Notification", false)));

            String dateString = Utils.getValue(detailsMap, "First_Health_Facility_Contact", false);
            if (!TextUtils.isEmpty(dateString)) {
                Date date = JsonFormUtils.formatDate(dateString, false);
                if (date != null) {
                    dateString = ChildDetailTabbedActivity.DATE_FORMAT.format(date);
                }
            }
            layout.addView(wd.createTableRow(inflater, container, "Date first seen", dateString));

            layout.addView(wd.createTableRow(inflater, container, "Mother/guardian first name", (Utils.getValue(childDetails.getColumnmaps(), "mother_first_name", true).isEmpty() ? Utils.getValue(childDetails.getDetails(), "mother_first_name", true) : Utils.getValue(childDetails.getColumnmaps(), "mother_first_name", true))));
            layout.addView(wd.createTableRow(inflater, container, "Mother/guardian last name", (Utils.getValue(childDetails.getColumnmaps(), "mother_last_name", true).isEmpty() ? Utils.getValue(childDetails.getDetails(), "mother_last_name", true) : Utils.getValue(childDetails.getColumnmaps(), "mother_last_name", true))));
            layout.addView(wd.createTableRow(inflater, container, "Mother/guardian gender", Utils.getValue(childDetails, "mother_gender", true)));
            String motherDob = Utils.getValue(childDetails, "mother_dob", true);

            try {
                DateTime dateTime = new DateTime(motherDob);
                Date mother_dob = dateTime.toDate();
                motherDob = ChildDetailTabbedActivity.DATE_FORMAT.format(mother_dob);
            } catch (Exception e) {

            }

            motherDob = motherDob != null && motherDob.equals(JsonFormUtils.MOTHER_DEFAULT_DOB) ? "" : motherDob;
            layout.addView(wd.createTableRow(inflater, container, "Mother/guardian DOB", motherDob));
            layout.addView(wd.createTableRow(inflater, container, "Relationship to child", JsonFormUtils.getRelationshipType(Context.getInstance(), Utils.getValue(childDetails.getColumnmaps(), "m_relationship_type", true))));
            layout.addView(wd.createTableRow(inflater, container, "Mother/guardian ID number", Utils.getValue(childDetails, "mother_id_number", true)));
            layout.addView(wd.createTableRow(inflater, container, "Mother/guardian phone number", Utils.getValue(detailsMap, "Mother_Guardian_Number", true)));

            layout.addView(wd.createTableRow(inflater, container, "Father/guardian full name", Utils.getValue(childDetails.getColumnmaps(), "guardian_name", true)));
            layout.addView(wd.createTableRow(inflater, container, "Father/guardian Gender", Utils.getValue(childDetails.getColumnmaps(), "guardian_gender", true)));

            String guardianDob = Utils.getValue(childDetails.getColumnmaps(), "guardian_dob", true);
            try {
                DateTime dateTime = new DateTime(guardianDob);
                Date guardian_dob = dateTime.toDate();
                guardianDob = ChildDetailTabbedActivity.DATE_FORMAT.format(guardian_dob);
            } catch (Exception e) {

            }

            guardianDob = guardianDob != null && guardianDob.equals(JsonFormUtils.FATHER_DEFAULT_DOB) ? "" : guardianDob;
            layout.addView(wd.createTableRow(inflater, container, "Father/guardian Birthdate", guardianDob));

            layout.addView(wd.createTableRow(inflater, container, "Relationship to child", JsonFormUtils.getRelationshipType(Context.getInstance(), Utils.getValue(childDetails.getColumnmaps(), "g_relationship_type", true))));
            layout.addView(wd.createTableRow(inflater, container, "Father/guardian ID number", Utils.getValue(childDetails.getColumnmaps(), "guardian_id_number", true)));

            layout.addView(wd.createTableRow(inflater, container, "County", Utils.getValue(detailsMap, "stateProvince", true)));
            layout.addView(wd.createTableRow(inflater, container, "Sub County", Utils.getValue(detailsMap, "countyDistrict", true)));
            layout.addView(wd.createTableRow(inflater, container, "Ward", Utils.getValue(detailsMap, "cityVillage", true)));
            //layout.addView(wd.createTableRow(inflater, container, "Location", JsonFormUtils.getOpenMrsReadableName(JsonFormUtils.getOpenMrsLocationName(Context.getInstance(), Utils.getValue(detailsMap, "address5", true)))));
            layout.addView(wd.createTableRow(inflater, container, "Sub Location", Utils.getValue(detailsMap, "address4", true)));
            layout.addView(wd.createTableRow(inflater, container, "Village", Utils.getValue(detailsMap, "address3", true)));
            layout.addView(wd.createTableRow(inflater, container, "Landmark", Utils.getValue(detailsMap, "address2", true)));
            layout.addView(wd.createTableRow(inflater, container, "Address", Utils.getValue(detailsMap, "address1", true)));

            layout.addView(wd.createTableRow(inflater, container, "CHW name", Utils.getValue(detailsMap, "CHW_Name", true)));
            layout.addView(wd.createTableRow(inflater, container, "CHW phone number", Utils.getValue(detailsMap, "CHW_Phone_Number", true)));

            layout.addView(wd.createTableRow(inflater, container, "HIV exposure", Utils.getValue(childDetails.getColumnmaps(), "pmtct_status", true)));
        }
    }
}
