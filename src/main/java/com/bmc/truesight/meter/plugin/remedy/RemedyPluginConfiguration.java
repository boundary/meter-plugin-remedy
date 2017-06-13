package com.bmc.truesight.meter.plugin.remedy;

import com.boundary.plugin.sdk.PluginConfiguration;
import java.util.ArrayList;

/**
 * @author Santosh Patil
 */
public class RemedyPluginConfiguration implements PluginConfiguration {

    private ArrayList<RemedyPluginConfigurationItem> items;

    @Override
    public String toString() {
        return "RemedyPluginConfiguration [items=" + items + "|||, pollInterval=" + pollInterval + "]";
    }

    private int pollInterval;

    public RemedyPluginConfiguration() {

    }

    public ArrayList<RemedyPluginConfigurationItem> getItems() {
        return items;
    }

    public void setItems(ArrayList<RemedyPluginConfigurationItem> items) {
        this.items = items;
    }

    public int getPollInterval() {
        return pollInterval;
    }

    public void setPollInterval(int pollInterval) {
        this.pollInterval = pollInterval;
    }

}
