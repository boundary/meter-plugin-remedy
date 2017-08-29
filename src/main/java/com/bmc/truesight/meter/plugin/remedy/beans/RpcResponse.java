package com.bmc.truesight.meter.plugin.remedy.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RpcResponse {

    private String jsonrpc;
    private int id;
    private RpcResult result;

    public String getJsonrpc() {
        return jsonrpc;
    }

    public void setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public RpcResult getResult() {
        return result;
    }

    public void setResult(RpcResult result) {
        this.result = result;
    }

}
