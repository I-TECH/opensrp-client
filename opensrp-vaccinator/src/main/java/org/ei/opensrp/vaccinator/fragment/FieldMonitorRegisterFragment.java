package org.ei.opensrp.vaccinator.fragment;

import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import com.google.gson.Gson;

import org.ei.opensrp.Context;
import org.ei.opensrp.commonregistry.CommonPersonObjectClient;
import org.ei.opensrp.commonregistry.CommonPersonObjectController;
import org.ei.opensrp.domain.form.FieldOverrides;
import org.ei.opensrp.provider.SmartRegisterClientsProvider;
import org.ei.opensrp.vaccinator.R;
import org.ei.opensrp.vaccinator.db.Client;
import org.ei.opensrp.vaccinator.field.FieldMonitorDailyDetailActivity;
import org.ei.opensrp.vaccinator.field.FieldMonitorMonthlyDetailActivity;
import org.ei.opensrp.vaccinator.field.FieldMonitorSmartClientsProvider;
import org.ei.opensrp.vaccinator.field.StockDailyServiceModeOption;
import org.ei.opensrp.vaccinator.field.StockMonthlyServiceModeOption;
import org.ei.opensrp.vaccinator.woman.DetailActivity;
import org.ei.opensrp.view.activity.SecuredNativeSmartRegisterActivity;
import org.ei.opensrp.view.contract.SmartRegisterClients;
import org.ei.opensrp.view.controller.FormController;
import org.ei.opensrp.view.dialog.DialogOption;
import org.ei.opensrp.view.dialog.FilterOption;
import org.ei.opensrp.view.dialog.ServiceModeOption;
import org.ei.opensrp.view.dialog.SortOption;
import org.joda.time.DateTime;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import util.Utils;

/**
 * Created by Safwan on 2/15/2016.
 */
public class FieldMonitorRegisterFragment extends SmartRegisterFragment {

    private FormController formController1;
    private final ClientActionHandler clientActionHandler = new ClientActionHandler();
    private CommonPersonObjectController controller;

    public FieldMonitorRegisterFragment(FormController formController) {
        super(formController);
        this.formController1 = formController;
    }

    @Override
    protected SecuredNativeSmartRegisterActivity.DefaultOptionsProvider getDefaultOptionsProvider() {
        return new SecuredNativeSmartRegisterActivity.DefaultOptionsProvider() {

            @Override
            public ServiceModeOption serviceMode() {
                return new StockMonthlyServiceModeOption(clientsProvider());
            }

            @Override
            public FilterOption villageFilter() {
                return new ReportFilterOption("monthly");
            }

            @Override
            public SortOption sortOption(){ return new DateSort(DateSort.ByColumnAndByDetails.byColumn, "Reporting Period", "date"); }

            @Override
            public String nameInShortFormForTitle() {
                return Context.getInstance().getStringResource(R.string.stock_register_title);
            }
        };
    }

    @Override
    protected SecuredNativeSmartRegisterActivity.NavBarOptionsProvider getNavBarOptionsProvider() {
        return new SecuredNativeSmartRegisterActivity.NavBarOptionsProvider() {

            @Override
            public DialogOption[] filterOptions() {
                return new DialogOption[]{};
            }

            @Override
            public DialogOption[] serviceModeOptions() {
                return new DialogOption[]{
                        new StockMonthlyServiceModeOption(clientsProvider(FieldMonitorSmartClientsProvider.ByMonthByDay.ByMonth)),
                        new StockDailyServiceModeOption(clientsProvider(FieldMonitorSmartClientsProvider.ByMonthByDay.ByDay))
                };
            }

            @Override
            public DialogOption[] sortingOptions() {
                return new DialogOption[]{
                        new DateSort(DateSort.ByColumnAndByDetails.byDetails, "Reporting Period", "date")};
            }

            @Override
            public String searchHint() {
                return Context.getInstance().getStringResource(R.string.str_field_search_hint);
            }
        };
    }

    @Override
    protected void onServiceModeSelection(ServiceModeOption serviceModeOption, View view) {
        if(serviceModeOption.name().toLowerCase().contains("month")){
            setCurrentSearchFilter(new ReportFilterOption("monthly"));
        }
        else{
            setCurrentSearchFilter(new ReportFilterOption("daily"));
        }
        super.onServiceModeSelection(serviceModeOption, view);
    }

