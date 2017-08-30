package com.bmc.truesight.meter.plugin.remedy;

import java.util.Arrays;

/**
 * @author Santosh Patil
 * @author vitiwari
 */
public class RemedyPluginConfigurationItem {

    private String hostName;
    private String port;
    private String userName;
    private String password;
    private Long pollInterval;
    private String requestType;
    private String appId;
    private String fields[];

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public String[] getFields() {
        return fields;
    }

    public void setFields(String[] fields) {
        this.fields = fields;
    }

    public RemedyPluginConfigurationItem() {
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Long getPollInterval() {
        return pollInterval;
    }

    @Override
    public String toString() {
        return "{hostName=" + hostName + ", port=" + port + ", userName=" + userName
                + ", password=" + password + ", pollInterval=" + pollInterval + ", requestType=" + requestType
                + ", fileds=" + Arrays.toString(fields) + "}";
    }

    public void setPollInterval(Long pollInterval) {
        this.pollInterval = pollInterval;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }
}
