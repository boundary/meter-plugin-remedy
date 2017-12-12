package com.bmc.truesight.meter.plugin.remedy;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bmc.arsys.api.ARServerUser;
import com.bmc.arsys.api.Field;
import com.bmc.truesight.meter.plugin.remedy.util.Constants;
import com.bmc.truesight.meter.plugin.remedy.util.PluginTemplateValidator;
import com.bmc.truesight.meter.plugin.remedy.util.RequestType;
import com.bmc.truesight.meter.plugin.remedy.util.Util;
import com.bmc.truesight.saas.remedy.integration.ARServerForm;
import com.bmc.truesight.saas.remedy.integration.RemedyReader;
import com.bmc.truesight.saas.remedy.integration.TemplateParser;
import com.bmc.truesight.saas.remedy.integration.TemplatePreParser;
import com.bmc.truesight.saas.remedy.integration.TemplateValidator;
import com.bmc.truesight.saas.remedy.integration.beans.Template;
import com.bmc.truesight.saas.remedy.integration.exception.ParsingException;
import com.bmc.truesight.saas.remedy.integration.exception.RemedyReadFailedException;
import com.bmc.truesight.saas.remedy.integration.exception.ValidationException;
import com.bmc.truesight.saas.remedy.integration.impl.GenericRemedyReader;
import com.bmc.truesight.saas.remedy.integration.impl.GenericTemplateParser;
import com.bmc.truesight.saas.remedy.integration.impl.GenericTemplatePreParser;
import com.boundary.plugin.sdk.CollectorDispatcher;
import com.boundary.plugin.sdk.Event;
import com.boundary.plugin.sdk.EventSink;
import com.boundary.plugin.sdk.EventSinkAPI;
import com.boundary.plugin.sdk.EventSinkStandardOutput;
import com.boundary.plugin.sdk.MeasurementSink;
import com.boundary.plugin.sdk.Plugin;
import com.boundary.plugin.sdk.PluginRunner;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

/**
 *
 * @author Santosh Patil
 * @author vitiwari
 */
public class RemedyPlugin implements Plugin<RemedyPluginConfiguration> {

    RemedyPluginConfiguration configuration;
    CollectorDispatcher dispatcher;
    EventSink eventOutput;
    EventSinkAPI eventSinkAPI;
    private static final Logger LOG = LoggerFactory.getLogger(RemedyPlugin.class);

    @Override
    public void setConfiguration(RemedyPluginConfiguration configuration) {
        this.configuration = configuration;
        this.eventOutput = new EventSinkStandardOutput();
        this.eventSinkAPI = new EventSinkAPI();
    }

    @Override
    public void setEventOutput(final EventSink output) {
        this.eventOutput = output;
    }

    @Override
    public void loadConfiguration() {
        LOG.debug("loading param.json parameters configuration");
        Gson gson = new Gson();
        String param = System.getenv(Constants.TSP_PLUGIN_PARAMS);
        LOG.debug("System environment has parameter available as  ,{}", param);
        try {
            RemedyPluginConfiguration pluginConfiguration = null;
            if (param == null || param == "") {
                pluginConfiguration = gson.fromJson(new FileReader("param.json"), RemedyPluginConfiguration.class);
            } else {
                pluginConfiguration = gson.fromJson(param, RemedyPluginConfiguration.class);
            }
            setConfiguration(pluginConfiguration);
            LOG.debug("param.json parameters configuration loading completed");
        } catch (JsonParseException e) {
            System.err.println("Exception occured while getting the param.json data" + e.getMessage());
            eventOutput.emit(Util.eventMeterTSI(Constants.REMEDY_PLUGIN_TITLE_MSG, e.getMessage(), Event.EventSeverity.ERROR.toString()));
        } catch (IOException e) {
            System.err.println("IOException occured while getting the param.json data" + e.getMessage());
            eventOutput.emit(Util.eventMeterTSI(Constants.REMEDY_PLUGIN_TITLE_MSG, e.getMessage(), Event.EventSeverity.ERROR.toString()));
        }
    }

