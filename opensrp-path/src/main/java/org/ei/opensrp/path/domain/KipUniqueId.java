package org.ei.opensrp.path.domain;

import org.ei.opensrp.domain.UniqueId;
import org.ei.opensrp.path.db.UniqueIdType;

import java.util.Date;

/**
 * Created by amosl on 5/15/17.
 */

public class KipUniqueId extends UniqueId {
    UniqueIdType uniqueIdType;

    public KipUniqueId() {
    }

    public KipUniqueId(String id, String openmrsId, String status, String usedBy, Date createdAt, UniqueIdType uniqueIdType) {
        super(id, openmrsId, status, usedBy, createdAt);
        this.uniqueIdType = uniqueIdType;
    }

    public UniqueIdType getUniqueIdType() {
        return uniqueIdType;
    }

    public void setUniqueIdType(UniqueIdType uniqueIdType) {
        this.uniqueIdType = uniqueIdType;
    }

    @Override
    public String toString() {
        return "KipUniqueId{" +
                "id='" + super.getId() + '\'' +
                ", openmrsId='" + super.getOpenmrsId() + '\'' +
                ", status='" + super.getStatus() + '\'' +
                ", usedBy='" + super.getUsedBy() + '\'' +
                ", createdAt=" + super.getCreatedAt() +
                ", updatedAt=" + super.getUpdatedAt() +
                ", uniqueIdType=" + getUniqueIdType().getValue() +
                '}';
    }
}
