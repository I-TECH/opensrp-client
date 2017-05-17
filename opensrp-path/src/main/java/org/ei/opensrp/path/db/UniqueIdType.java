package org.ei.opensrp.path.db;

/**
 * Created by amosl on 5/15/17.
 */

public enum UniqueIdType {
    OPENMRS_ID(1), KIP_ID(2);

    private int value;

    UniqueIdType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static UniqueIdType fromInteger(int x) {
        switch (x) {
            case 1:
                return OPENMRS_ID;
            case 2:
                return KIP_ID;
        }
        return null;
    }
}