    @Override
    protected SmartRegisterClientsProvider clientsProvider() {
        FieldMonitorSmartClientsProvider clientProvider;
        if (getCurrentServiceModeOption() == null || getCurrentServiceModeOption().name().toLowerCase().contains("month")) {
            clientProvider = new FieldMonitorSmartClientsProvider(
                    getActivity().getApplicationContext(), clientActionHandler, controller, context.alertService(), FieldMonitorSmartClientsProvider.ByMonthByDay.ByMonth, context, this);
        }
        else {
            clientProvider = new FieldMonitorSmartClientsProvider(
                    getActivity().getApplicationContext(), clientActionHandler, controller, context.alertService(), FieldMonitorSmartClientsProvider.ByMonthByDay.ByDay, context, this);
        }
        return clientProvider;
    }

    private SmartRegisterClientsProvider clientsProvider(FieldMonitorSmartClientsProvider.ByMonthByDay type) {
        FieldMonitorSmartClientsProvider clientProvider;
        if (type.equals(FieldMonitorSmartClientsProvider.ByMonthByDay.ByMonth)) {
            clientProvider = new FieldMonitorSmartClientsProvider(
                    getActivity().getApplicationContext(), clientActionHandler, controller, context.alertService(), FieldMonitorSmartClientsProvider.ByMonthByDay.ByMonth, context, this);
        }
        else {
            clientProvider = new FieldMonitorSmartClientsProvider(
                    getActivity().getApplicationContext(), clientActionHandler, controller, context.alertService(), FieldMonitorSmartClientsProvider.ByMonthByDay.ByDay, context, this);
        }
        return clientProvider;
    }

    @Override
    protected void onInitialization() {
        if (getCurrentServiceModeOption() == null || getCurrentServiceModeOption().name().toLowerCase().contains("month")) {
            controller = new CommonPersonObjectController(context.allCommonsRepositoryobjects("stock"),
                    context.allBeneficiaries(), context.listCache(),
                    context.personObjectClientsCache(), "date", "stock", "report", "monthly",
                    CommonPersonObjectController.ByColumnAndByDetails.byColumn, "date",
                    CommonPersonObjectController.ByColumnAndByDetails.byColumn);
        } else {
            controller = new CommonPersonObjectController(context.allCommonsRepositoryobjects("stock"),
                    context.allBeneficiaries(), context.listCache(),
                    context.personObjectClientsCache(), "date", "stock", "report", "daily",
                    CommonPersonObjectController.ByColumnAndByDetails.byColumn, "date",
                    CommonPersonObjectController.ByColumnAndByDetails.byColumn);
        }

        ((TextView)mView.findViewById(org.ei.opensrp.R.id.txt_title_label)).setText(getRegisterLabel());

        mView.findViewById(org.ei.opensrp.R.id.btn_report_month).setVisibility(View.GONE);

        mView.findViewById(org.ei.opensrp.R.id.filter_selection).setVisibility(View.GONE);

        ((TextView)mView.findViewById(org.ei.opensrp.R.id.statusbar_today)).setText("Today : " + Utils.convertDateFormat(DateTime.now()));
    }

    @Override
    protected void startRegistration() {
        HashMap<String, String> overrides = new HashMap<>();
        overrides.putAll(providerOverrides());
        formController1.startFormActivity(getRegistrationForm(overrides), null, new FieldOverrides(new JSONObject(overrides).toString()).getJSONString());
    }

    @Override
    protected String getRegisterLabel() {
        return "Stock Register";
    }

    @Override
    protected String getRegistrationForm(HashMap<String, String> overridemap) {
        return "vaccine_stock_position";
    }

    @Override
    protected String getOAFollowupForm(Client client, HashMap<String, String> overridemap) {
        return null;
    }

    @Override
    protected Map<String, String> customFieldOverrides() {
        return null;
    }

    @Override
    protected void onResumption() {
        super.onResumption();
        getDefaultOptionsProvider();
        updateSearchView();
    }//end of method

    @Override
    protected void onCreation() {    }

    private class ClientActionHandler implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.stock_detail_holder:
                    if (getCurrentServiceModeOption().name().toLowerCase().contains("month")) {
                        DetailActivity.startDetailActivity(getActivity(), (CommonPersonObjectClient) view.getTag(R.id.client_details_tag), FieldMonitorMonthlyDetailActivity.class);
                    }
                    else {
                        Intent intent = new Intent(getActivity(), FieldMonitorDailyDetailActivity.class);
                    }

                    getActivity().finish();

                break;
            }
        }
    }


    private SmartRegisterClients getFilteredClients(String filterString) {
        setCurrentSearchFilter(new BasicSearchOption(filterString));
        SmartRegisterClients filteredClients = getClientsAdapter().getListItemProvider()
                .updateClients(getCurrentVillageFilter(), getCurrentServiceModeOption(),
                        getCurrentSearchFilter(), getCurrentSortOption());
        return filteredClients;
    }//end of method
}
