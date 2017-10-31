package com.bmc.truesight.meter.plugin.remedy.util;

public enum Metrics {

    REMEDY_HEARTBEAT("REMEDY_HEARTBEAT"),
    REMEDY_INGESTION_SUCCESS_COUNT("REMEDY_INGESTION_SUCCESS_COUNT"),
    REMEDY_INGESTION_FAILURE_COUNT("REMEDY_INGESTION_FAILURE_COUNT"),
    REMEDY_INGESTION_EXCEPTION("REMEDY_INGESTION_EXCEPTION");
    private final String name;

    Metrics(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Get the metric
     *
     * @return
     */
    public String getMetricName() {
        return name;
    }

}
