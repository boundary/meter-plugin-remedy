package com.bmc.truesight.meter.plugin.remedy;

import java.util.Arrays;

/**
 * @author Santosh Patil
 * @author vitiwari
 */
public class RemedyPluginConfigurationItem {

    private String hostName;
    private Integer port;
    private String userName;
    private String password;
    private int pollInterval;
    private String requestType;
    private String fileds[];

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public String[] getFileds() {
        return fileds;
    }

    public void setFileds(String[] fileds) {
        this.fileds = fileds;
    }

    public RemedyPluginConfigurationItem() {
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public Integer getPort() {
        return port;
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

    public int getPollInterval() {
        return pollInterval;
    }

    @Override
	public String toString() {
		return "{hostName=" + hostName + ", port=" + port + ", userName=" + userName
				+ ", password=" + password + ", pollInterval=" + pollInterval + ", requestType=" + requestType
				+ ", fileds=" + Arrays.toString(fileds) + "}";
	}

	public void setPollInterval(int pollInterval) {
        this.pollInterval = pollInterval;
    }
}
