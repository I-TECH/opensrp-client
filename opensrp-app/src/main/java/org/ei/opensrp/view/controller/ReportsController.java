package org.ei.opensrp.view.controller;

import android.content.Context;
import android.content.Intent;
import org.ei.opensrp.AllConstants;
import org.ei.opensrp.view.activity.ReportIndicatorListViewActivity;

public class ReportsController {
    private Context context;

    public ReportsController(Context context) {
        this.context = context;
    }

    public void startIndicatorListViewFor(String reportCategory) {
        Intent intent = new Intent(context.getApplicationContext(), ReportIndicatorListViewActivity.class);
        intent.putExtra(AllConstants.REPORT_CATEGORY, reportCategory);
        context.startActivity(intent);
    }
}
