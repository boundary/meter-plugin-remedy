package com.bmc.truesight.meter.plugin.remedy;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bmc.arsys.api.ARServerUser;
import com.bmc.arsys.api.OutputInteger;
import com.bmc.truesight.meter.plugin.remedy.util.Constants;
import com.bmc.truesight.meter.plugin.remedy.util.Util;
import com.bmc.truesight.saas.remedy.integration.ARServerForm;
import com.bmc.truesight.saas.remedy.integration.RemedyReader;
import com.bmc.truesight.saas.remedy.integration.adapter.RemedyEntryEventAdapter;
import com.bmc.truesight.saas.remedy.integration.beans.TSIEvent;
import com.bmc.truesight.saas.remedy.integration.beans.Template;
import com.bmc.truesight.saas.remedy.integration.impl.GenericRemedyReader;
import com.boundary.plugin.sdk.Collector;
import com.boundary.plugin.sdk.Event;
import com.boundary.plugin.sdk.EventSinkAPI;
import com.boundary.plugin.sdk.EventSinkStandardOutput;
import com.boundary.plugin.sdk.Measurement;
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
        while (true) {
            try {
                RemedyReader reader = new GenericRemedyReader();
                ARServerUser arServerContext = reader.createARServerContext(config.getHostName(), Integer.getInteger(config.getPort()), config.getUserName(), config.getPassword());
                try {
                    reader.login(arServerContext);
                    int chunkSize = template.getConfig().getChunkSize();
                    int startFrom = 0;
                    int iteration = 1;
                    int totalRecordsRead = 0;
                    OutputInteger nMatches = new OutputInteger();
                    boolean readNext = true;
                    Long currentMili = Calendar.getInstance().getTimeInMillis();
                    Long pastMili = currentMili - (config.getPollInterval() * 60 * 1000);
                    template.getConfig().setStartDateTime(new Date(pastMili));
                    template.getConfig().setEndDateTime(new Date(currentMili));
                    boolean exceededMaxServerEntries = false;
                    System.err.println("Starting event reading & ingestion to tsi for (DateTime:" + template.getConfig().getStartDateTime() + " to DateTime:" + template.getConfig().getEndDateTime() + ")");
                    while (readNext) {
                        System.err.println("Iteration : " + iteration);
                        List<TSIEvent> eventList = reader.readRemedyTickets(arServerContext, arServerForm, template, startFrom, chunkSize, nMatches, remedyEntryEventAdapter);
                        exceededMaxServerEntries = reader.exceededMaxServerEntries(arServerContext);
                        totalRecordsRead += eventList.size();
                        if (eventList.size() < chunkSize && totalRecordsRead < nMatches.intValue() && exceededMaxServerEntries) {
                            System.err.println(" Request Sent to remedy (startFrom:" + startFrom + ",chunkSize:" + chunkSize + "), Response Got(RecordsRead:" + eventList.size() + ", totalRecordsRead:" + totalRecordsRead + ", recordsAvailable:" + nMatches.intValue() + ")");
                            System.err.println(" Based on exceededMaxServerEntries response as("+exceededMaxServerEntries+"), adjusting the chunk Size as " + eventList.size());
                            chunkSize = eventList.size();
                        } else if (eventList.size() <= chunkSize) {
                            System.err.println(" Request Sent to remedy (startFrom:" + startFrom + ", chunkSize:" + chunkSize + "), Response Got (RecordsRead:" + eventList.size() + ", totalRecordsRead:" + totalRecordsRead + ", recordsAvailable:" + nMatches.intValue() + ")");
                        }
                        
                        if (eventList.size() > 0) {
                        	List<String> eventsList=new ArrayList<>();
                            eventList.forEach(event -> {
                                Gson gson = new Gson();
                                String eventJson = gson.toJson(event, Object.class);
                                StringBuilder sendEventToTSI = new StringBuilder();
                                sendEventToTSI.append(Constants.REMEDY_PROXY_EVENT_JSON_START_STRING).append(eventJson).append(Constants.REMEDY_PROXY_EVENT_JSON_END_STRING);
                                //eventSinkAPI.emit(sendEventToTSI.toString());
                                eventsList.add(sendEventToTSI.toString());
                            });
                           int succ= eventSinkAPI.emit(eventsList);
                            System.err.println(succ + " Events successfuly ingested out of total " + nMatches + " events in iteration " + iteration);
                        } else {
                            System.err.println(eventList.size() + " Events found for the interval, DateTime:" + template.getConfig().getStartDateTime() + " to DateTime:" + template.getConfig().getEndDateTime());
                            eventSinkAPIstd.emit(Util.eventMeterTSI(Constants.REMEDY_PLUGIN_TITLE_MSG, Constants.REMEDY_IM_NO_DATA_AVAILABLE, Event.EventSeverity.INFO.toString()));
                        }

                        if (totalRecordsRead < nMatches.longValue() && (totalRecordsRead + chunkSize) > nMatches.longValue()) {
                            //assuming the long value would be in int range always
                        	chunkSize = (int) (nMatches.longValue() - totalRecordsRead);
                        }else if(totalRecordsRead >= nMatches.longValue()){
                        	readNext=false;
                        }
                        iteration++;
                        startFrom = totalRecordsRead;
                    }

                } catch (Exception e) {
                    System.err.println("Exception occure while fetching the data" + e.getMessage());
                    e.printStackTrace();
                    eventSinkAPIstd.emit(Util.eventMeterTSI(Constants.REMEDY_PLUGIN_TITLE_MSG, e.getMessage(), Event.EventSeverity.ERROR.toString()));
                } finally {
                    reader.logout(arServerContext);
                }
                Thread.sleep((config.getPollInterval() * 60 * 1000));
            } catch (InterruptedException ex) {
                eventSinkAPIstd.emit(Util.eventMeterTSI(Constants.REMEDY_PLUGIN_TITLE_MSG, ex.getMessage(), Event.EventSeverity.ERROR.toString()));
            }
        }
    }

	
}
