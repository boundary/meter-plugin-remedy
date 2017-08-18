package com.bmc.truesight.meter.plugin.remedy.beans;

import com.bmc.truesight.saas.remedy.integration.beans.Result;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RpcResult {

	private Result result;
	private Error error;
	
	public Result getResult() {
		return result;
	}

	public void setResult(Result result) {
		this.result = result;
	}

	public Error getError() {
		return error;
	}

	public void setError(Error error) {
		this.error = error;
	}
}
