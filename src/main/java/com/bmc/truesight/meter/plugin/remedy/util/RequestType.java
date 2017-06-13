package com.bmc.truesight.meter.plugin.remedy.util;

/**
 *
 * @author Santosh Patil
 */
public enum RequestType {

    IM("incident tickets"),
    CM("change tickets");
    private final String name;

    RequestType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Get the Values
     *
     * @return
     */
    public String getValues() {
        return name;
    }

    public static RequestType findByName(String name) {
        for (RequestType s : RequestType.values()) {
            if (s.getValues().equals(name)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unable to find service: " + name);
    }

}
