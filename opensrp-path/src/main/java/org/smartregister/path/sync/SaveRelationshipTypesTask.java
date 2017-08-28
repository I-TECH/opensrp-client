package org.smartregister.path.sync;


import org.smartregister.path.context.AllSettings;
import org.smartregister.util.Log;
import org.smartregister.view.BackgroundAction;
import org.smartregister.view.LockingBackgroundTask;
import org.smartregister.view.ProgressIndicator;

/**
 * Created by amosl on 5/25/17.
 */

public class SaveRelationshipTypesTask {
    private final LockingBackgroundTask task;
    private AllSettings allSettings;

    public SaveRelationshipTypesTask(AllSettings allSettings) {
        this.allSettings = allSettings;
        task = new LockingBackgroundTask(new ProgressIndicator() {
            @Override
            public void setVisible() {
            }

            @Override
            public void setInvisible() {
                Log.logInfo("Successfully saved relationship types information");
            }
        });
    }

    public void save(final String relationshipTypes) {
        task.doActionInBackground(new BackgroundAction<String>() {
            @Override
            public String actionToDoInBackgroundThread() {
                allSettings.saveRelationshipTypes(relationshipTypes);
                return relationshipTypes;
            }

            @Override
            public void postExecuteInUIThread(String result) {
            }
        });
    }
}
