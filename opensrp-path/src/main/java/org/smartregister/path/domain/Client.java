package org.smartregister.path.domain;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by amosl on 8/22/17.
 */

public class Client extends org.smartregister.clientandeventmodel.Client {

    private Map<String, Map<String, String>> relationships;

    public Client(String baseEntityId) {
        super(baseEntityId);
    }

    public Map<String, String> getRelatives(String relationshipType) {
        if(relationships == null){
            relationships = new HashMap<>();
        }

        return relationships.get(relationshipType);
    }

    /**
     *
     * @param relationType "mother/guardian"
     * @param relativeEntityId uuid of relative
     * @param relationshipType "Parent/child, Doctor/patient" uuid from OpenMRS.
     */
    public void addRelationship(String relationType, String relativeEntityId, String relationshipType) {
        if(relationships == null){
            relationships = new HashMap<>();
        }

        Map<String, String> relatives = getRelatives(relationType);
        if(relatives == null){
            relatives = new HashMap<>();
        }

        relatives.put("relativeEntityId", relativeEntityId);
        relatives.put("relationshipType", relationshipType);

        relationships.put(relationType, relatives);
    }
}
