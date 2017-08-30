package com.bmc.truesight.meter.plugin.remedy.util;

/**
 *
 * @author Santosh Patil
 * @author vitiwari
 */
public class Constants {

    public static String REMEDY_PLUGIN_TITLE_MSG = "Remedy Plugin";
    public static final String REMEDY_PROXY_EVENT_JSON_START_STRING = "{ \"jsonrpc\": \"2.0\", \"id\":1, \"method\": \"proxy_event\", \"params\": {  \"data\":";
    public static final String REMEDY_PROXY_EVENT_JSON_END_STRING = " } }";
    public final static String REMEDY_IM_NO_DATA_AVAILABLE = "No data available for incident management";
    public final static String REMEDY_CM_NO_DATA_AVAILABLE = "No data available for change management";
    public static final int CHUNK_SIZE = 100;
    public static final String PROPERTY_INCIDENTNO = "IncidentNumber";
    public static final String PROPERTY_CHANGEID = "ChangeID";

}
