package org.ei.opensrp.domain;

import org.ei.opensrp.clientandeventmodel.Event;

import java.util.Date;
import java.util.HashMap;

/**
 * Created by keyman on 3/1/17.
 */
public class Weight {
    private static final String ZEIR_ID = "ZEIR_ID";
    Long id;
    String baseEntityId;
    String programClientId;
    Float kg;
    Date date;
    String anmId;
    String locationId;
    String syncStatus;
    Long updatedAt;

    public Weight() {
    }

    public Weight(Long id, String baseEntityId, Float kg, Date date, String anmId, String locationId, String syncStatus, Long updatedAt) {
        this.id = id;
        this.baseEntityId = baseEntityId;
        this.programClientId = null;
        this.kg = kg;
        this.date = date;
        this.anmId = anmId;
        this.locationId = locationId;
        this.syncStatus = syncStatus;
        this.updatedAt = updatedAt;
    }

    public Weight(Long id, String baseEntityId, String programClientId, Float kg, Date date, String anmId, String locationId, String syncStatus, Long updatedAt) {
        this.id = id;
        this.baseEntityId = baseEntityId;
        this.programClientId = programClientId;
        this.kg = kg;
        this.date = date;
        this.anmId = anmId;
        this.locationId = locationId;
        this.syncStatus = syncStatus;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBaseEntityId() {
        return baseEntityId;
    }

    public void setBaseEntityId(String baseEntityId) {
        this.baseEntityId = baseEntityId;
    }

    public String getProgramClientId() {
        return programClientId;
    }

    public void setProgramClientId(String programClientId) {
        this.programClientId = programClientId;
    }

    public HashMap<String, String> getIdentifiers() {
        HashMap<String, String> identifiers = new HashMap<>();
        identifiers.put(ZEIR_ID, programClientId);
        return identifiers;
    }

    public Float getKg() {
        return kg;
    }

    public void setKg(Float kg) {
        this.kg = kg;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getAnmId() {
        return anmId;
    }

    public void setAnmId(String anmId) {
        this.anmId = anmId;
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(String syncStatus) {
        this.syncStatus = syncStatus;
    }
}
