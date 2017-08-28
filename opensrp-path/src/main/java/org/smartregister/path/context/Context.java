package org.smartregister.path.context;


import org.smartregister.path.sync.SaveRelationshipTypesTask;

/**
 * Created by amosl on 8/22/17.
 */

public class Context extends org.smartregister.Context {

    private static Context context = new Context();

    private AllSettings allSettings;

    private SaveRelationshipTypesTask saveRelationshipTypesTask;

    public static Context getInstance() {
        if (context == null){
            context = new Context();
        }
        return context;
    }

    public AllSettings allSettings() {
        initRepository();
        if (allSettings == null) {
            allSettings = new AllSettings(allSharedPreferences(), settingsRepository());
        }
        return allSettings;
    }

    private SaveRelationshipTypesTask saveRelationshipTypesTask() {

        if (saveRelationshipTypesTask == null) {
            saveRelationshipTypesTask = new SaveRelationshipTypesTask(allSettings());
        }
        return saveRelationshipTypesTask;
    }

}
