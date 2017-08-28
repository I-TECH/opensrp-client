package org.smartregister.path.context;

import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.repository.SettingsRepository;

/**
 * Created by amosl on 8/22/17.
 */

public class AllSettings extends org.smartregister.repository.AllSettings {

    private static final String RELATIONSHIP_TYPES = "relationshipTypes";

    public AllSettings(AllSharedPreferences preferences, SettingsRepository settingsRepository) {
        super(preferences, settingsRepository);
    }

    public String fetchRelationshipTypes() {
        return settingsRepository.querySetting(RELATIONSHIP_TYPES, "");
    }

    public void saveRelationshipTypes(String relationshipTypes) {
        settingsRepository.updateSetting(RELATIONSHIP_TYPES, relationshipTypes);
    }
}
