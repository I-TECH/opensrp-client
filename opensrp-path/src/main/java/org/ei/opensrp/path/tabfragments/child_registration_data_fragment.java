package org.ei.opensrp.path.tabfragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TextView;

import org.ei.opensrp.Context;
import org.ei.opensrp.commonregistry.CommonPersonObject;
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


public class child_registration_data_fragment extends Fragment {
    public CommonPersonObjectClient childDetails;
    public Map<String, String> Detailsmap;
    private LayoutInflater inflater;
    private ViewGroup container;
    private LinearLayout layout;

    public child_registration_data_fragment() {
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
        LinearLayout layout = (LinearLayout)fragmentview.findViewById(R.id.rowholder);
        this.inflater = inflater;
        this.container = container;
        this.layout = layout;
        LoadData();

//        layout.addView(createTableRow(inflater,container,"Catchment Area","Linda"));
//        layout.addView(createTableRow(inflater,container,"Catchment Area","Linda"));
//        layout.addView(createTableRow(inflater,container,"Catchment Area","Linda"));
//        layout.addView(createTableRow(inflater,container,"Catchment Area","Linda"));
//        layout.addView(createTableRow(inflater,container,"Catchment Area","Linda"));
//        layout.addView(createTableRow(inflater,container,"Catchment Area","Linda"));
//        layout.addView(createTableRow(inflater,container,"Catchment Area","Linda"));
//        layout.addView(createTableRow(inflater,container,"Catchment Area","Linda"));
//        layout.addView(createTableRow(inflater,container,"Catchment Area","Linda"));




        // Inflate the layout for this fragment
        return fragmentview;
    }

