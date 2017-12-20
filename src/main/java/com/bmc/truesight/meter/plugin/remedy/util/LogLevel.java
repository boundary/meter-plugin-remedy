package com.bmc.truesight.meter.plugin.remedy.util;

public enum LogLevel {
    CRITICAL(5), ERROR(4), INFO(3), WARN(2), DEBUG(1);

    private LogLevel(int loglevel) {
        this.loglevel = loglevel;
    }

    private final int loglevel;

    public int getLevel() {
        return loglevel;
    }

    public boolean isEnabled(int configLevel) {
        if (this.getLevel() >= configLevel) {
            return true;
        }
        return false;
    }

}
