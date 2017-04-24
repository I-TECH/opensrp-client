package org.ei.opensrp.path.provider;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.ei.opensrp.commonregistry.CommonFtsObject;
import org.ei.opensrp.commonregistry.CommonPersonObject;
import org.ei.opensrp.commonregistry.CommonPersonObjectClient;
import org.ei.opensrp.commonregistry.CommonRepository;
import org.ei.opensrp.path.R;
import org.ei.opensrp.path.fragment.AdvancedSearchFragment;
import org.ei.opensrp.path.repository.VaccineRepository;
import org.ei.opensrp.path.repository.WeightRepository;
import org.ei.opensrp.service.AlertService;
import org.ei.opensrp.view.contract.SmartRegisterClient;

import java.util.ArrayList;
import java.util.HashMap;

import static util.Utils.getValue;

/**
 * Created by Keyman on 06-Apr-17.
 */
public class AdvancedSearchClientsProvider extends ChildSmartClientsProvider {
    private final Context context;
    private final View.OnClickListener onClickListener;
    private CommonRepository commonRepository;

    public AdvancedSearchClientsProvider(Context context, View.OnClickListener onClickListener,
                                         AlertService alertService, VaccineRepository vaccineRepository, WeightRepository weightRepository, CommonRepository commonRepository) {
        super(context, onClickListener, alertService, vaccineRepository, weightRepository);
        this.onClickListener = onClickListener;
        this.context = context;
        this.commonRepository = commonRepository;

    }

    public void getView(Cursor cursor, SmartRegisterClient client, View convertView) {
        super.getView(client, convertView);

        CommonPersonObjectClient pc = (CommonPersonObjectClient) client;

        //TODO check if record exists ...
        if (cursor instanceof AdvancedSearchFragment.AdvancedMatrixCursor) {
            if (commonRepository != null) {
                String ftsSearchTable = CommonFtsObject.searchTableName(commonRepository.TABLE_NAME);
                ArrayList<HashMap<String, String>> mapList = commonRepository.rawQuery(String.format("SELECT " + CommonFtsObject.idColumn + " FROM " + ftsSearchTable + " WHERE  " + CommonFtsObject.idColumn + " = '%s'", pc.entityId()));

                if (mapList.isEmpty()) { //Out of area -- doesn't exist in local database
                    TextView recordWeightText = (TextView) convertView.findViewById(R.id.record_weight_text);
                    recordWeightText.setText("Record\nservice");

                    String zeirId = getValue(pc.getColumnmaps(), "zeir_id", false);

                    View recordWeight = convertView.findViewById(R.id.record_weight);
                    recordWeight.setBackground(context.getResources().getDrawable(R.drawable.record_weight_bg));
                    recordWeight.setTag(zeirId);
                    recordWeight.setClickable(true);
                    recordWeight.setEnabled(true);
                    recordWeight.setOnClickListener(onClickListener);

                    Button recordVaccination = (Button) convertView.findViewById(R.id.record_vaccination);
                    recordVaccination.setText("Move to my\ncatchment");
                    recordVaccination.setTag(pc.entityId());
                    recordVaccination.setClickable(true);
                    recordVaccination.setEnabled(true);
                    recordVaccination.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                    recordVaccination.setTextColor(context.getResources().getColor(R.color.text_black));
                    recordVaccination.setBackground(context.getResources().getDrawable(R.drawable.record_weight_bg));
                    recordVaccination.setOnClickListener(onClickListener);
                }
            }
        }

    }

    @Deprecated
    @Override
    public void getView(SmartRegisterClient client, View convertView) {
        super.getView(client, convertView);
    }
}