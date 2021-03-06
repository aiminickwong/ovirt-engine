package org.ovirt.engine.core.common.businessentities.gluster;

public enum GeoRepSessionStatus {
    INITIALIZING("INITIALIZING"),
    NOTSTARTED("NOT STARTED"),
    ACTIVE("ACTIVE"),
    PASSIVE("PASSIVE"),
    STOPPED("STOPPED"),
    PARTIAL_FAULTY("PARTIAL FAULTY"),
    UNKNOWN("UNKNOWN"),
    FAULTY("FAULTY");

    private String statusMsg;

    private GeoRepSessionStatus(String status) {
        statusMsg = status;
    }
    public String value() {
        return statusMsg;
    }

    public static GeoRepSessionStatus from(String status) {
        for (GeoRepSessionStatus sessionStatus : values()) {
            if (sessionStatus.value().equalsIgnoreCase(status)) {
                return sessionStatus;
            }
        }
        return GeoRepSessionStatus.UNKNOWN;
    }
}
