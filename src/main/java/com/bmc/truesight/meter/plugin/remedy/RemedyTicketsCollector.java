package com.bmc.truesight.meter.plugin.remedy;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bmc.arsys.api.ARServerUser;
import com.bmc.arsys.api.OutputInteger;
import com.bmc.truesight.meter.plugin.remedy.beans.RpcResponse;
import com.bmc.truesight.meter.plugin.remedy.util.Constants;
import com.bmc.truesight.meter.plugin.remedy.util.Util;
import com.bmc.truesight.saas.remedy.integration.ARServerForm;
import com.bmc.truesight.saas.remedy.integration.RemedyReader;
import com.bmc.truesight.saas.remedy.integration.adapter.RemedyEntryEventAdapter;
import com.bmc.truesight.saas.remedy.integration.beans.Error;
import com.bmc.truesight.saas.remedy.integration.beans.RemedyEventResponse;
import com.bmc.truesight.saas.remedy.integration.beans.Result;
import com.bmc.truesight.saas.remedy.integration.beans.Success;
import com.bmc.truesight.saas.remedy.integration.beans.TSIEvent;
import com.bmc.truesight.saas.remedy.integration.beans.Template;
import com.bmc.truesight.saas.remedy.integration.exception.RemedyLoginFailedException;
import com.bmc.truesight.saas.remedy.integration.exception.RemedyReadFailedException;
import com.bmc.truesight.saas.remedy.integration.impl.GenericRemedyReader;
import com.boundary.plugin.sdk.Collector;
import com.boundary.plugin.sdk.Event;
import com.boundary.plugin.sdk.EventSinkAPI;
import com.boundary.plugin.sdk.EventSinkStandardOutput;
import com.boundary.plugin.sdk.Measurement;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

/**
 *
 * @author vitiwari
 */
public class RemedyTicketsCollector implements Collector {

    private static final Logger LOG = LoggerFactory.getLogger(RemedyPlugin.class);
    private final RemedyPluginConfigurationItem config;
    private final Template template;
    private final ARServerForm arServerForm;
    private RemedyEntryEventAdapter remedyEntryEventAdapter = new RemedyEntryEventAdapter();

    public RemedyTicketsCollector(RemedyPluginConfigurationItem config, Template template, ARServerForm arServerForm) {
        this.config = config;
        this.template = template;
        Util.updateConfiguration(this.template, config);
        this.arServerForm = arServerForm;
    }

    @Override
    public Measurement[] getMeasures() {
        return null;
    }

