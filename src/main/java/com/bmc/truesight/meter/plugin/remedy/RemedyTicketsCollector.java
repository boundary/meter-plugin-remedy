package com.bmc.truesight.meter.plugin.remedy;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bmc.arsys.api.ARServerUser;
import com.bmc.arsys.api.Field;
import com.bmc.arsys.api.OutputInteger;
import com.bmc.truesight.meter.plugin.remedy.beans.RpcResponse;
import com.bmc.truesight.meter.plugin.remedy.util.Constants;
import com.bmc.truesight.meter.plugin.remedy.util.LogLevel;
import com.bmc.truesight.meter.plugin.remedy.util.Metrics;
import com.bmc.truesight.meter.plugin.remedy.util.PluginLogger;
import com.bmc.truesight.meter.plugin.remedy.util.Util;
import com.bmc.truesight.saas.remedy.integration.ARServerForm;
import com.bmc.truesight.saas.remedy.integration.RemedyReader;
import com.bmc.truesight.saas.remedy.integration.adapter.RemedyEntryEventAdapter;
import com.bmc.truesight.saas.remedy.integration.beans.Error;
import com.bmc.truesight.saas.remedy.integration.beans.InvalidEvent;
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
import com.boundary.plugin.sdk.MeasurementSinkAPI;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

/**
 *
 * @author vitiwari
 */
public class RemedyTicketsCollector implements Collector {

    private PluginLogger LOG;
    private final RemedyPluginConfigurationItem config;
    private final Template template;
    private final Map<Integer, Field> fieldMap;
    private final ARServerForm arServerForm;
    private RemedyEntryEventAdapter remedyEntryEventAdapter;

    public RemedyTicketsCollector(RemedyPluginConfigurationItem config, Template template, Map<Integer, Field> fieldMap, ARServerForm arServerForm) {
        this.config = config;
        this.template = template;
        this.fieldMap = fieldMap;
        Util.updateConfiguration(this.template, config);
        this.arServerForm = arServerForm;
        remedyEntryEventAdapter = new RemedyEntryEventAdapter(fieldMap);
        LOG = new PluginLogger(config);
    }

    @Override
    public Measurement[] getMeasures() {
        return null;
    }