    public void LoadData() {
        if(layout != null && container!=null && inflater!=null) {
            if (layout.getChildCount() > 0) {
                layout.removeAllViews();
            }

            DetailsRepository detailsRepository = org.ei.opensrp.Context.getInstance().detailsRepository();
            Detailsmap = detailsRepository.getAllDetailsForClient(childDetails.entityId());

//        Detailsmap = childDetails.getColumnmaps();
            WidgetFactory wd = new WidgetFactory();

            layout.addView(wd.createTableRow(inflater, container, "Child's home health facility", fixlocationview(JsonFormUtils.getOpenMrsLocationName(Context.getInstance(), Utils.getValue(Detailsmap, "Home_Facility", false)))));
            layout.addView(wd.createTableRow(inflater, container, "Child's KIP ID", Utils.getValue(childDetails.getColumnmaps(), "zeir_id", false)));
            layout.addView(wd.createTableRow(inflater, container, "Child's register card number", Utils.getValue(Detailsmap, "Child_Register_Card_Number", false)));
            layout.addView(wd.createTableRow(inflater, container, "Child's birth certificate number", Utils.getValue(Detailsmap, "Child_Birth_Certificate", false)));
            layout.addView(wd.createTableRow(inflater, container, "First name", Utils.getValue(childDetails.getColumnmaps(), "first_name", true)));
            layout.addView(wd.createTableRow(inflater, container, "Last name", Utils.getValue(childDetails.getColumnmaps(), "last_name", true)));
            layout.addView(wd.createTableRow(inflater, container, "Sex", Utils.getValue(childDetails.getColumnmaps(), "gender", true)));
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


            layout.addView(wd.createTableRow(inflater, container, "Date first seen", Utils.getValue(Detailsmap, "First_Health_Facility_Contact", true)));
            layout.addView(wd.createTableRow(inflater, container, "Birth weight", Utils.getValue(Detailsmap, "Birth_Weight", true) + " kg"));

            layout.addView(wd.createTableRow(inflater, container, "Mother/guardian first name", (Utils.getValue(childDetails.getColumnmaps(), "mother_first_name", true).isEmpty()?Utils.getValue(childDetails.getDetails(), "mother_first_name", true):Utils.getValue(childDetails.getColumnmaps(), "mother_first_name", true))));
            layout.addView(wd.createTableRow(inflater, container, "Mother/guardian last name",(Utils.getValue(childDetails.getColumnmaps(), "mother_last_name", true).isEmpty() ? Utils.getValue(childDetails.getDetails(), "mother_last_name", true) : Utils.getValue(childDetails.getColumnmaps(), "mother_last_name", true))));
            String motherDob = Utils.getValue(childDetails, "mother_dob", true);
            if (motherDob != null && motherDob.equals(JsonFormUtils.MOTHER_DEFAULT_DOB)) {
                motherDob = "";
            }else{
                try {
                    DateTime dateTime = new DateTime(motherDob);
                    Date mother_dob = dateTime.toDate();
                    motherDob = ChildDetailTabbedActivity.DATE_FORMAT.format(mother_dob);
                }catch (Exception e){

                }
            }
            layout.addView(wd.createTableRow(inflater, container, "Mother/guardian DOB",motherDob));

            layout.addView(wd.createTableRow(inflater, container, "Mother/guardian NRC number", Utils.getValue(childDetails, "mother_nrc_number", true)));
            layout.addView(wd.createTableRow(inflater, container, "Mother/guardian phone number", Utils.getValue(Detailsmap, "Mother_Guardian_Number", true)));
            layout.addView(wd.createTableRow(inflater, container, "Father/guardian full name", Utils.getValue(Detailsmap, "Father_Guardian_Name", true)));
            layout.addView(wd.createTableRow(inflater, container, "Father/guardian NRC number", Utils.getValue(Detailsmap, "Father_NRC_Number", true)));

            layout.addView(wd.createTableRow(inflater, container, "County", fixlocationview(JsonFormUtils.getOpenMrsLocationName(Context.getInstance(), Utils.getValue(Detailsmap, "stateProvince", true)))));
            layout.addView(wd.createTableRow(inflater, container, "Sub County", fixlocationview(JsonFormUtils.getOpenMrsLocationName(Context.getInstance(), Utils.getValue(Detailsmap, "countyDistrict", true)))));
            layout.addView(wd.createTableRow(inflater, container, "Ward", fixlocationview(JsonFormUtils.getOpenMrsLocationName(Context.getInstance(), Utils.getValue(Detailsmap, "cityVillage", true)))));
            layout.addView(wd.createTableRow(inflater, container, "Location", fixlocationview(JsonFormUtils.getOpenMrsLocationName(Context.getInstance(), Utils.getValue(Detailsmap, "address5", true)))));
            layout.addView(wd.createTableRow(inflater, container, "Sub Location", fixlocationview(JsonFormUtils.getOpenMrsLocationName(Context.getInstance(), Utils.getValue(Detailsmap, "address4", true)))));
            layout.addView(wd.createTableRow(inflater, container, "Village", fixlocationview(JsonFormUtils.getOpenMrsLocationName(Context.getInstance(), Utils.getValue(Detailsmap, "address3", true)))));
            layout.addView(wd.createTableRow(inflater, container, "Landmark", fixlocationview(JsonFormUtils.getOpenMrsLocationName(Context.getInstance(), Utils.getValue(Detailsmap, "address2", true)))));
            layout.addView(wd.createTableRow(inflater, container, "Address", fixlocationview(JsonFormUtils.getOpenMrsLocationName(Context.getInstance(), Utils.getValue(Detailsmap, "address1", true)))));

            layout.addView(wd.createTableRow(inflater, container, "CHW name", Utils.getValue(Detailsmap, "CHW_Name", true)));
            layout.addView(wd.createTableRow(inflater, container, "CHW phone number", Utils.getValue(Detailsmap, "CHW_Phone_Number", true)));
        }
    }

    public String fixlocationview(String value){
        if(value.contains("[")){
            value = value.replace("[","").replace("]","");
            if(value.contains(",")){
                value = value.split(",")[value.split(",").length-1];
            }
            if(value.contains("\"")){
                value = value.replace("\"","");
            }
            return value;
        }else{
            return value;
        }
    }




}