    @Override
    public void run() {
        EventSinkAPI eventSinkAPI = new EventSinkAPI();
        EventSinkStandardOutput eventSinkAPIstd = new EventSinkStandardOutput();
        String name="";
        if(arServerForm == ARServerForm.INCIDENT_FORM){
        	name = "Incidents";
        }else{
        	name = "Changes";
        }
        while (true) {
            RemedyReader reader = new GenericRemedyReader();
            ARServerUser arServerContext = reader.createARServerContext(template.getConfig().getRemedyHostName(), template.getConfig().getRemedyPort(), template.getConfig().getRemedyUserName(), template.getConfig().getRemedyPassword());
            boolean isConnectionOpen = false;
            RemedyEventResponse remedyResponse = null;
            try {
                reader.login(arServerContext);
                //Not Using template.getConfig().getChunkSize(); as for meter chunk size needed different
                int chunkSize = Constants.CHUNK_SIZE;
                int startFrom = 0;
                int iteration = 1;
                int totalRecordsRead = 0;
                OutputInteger nMatches = new OutputInteger();
                boolean readNext = true;
                int totalFailure = 0;
                int totalSuccessful = 0;
                int validRecords = 0;
                Long currentMili = Calendar.getInstance().getTimeInMillis();
                Long pastMili = currentMili - (config.getPollInterval() * 60 * 1000);
                template.getConfig().setStartDateTime(new Date(pastMili));
                template.getConfig().setEndDateTime(new Date(currentMili));
                boolean exceededMaxServerEntries = false;
                System.err.println("Starting event reading & ingestion to tsi for (DateTime:" + Util.dateToString(template.getConfig().getStartDateTime()) + " to DateTime:" + Util.dateToString(template.getConfig().getEndDateTime()) + ")");
                isConnectionOpen = eventSinkAPI.openConnection();
                if (isConnectionOpen) {
                    System.err.println("JSON RPC Socket connection successful");
                } else {
                    System.err.println("JSON RPC Socket connection failed");
                    break;
                }
                if (isConnectionOpen) {
                	Map<String,List<String>> errorsMap=new HashMap<>();
                    while (readNext) {
                        System.err.println("Iteration : " + iteration);
                        remedyResponse = reader.readRemedyTickets(arServerContext, arServerForm, template, startFrom, chunkSize, nMatches, remedyEntryEventAdapter);
                        exceededMaxServerEntries = reader.exceededMaxServerEntries(arServerContext);
                        int recordsCount = remedyResponse.getValidEventList().size() + remedyResponse.getLargeInvalidEventCount();
                        totalRecordsRead += recordsCount;
                        validRecords += remedyResponse.getValidEventList().size();
                        if (recordsCount < chunkSize && totalRecordsRead < nMatches.intValue() && exceededMaxServerEntries) {
                        	System.err.println(" Request to remedy (Start From:" + startFrom + ",Chunk Size:" + chunkSize + "), Response (Valid Events:" + remedyResponse.getValidEventList().size() + ", Invalid Events:" + remedyResponse.getLargeInvalidEventCount() + ", Total Records Read: (" + totalRecordsRead + "/" + nMatches.intValue() + ")");
                        	System.err.println(" Based on exceededMaxServerEntries response as(" + exceededMaxServerEntries + "), adjusting the chunk Size as " + recordsCount);
                            chunkSize = recordsCount;
                        } else if (recordsCount <= chunkSize) {
                        	System.err.println(" Request to remedy (Start From:" + startFrom + ",Chunk Size:" + chunkSize + "), Response (Valid Events:" + remedyResponse.getValidEventList().size() + ", Invalid Events:" + remedyResponse.getLargeInvalidEventCount() + ", Total Records Read: (" + totalRecordsRead + "/" + nMatches.intValue() + ")");
                        }
                        if (totalRecordsRead < nMatches.longValue() && (totalRecordsRead + chunkSize) > nMatches.longValue()) {
                            //assuming the long value would be in int range always
                            chunkSize = ((int) (nMatches.longValue()) - totalRecordsRead);
                        } else if (totalRecordsRead >= nMatches.longValue()) {
                            readNext = false;
                        }
                        iteration++;
                        startFrom = totalRecordsRead;
                        
                        if(remedyResponse.getLargeInvalidEventCount()>0){
                        	List<String> eventIds = new ArrayList<>();
                            if (arServerForm == ARServerForm.INCIDENT_FORM) {
                                for (TSIEvent event : remedyResponse.getInvalidEventList()) {
                                    eventIds.add(event.getProperties().get(Constants.PROPERTY_INCIDENTNO));
                                }
                            } else {
                                for (TSIEvent event : remedyResponse.getInvalidEventList()) {
                                    eventIds.add(event.getProperties().get(Constants.PROPERTY_CHANGEID));
                                }
                            }
                            System.err.println("Following "+name+" ids are larger than allowed limits ["+String.join(",", eventIds)+"]");

                        }
                        List<TSIEvent> eventsList = remedyResponse.getValidEventList();
                        if (eventsList.size() > 0) {
                            Gson gson = new Gson();
                            String eventJson = gson.toJson(eventsList);
                            StringBuilder sendEventToTSI = new StringBuilder();
                            sendEventToTSI.append(Constants.REMEDY_PROXY_EVENT_JSON_START_STRING).append(eventJson).append(Constants.REMEDY_PROXY_EVENT_JSON_END_STRING);
                            String resultJson = eventSinkAPI.emit(sendEventToTSI.toString());
                            ObjectMapper mapper = new ObjectMapper();
                            RpcResponse rpcResponse =mapper.readValue(resultJson, RpcResponse.class);
                            if(rpcResponse.getResult() == null){
                            	totalFailure += eventsList.size();
                            	addEventIdsToErrorMap(eventsList, errorsMap,"Event ingestion failed with no response from meter");
                            }else if(rpcResponse.getResult().getError() != null){
                            	totalFailure += eventsList.size();
                            	String msg = "Event ingestion failed with status code "+rpcResponse.getResult().getError().getCode()+","+rpcResponse.getResult().getError().getMessage();
                            	addEventIdsToErrorMap(eventsList, errorsMap, msg);
                            }else{
                            	Result result= rpcResponse.getResult().getResult();
                            	if (result.getAccepted() != null) {
                                    totalSuccessful += result.getAccepted().size();
                                }
                                if (result.getErrors() != null) {
                                    totalFailure += result.getErrors().size();
                                }
                                if (result.getSuccess() == Success.PARTIAL) {
                                    for(Error error:result.getErrors()){
                                    	String id = "";
                                        String msg = error.getMessage().trim();
                                        if(arServerForm == ARServerForm.INCIDENT_FORM){
                                    		id=eventsList.get(error.getIndex()).getProperties().get(Constants.PROPERTY_INCIDENTNO);
                                        }else{
                                        	id=eventsList.get(error.getIndex()).getProperties().get(Constants.PROPERTY_CHANGEID);
                                        }
                                        if (errorsMap.containsKey(msg)) {
                                        	List<String> errorsId = errorsMap.get(msg);
                                        	errorsId.add(id);
                                            errorsMap.put(msg, errorsId);
                                        } else {
                                        	List<String> errorsId = new ArrayList<String>();
                                        	errorsId.add(id);
                                            errorsMap.put(msg, errorsId);
                                        }
                                    }
                                }
                            }
                        } else {
                            System.err.println(eventsList.size() + " Events found for the interval, DateTime:" + Util.dateToString(template.getConfig().getStartDateTime()) + " to DateTime:" + Util.dateToString(template.getConfig().getEndDateTime()));
                            eventSinkAPIstd.emit(Util.eventMeterTSI(Constants.REMEDY_PLUGIN_TITLE_MSG, Constants.REMEDY_IM_NO_DATA_AVAILABLE, Event.EventSeverity.INFO.toString()));
                        }

                    }//each chunk iteration
                    
                    System.err.println("____________"+name+" ingestion to truesight intelligence final status: Remedy Records = "+nMatches.longValue()+", Valid Records Sent = "+validRecords+", Successful = "+totalSuccessful+" , Failure = "+totalFailure+" ______");
                    if (totalFailure > 0) {
                    	System.err.println("______ Event Count, Failure reason , [Reference Id(s)] ______");
                        errorsMap.keySet().forEach(msg -> {
                        	  System.err.println("______ "+errorsMap.get(msg).size()+"    , "+msg+",  "+errorsMap.get(msg));
                        });

                    }
                  }
            } catch (RemedyLoginFailedException e) {
                System.err.println("Remedy login failed :" + e.getMessage());
                break;
            } catch (RemedyReadFailedException e) {
                System.err.println("Reading records from Remedy Failed, Reason :" + e.getMessage());
                break;
            } catch (Exception e) {
                System.err.println("Exception occured while fetching the data" + e.getMessage());
                eventSinkAPIstd.emit(Util.eventMeterTSI(Constants.REMEDY_PLUGIN_TITLE_MSG, e.getMessage(), Event.EventSeverity.ERROR.toString()));
                break;
            } finally {
                reader.logout(arServerContext);
                if (isConnectionOpen) {
                    boolean isConnectionClosed = eventSinkAPI.closeConnection();
                    if (isConnectionClosed) {
                        System.err.println("JSON RPC Socket connection successfuly closed");
                    } else {
                        System.err.println("Closing JSON RPC Socket connection failed");
                    }
                }
            }
            try {
                Thread.sleep((config.getPollInterval() * 60 * 1000));
            } catch (InterruptedException ex) {
                System.err.println("Interrupted Exception :" + ex.getMessage());
                eventSinkAPIstd.emit(Util.eventMeterTSI(Constants.REMEDY_PLUGIN_TITLE_MSG, ex.getMessage(), Event.EventSeverity.ERROR.toString()));
            }
        }//infinite while loop end
    }

    private void addEventIdsToErrorMap(List<TSIEvent> eventsList, Map<String,List<String>> errorsMap,String msg){
    	eventsList.forEach(event->{
    		String id;
        	if(arServerForm == ARServerForm.INCIDENT_FORM){
        		id = event.getProperties().get(Constants.PROPERTY_INCIDENTNO);
            }else{
            	id = event.getProperties().get(Constants.PROPERTY_CHANGEID);
            }
            if (errorsMap.containsKey(msg)) {
            	List<String> errorsId = errorsMap.get(msg);
            	errorsId.add(id);
                errorsMap.put(msg, errorsId);
            } else {
            	List<String> errorsId = new ArrayList<String>();
            	errorsId.add(id);
                errorsMap.put(msg, errorsId);
            }
    	});
    	
    }
}
