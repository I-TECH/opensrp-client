package org.ei.drishti.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View;
import android.widget.LinearLayout;
import com.markupartist.android.widget.ActionBar;
import org.ei.drishti.R;

import java.util.ArrayList;
import java.util.List;

public class DialogAction<T> implements ActionBar.Action {
    private AlertDialog.Builder builder;
    private AlertDialog dialog;
    private T[] options;
    private int icon;
    private final Activity context;
    private View viewOfLatestAction;

    public DialogAction(Activity context, int icon, String title, T... options) {
        this.options = options;
        this.icon = icon;
        this.context = context;
        builder = new AlertDialog.Builder(this.context);
        builder.setTitle(title);
    }

    public int getDrawable() {
        return icon;
    }

    public void performAction(View view) {
        dialog.show();
    }

    public void setOnSelectionChangedListener(final OnSelectionChangeListener<T> onSelectionChangeListener) {
        LinearLayout actionItemsLayout = (LinearLayout) context.findViewById(R.id.actionbar_actions);
        viewOfLatestAction = actionItemsLayout.getChildAt(actionItemsLayout.getChildCount() - 1);

        builder.setSingleChoiceItems(buildDisplayItemsFrom(options), 0, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                onSelectionChangeListener.selectionChanged(viewOfLatestAction, options[item]);
                dialog.dismiss();
            }
        });
        dialog = builder.create();
    }

    private String[] buildDisplayItemsFrom(T[] options) {
        List<String> displayItems = new ArrayList<String>();
        for (T option : options) {
            displayItems.add(option.toString());
        }
        return displayItems.toArray(new String[options.length]);
    }
}