    @Override
    public void run() {
        LOG.debug("########### Instance started ##############");
        EventSinkAPI eventSinkAPI = new EventSinkAPI();
        MeasurementSinkAPI measurementSinkApi = new MeasurementSinkAPI();
        EventSinkStandardOutput eventSinkAPIstd = new EventSinkStandardOutput();
        String name = "";
        if (arServerForm == ARServerForm.INCIDENT_FORM) {
            name = "Incidents";
        } else {
            name = "Changes";
        }
        String source = template.getConfig().getRemedyHostName() + "_" + name;
        Long pollInterval = config.getPollInterval() * 60 * 1000;
        Long lastPoll = null;
        while (true) {
            LOG.debug("Instance Polling started ......");
            //sending heart beat
            measurementSinkApi.send(new Measurement(Metrics.REMEDY_HEARTBEAT.getMetricName(), Constants.MEASURE_YES, source));
            RemedyReader reader = new GenericRemedyReader();
            ARServerUser arServerContext = reader.createARServerContext(template.getConfig().getRemedyHostName(), template.getConfig().getRemedyPort(), template.getConfig().getRemedyUserName(), template.getConfig().getRemedyPassword());
            boolean isConnectionOpen = false;
            RemedyEventResponse remedyResponse = null;
            try {
                LOG.debug("Started login to AR Server");
                reader.login(arServerContext);
                LOG.debug("Login to AR Server completed");
                //Not Using template.getConfig().getChunkSize(); as for meter chunk size needed different
                int chunkSize = Constants.CHUNK_SIZE;
                LOG.debug("ChunkSize set as {0}", new Object[]{chunkSize});
                int startFrom = 0;
                int iteration = 1;
                int totalRecordsRead = 0;
                OutputInteger nMatches = new OutputInteger();
                boolean readNext = true;
                int totalFailure = 0;
                int totalSuccessful = 0;
                int validRecords = 0;
                Long currentMili = Calendar.getInstance().getTimeInMillis();
                Long pastMili = null;
                if (lastPoll == null) {
                    pastMili = currentMili - pollInterval;
                    LOG.debug("This is the first poll, setting the pastmili as currentMili - pollInterval = {0}", new Object[]{pastMili});
                } else {
                    LOG.debug("Last poll time was {0}", new Object[]{lastPoll});
                    pastMili = lastPoll;
                }
                lastPoll = currentMili;
                template.getConfig().setStartDateTime(new Date(pastMili));
                template.getConfig().setEndDateTime(new Date(currentMili));
                boolean exceededMaxServerEntries = false;
                List<InvalidEvent> droppedEvents = new ArrayList<>();
                LOG.info("Starting event reading & ingestion to tsi for (DateTime:" + Util.dateToString(template.getConfig().getStartDateTime()) + " to DateTime:" + Util.dateToString(template.getConfig().getEndDateTime()) + ")");
                isConnectionOpen = eventSinkAPI.openConnection();
                if (isConnectionOpen) {
                    LOG.info("JSON RPC Socket connection successful");
                } else {
                    LOG.error("JSON RPC Socket connection failed");
                }
                if (isConnectionOpen) {
                    Map<String, List<String>> errorsMap = new HashMap<>();
                    while (readNext) {
                        LOG.info("Iteration : " + iteration);
                        remedyResponse = reader.readRemedyTickets(arServerContext, arServerForm, template, startFrom, chunkSize, nMatches, remedyEntryEventAdapter);
                        fixCreatedAtTimestamp(remedyResponse, pastMili);
                        exceededMaxServerEntries = reader.exceededMaxServerEntries(arServerContext);
                        int recordsCount = remedyResponse.getValidEventList().size() + remedyResponse.getInvalidEventList().size();
                        totalRecordsRead += recordsCount;
                        validRecords += remedyResponse.getValidEventList().size();
                        if (recordsCount < chunkSize && totalRecordsRead < nMatches.intValue() && exceededMaxServerEntries) {
                            LOG.info(" Request to remedy (Start From:" + startFrom + ",Chunk Size:" + chunkSize + "), Response (Valid Events:" + remedyResponse.getValidEventList().size() + ", Invalid Events:" + remedyResponse.getInvalidEventList().size() + ", Total Records Read: (" + totalRecordsRead + "/" + nMatches.intValue() + ")");
                            LOG.info(" Based on exceededMaxServerEntries response as(" + exceededMaxServerEntries + "), adjusting the chunk Size as " + recordsCount);
                            chunkSize = recordsCount;
                        } else if (recordsCount <= chunkSize) {
                            LOG.info(" Request to remedy (Start From:" + startFrom + ",Chunk Size:" + chunkSize + "), Response (Valid Events:" + remedyResponse.getValidEventList().size() + ", Invalid Events:" + remedyResponse.getInvalidEventList().size() + ", Total Records Read: (" + totalRecordsRead + "/" + nMatches.intValue() + ")");
                        }
                        if (totalRecordsRead < nMatches.longValue() && (totalRecordsRead + chunkSize) > nMatches.longValue()) {
                            //assuming the long value would be in int range always
                            chunkSize = ((int) (nMatches.longValue()) - totalRecordsRead);
                        } else if (totalRecordsRead >= nMatches.longValue()) {
                            readNext = false;
                        }

                        if (recordsCount == 0) {
                            break;
                        }

                        iteration++;
                        startFrom = totalRecordsRead;

                        if (remedyResponse.getInvalidEventList().size() > 0) {
                            List<String> eventIds = new ArrayList<>();
                            if (arServerForm == ARServerForm.INCIDENT_FORM) {
                                for (InvalidEvent event : remedyResponse.getInvalidEventList()) {
                                    eventIds.add(event.getInvalidEvent().getProperties().get(com.bmc.truesight.saas.remedy.integration.util.Constants.PROPERTY_INCIDENT_NO));
                                }
                            } else {
                                for (InvalidEvent event : remedyResponse.getInvalidEventList()) {
                                    eventIds.add(event.getInvalidEvent().getProperties().get(com.bmc.truesight.saas.remedy.integration.util.Constants.PROPERTY_CHANGE_ID));
                                }
                            }
                            droppedEvents.addAll(remedyResponse.getInvalidEventList());
                            LOG.debug("{0} Events are dropped as they are large in size.", new Object[]{remedyResponse.getInvalidEventList().size()});
                        }
                        List<TSIEvent> eventsList = remedyResponse.getValidEventList();
                        if (eventsList.size() > 0) {
                            Gson gson = new Gson();
                            String eventJson = gson.toJson(eventsList);
                            StringBuilder sendEventToTSI = new StringBuilder();
                            sendEventToTSI.append(Constants.REMEDY_PROXY_EVENT_JSON_START_STRING).append(eventJson).append(Constants.REMEDY_PROXY_EVENT_JSON_END_STRING);
                            LOG.debug("{0} Events are about to be sent to Meter JSON RPC channel.", new Object[]{eventsList.size()});
                            String resultJson = eventSinkAPI.emit(sendEventToTSI.toString());
                            LOG.debug("Meter Response: ", new Object[]{resultJson});
                            ObjectMapper mapper = new ObjectMapper();
                            RpcResponse rpcResponse = mapper.readValue(resultJson, RpcResponse.class);
                            if (rpcResponse.getResult() == null) {
                                totalFailure += eventsList.size();
                                addEventIdsToErrorMap(eventsList, errorsMap, "Event ingestion failed with no response from meter");
                            } else if (rpcResponse.getResult().getError() != null) {
                                totalFailure += eventsList.size();
                                String msg = "Event ingestion failed with status code " + rpcResponse.getResult().getError().getCode() + "," + rpcResponse.getResult().getError().getMessage();
                                addEventIdsToErrorMap(eventsList, errorsMap, msg);
                            } else {
                                Result result = rpcResponse.getResult().getResult();
                                if (result.getAccepted() != null) {
                                    totalSuccessful += result.getAccepted().size();
                                }
                                if (result.getErrors() != null) {
                                    totalFailure += result.getErrors().size();
                                }
                                if (result.getSuccess() == Success.PARTIAL) {
                                    for (Error error : result.getErrors()) {
                                        String id = "";
                                        String msg = error.getMessage().trim();
                                        if (arServerForm == ARServerForm.INCIDENT_FORM) {
                                            id = eventsList.get(error.getIndex()).getProperties().get(com.bmc.truesight.saas.remedy.integration.util.Constants.PROPERTY_INCIDENT_NO);
                                        } else {
                                            id = eventsList.get(error.getIndex()).getProperties().get(com.bmc.truesight.saas.remedy.integration.util.Constants.PROPERTY_CHANGE_ID);
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
                            LOG.info(eventsList.size() + " Events found for the interval, DateTime:" + Util.dateToString(template.getConfig().getStartDateTime()) + " to DateTime:" + Util.dateToString(template.getConfig().getEndDateTime()));
                            eventSinkAPIstd.emit(Util.eventMeterTSI(Constants.REMEDY_PLUGIN_TITLE_MSG, Constants.REMEDY_IM_NO_DATA_AVAILABLE, Event.EventSeverity.INFO.toString()));
                        }

                    }//each chunk iteration

                    LOG.info("____________" + name + " ingestion to truesight intelligence final status: Remedy Records = " + totalRecordsRead + ", Valid Records Sent = " + validRecords + ", Successful = " + totalSuccessful + " , Failure = " + totalFailure + " ______");
                    if (droppedEvents.size() > 0) {
                        LOG.error("______Following " + droppedEvents.size() + " events were invalid & dropped.");
                        if (arServerForm == ARServerForm.INCIDENT_FORM) {
                            droppedEvents.forEach(invalidEvent -> {
                                LOG.error(com.bmc.truesight.saas.remedy.integration.util.Constants.PROPERTY_INCIDENT_NO + ": " + invalidEvent.getInvalidEvent().getProperties().get(com.bmc.truesight.saas.remedy.integration.util.Constants.PROPERTY_INCIDENT_NO) + " , Event Size :" + invalidEvent.getEventSize() + ", Field with max size  : " + invalidEvent.getMaxSizePropertyName() + ", Field Size: " + invalidEvent.getPropertySize());
                            });
                        } else {
                            droppedEvents.forEach(invalidEvent -> {
                                LOG.error(com.bmc.truesight.saas.remedy.integration.util.Constants.PROPERTY_CHANGE_ID + ": " + invalidEvent.getInvalidEvent().getProperties().get(com.bmc.truesight.saas.remedy.integration.util.Constants.PROPERTY_CHANGE_ID) + " , Event Size :" + invalidEvent.getEventSize() + ", Field with max size  : " + invalidEvent.getMaxSizePropertyName() + ", Field Size: " + invalidEvent.getPropertySize());
                            });
                        }
                        measurementSinkApi.send(new Measurement(Metrics.REMEDY_INVALID_EVENTS_COUNT.getMetricName(), droppedEvents.size(), source));
                    } else {
                        measurementSinkApi.send(new Measurement(Metrics.REMEDY_INVALID_EVENTS_COUNT.getMetricName(), 0, source));
                    }

                    if (totalFailure > 0) {
                        LOG.error("______ Event Count, Failure reason , [Reference Id(s)] ______");
                        errorsMap.keySet().forEach(msg -> {
                            LOG.error("______ " + errorsMap.get(msg).size() + "    , " + msg + ",  " + errorsMap.get(msg));
                        });
                    }
                    //sending success failure measurements
                    measurementSinkApi.send(new Measurement(Metrics.REMEDY_INGESTION_SUCCESS_COUNT.getMetricName(), totalSuccessful, source));
                    measurementSinkApi.send(new Measurement(Metrics.REMEDY_INGESTION_FAILURE_COUNT.getMetricName(), totalFailure, source));
                    measurementSinkApi.send(new Measurement(Metrics.REMEDY_INGESTION_EXCEPTION.getMetricName(), Constants.MEASURE_NO, source));
                }
            } catch (RemedyLoginFailedException e) {
                LOG.error("Remedy login failed :" + e.getMessage());
                measurementSinkApi.send(new Measurement(Metrics.REMEDY_INGESTION_EXCEPTION.getMetricName(), Constants.MEASURE_YES, source));
            } catch (RemedyReadFailedException e) {
                LOG.error("Reading records from Remedy Failed, Reason :" + e.getMessage());
                measurementSinkApi.send(new Measurement(Metrics.REMEDY_INGESTION_EXCEPTION.getMetricName(), Constants.MEASURE_YES, source));
            } catch (Exception e) {
                LOG.error("Exception occured while fetching the data" + e.getMessage());
                measurementSinkApi.send(new Measurement(Metrics.REMEDY_INGESTION_EXCEPTION.getMetricName(), Constants.MEASURE_YES, source));
                eventSinkAPIstd.emit(Util.eventMeterTSI(Constants.REMEDY_PLUGIN_TITLE_MSG, e.getMessage(), Event.EventSeverity.ERROR.toString()));
            } finally {
                reader.logout(arServerContext);
                if (isConnectionOpen) {
                    boolean isConnectionClosed = eventSinkAPI.closeConnection();
                    if (isConnectionClosed) {
                        LOG.info("JSON RPC Socket connection successfuly closed");
                    } else {
                        LOG.error("Closing JSON RPC Socket connection failed");
                    }
                }
            }
            Long now = Calendar.getInstance().getTimeInMillis();
            Long elapsedTime = now - lastPoll;
            LOG.debug("Elapsed time from last poll = {0} sec", new Object[]{elapsedTime / 1000});
            Long timeToSleep = null;
            if (elapsedTime > pollInterval) {
                timeToSleep = 0l;
                LOG.debug("Elapsed time is more than poll interval, Next poll will happen after {0} miliseconds, ie {1} min", new Object[]{timeToSleep, timeToSleep / (1000 * 60)});
            } else {
                timeToSleep = pollInterval - elapsedTime;
                LOG.debug("Elapsed time is less than poll interval, Next poll will happen after {0} miliseconds, ie {1} min", new Object[]{timeToSleep, timeToSleep / (1000 * 60)});
            }

            if (timeToSleep > 0) {
                try {
                    LOG.debug("Making the thread to Sleep for {0} milisecond, ie {1} min", new Object[]{timeToSleep, timeToSleep / (1000 * 60)});
                    TimeUnit.MILLISECONDS.sleep(timeToSleep);
                } catch (InterruptedException e) {
                    LOG.error("Interrupted Exception :" + e.getMessage());
                }
            }
        }//infinite while loop end
    }

    private void fixCreatedAtTimestamp(RemedyEventResponse remedyResponse, Long pastMili) {
        if (remedyResponse.getValidEventList().size() > 0) {
            remedyResponse.getValidEventList().forEach(event -> {
                String lastModDateString = event.getProperties().get(com.bmc.truesight.saas.remedy.integration.util.Constants.PROPERTY_LAST_MODIFIED_DATE);
                String createdDateString = event.getCreatedAt();
                try {
                    long lastModDate = Long.parseLong(lastModDateString);
                    long createdDate = Long.parseLong(createdDateString);
                    if (createdDate < pastMili) {
                        event.setCreatedAt(Long.toString(lastModDate));
                    }
                } catch (Exception ex) {
                    System.err.println("Cannot parse the last modified date, setting createdAt as modified date failed. Please review the mapping for last modified date.");
                }

            });
        }

    }

    private void addEventIdsToErrorMap(List<TSIEvent> eventsList, Map<String, List<String>> errorsMap, String msg) {
        eventsList.forEach(event -> {
            String id;
            if (arServerForm == ARServerForm.INCIDENT_FORM) {
                id = event.getProperties().get(com.bmc.truesight.saas.remedy.integration.util.Constants.PROPERTY_INCIDENT_NO);
            } else {
                id = event.getProperties().get(com.bmc.truesight.saas.remedy.integration.util.Constants.PROPERTY_CHANGE_ID);
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