    @Override
    public void setDispatcher(CollectorDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public void run() {
        LOG.debug("###########  Plugin started ##############");
        ArrayList<RemedyPluginConfigurationItem> items = configuration.getItems();
        LOG.debug("Plugin has {} instance ", items.size());
        if (items != null && items.size() > 0) {
            for (RemedyPluginConfigurationItem config : items) {
                LOG.debug("_____ {} instance validation", config.getRequestType());
                boolean isTemplateParsingSuccessful = false;
                boolean isTemplateValidationSuccessful = false;
                ARServerForm form = null;
                //PARSING THE JSON STRING
                //System.err.println("parsing param.json data");
                TemplateParser templateParser = new GenericTemplateParser();
                TemplatePreParser templatePreParser = new GenericTemplatePreParser();
                Template template = null;
                Map<Integer, Field> fieldmap = null;
                try {
                    Template defaultTemplate = new Template();
                    LOG.debug("Loading default field mapping  ...");
                    if (config.getRequestType().equalsIgnoreCase(RequestType.IM.getValues())) {
                        defaultTemplate = templatePreParser.loadDefaults(ARServerForm.INCIDENT_FORM);
                        form = ARServerForm.INCIDENT_FORM;
                    } else if (config.getRequestType().equalsIgnoreCase(RequestType.CM.getValues())) {
                        defaultTemplate = templatePreParser.loadDefaults(ARServerForm.CHANGE_FORM);
                        form = ARServerForm.CHANGE_FORM;
                    }
                    LOG.debug("Default field mapping load complete");
                    template = templateParser.readParseConfigJson(defaultTemplate, Util.getFieldValues(config.getFields()));
                    LOG.debug("User field mapping loading and overriding default values completed");
                    template.getEventDefinition().getProperties().put("app_id", config.getAppId());
                    LOG.debug("App Id set as \"{}\"", config.getAppId());
                    isTemplateParsingSuccessful = true;
                } catch (ParsingException ex) {
                    System.err.println("Parsing failed - " + ex.getMessage());
                } catch (Exception ex) {
                    System.err.println("Parsing failed - " + ex.getMessage());
                }

                if (isTemplateParsingSuccessful) {
                    RemedyReader reader = new GenericRemedyReader();
                    try {
                        int port = 0;
                        if (config.getPort() != null && config.getPort() != "") {
                            try {
                                port = Integer.parseInt(config.getPort());
                            } catch (Exception ex) {
                                LOG.warn("port number invalid, default port number 0 is used ");
                            }
                        }
                        LOG.debug("Starting call to recieve available field maps from  AR server");
                        ARServerUser user = reader.createARServerContext(config.getHostName(), port, config.getUserName(), config.getPassword());
                        fieldmap = reader.getFieldsMap(user, form);
                        // VALIDATION OF THE CONFIGURATION
                        LOG.debug("{} fieldMap recieved from  AR server Remedy \"{}\"", fieldmap.size());
                        TemplateValidator validator = new PluginTemplateValidator(fieldmap);
                        LOG.debug("Starting template validation ....");
                        validator.validate(template);
                        LOG.debug("Template validation completed");
                        isTemplateValidationSuccessful = true;
                    } catch (ValidationException ex) {
                        System.err.println("Validation failed - " + ex.getMessage());
                    } catch (RemedyReadFailedException e) {
                        System.err.println("Validation failed - " + e.getMessage());
                    } catch (Exception ex) {
                        System.err.println("Validation failed - " + ex.getMessage());
                    }
                } else {
                    LOG.debug("Template parsing is not successful, plugin cannot run if any instance has wrong configuration. Stopping the plugin");
                    System.exit(1);
                }

                if (isTemplateValidationSuccessful) {
                    LOG.debug("Template validation is successful, the collector instance will be added");
                    if (config.getRequestType().equalsIgnoreCase(RequestType.CM.getValues())) {
                        dispatcher.addCollector(new RemedyTicketsCollector(config, template, fieldmap, ARServerForm.CHANGE_FORM));
                    } else if (config.getRequestType().equalsIgnoreCase(RequestType.IM.getValues())) {
                        dispatcher.addCollector(new RemedyTicketsCollector(config, template, fieldmap, ARServerForm.INCIDENT_FORM));
                    }
                    LOG.debug("Collector instance added");
                } else {
                    LOG.debug("Template validation is not successful, plugin cannot run if any instance has wrong configuration. Stopping the plugin");
                    System.exit(1);
                }
            }
            LOG.debug("About to start running the added instances .....");
            dispatcher.run();
            LOG.debug("______  Instances started ");
        }
    }

    public static void main(String[] args) {
        PluginRunner plugin = new PluginRunner("com.bmc.truesight.meter.plugin.remedy.RemedyPlugin");
        plugin.run();
    }

    @Override
    public void setMeasureOutput(MeasurementSink output) {
    }
}
