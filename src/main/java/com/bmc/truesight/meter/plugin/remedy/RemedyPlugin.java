package com.bmc.truesight.meter.plugin.remedy;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bmc.truesight.meter.plugin.remedy.util.Constants;
import com.bmc.truesight.meter.plugin.remedy.util.PluginTemplateValidator;
import com.bmc.truesight.meter.plugin.remedy.util.RequestType;
import com.bmc.truesight.meter.plugin.remedy.util.Util;
import com.bmc.truesight.saas.remedy.integration.ARServerForm;
import com.bmc.truesight.saas.remedy.integration.TemplateParser;
import com.bmc.truesight.saas.remedy.integration.TemplatePreParser;
import com.bmc.truesight.saas.remedy.integration.TemplateValidator;
import com.bmc.truesight.saas.remedy.integration.beans.Template;
import com.bmc.truesight.saas.remedy.integration.exception.ParsingException;
import com.bmc.truesight.saas.remedy.integration.exception.ValidationException;
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
        Gson gson = new Gson();
        try {
            //System.err.println("loading param.json data");
            RemedyPluginConfiguration pluginConfiguration = gson.fromJson(new FileReader("param.json"), RemedyPluginConfiguration.class);
            setConfiguration(pluginConfiguration);
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
        ArrayList<RemedyPluginConfigurationItem> items = configuration.getItems();
        if (items != null && items.size() > 0) {
            for (RemedyPluginConfigurationItem config : items) {
                boolean isTemplateParsingSuccessful = false;
                boolean isTemplateValidationSuccessful = false;

                //PARSING THE JSON STRING
                //System.err.println("parsing param.json data");
                TemplateParser templateParser = new GenericTemplateParser();
                TemplatePreParser templatePreParser = new GenericTemplatePreParser();
                Template template = null;
                try {
                    Template defaultTemplate = new Template();
                    if (config.getRequestType().equalsIgnoreCase(RequestType.IM.getValues())) {
                        defaultTemplate = templatePreParser.loadDefaults(ARServerForm.INCIDENT_FORM);
                    } else if (config.getRequestType().equalsIgnoreCase(RequestType.CM.getValues())) {
                        defaultTemplate = templatePreParser.loadDefaults(ARServerForm.CHANGE_FORM);
                    }
                    template = templateParser.readParseConfigJson(defaultTemplate, Util.getFieldValues(config.getFields()));
                    template.getEventDefinition().getProperties().put("app_id", config.getAppId());
                    isTemplateParsingSuccessful = true;
                } catch (ParsingException ex) {
                    System.err.println("Parsing failed - " + ex.getMessage());
                } catch (Exception ex) {
                    System.err.println("Parsing failed - " + ex.getMessage());
                }

                if (isTemplateParsingSuccessful) {
                    TemplateValidator templateValidator = new PluginTemplateValidator();
                    try {
                        templateValidator.validate(template);
                        isTemplateValidationSuccessful = true;
                    } catch (ValidationException ex) {
                        System.err.println("Validation failed - " + ex.getMessage());
                    } catch (Exception ex) {
                        System.err.println("Validation failed - " + ex.getMessage());
                    }
                } else {
                    System.exit(1);
                }

                if (isTemplateValidationSuccessful) {
                    if (config.getRequestType().equalsIgnoreCase(RequestType.CM.getValues())) {
                        dispatcher.addCollector(new RemedyTicketsCollector(config, template, ARServerForm.CHANGE_FORM));
                    } else if (config.getRequestType().equalsIgnoreCase(RequestType.IM.getValues())) {
                        dispatcher.addCollector(new RemedyTicketsCollector(config, template, ARServerForm.INCIDENT_FORM));
                    }
                } else {
                    System.exit(1);
                }
            }
            dispatcher.run();
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
