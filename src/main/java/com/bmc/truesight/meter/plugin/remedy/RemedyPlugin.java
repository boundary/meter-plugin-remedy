/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bmc.truesight.meter.plugin.remedy;

import com.boundary.plugin.sdk.CollectorDispatcher;
import com.boundary.plugin.sdk.EventSink;
import com.boundary.plugin.sdk.EventSinkStandardOutput;
import com.boundary.plugin.sdk.MeasurementSink;
import com.boundary.plugin.sdk.MeasurementSinkStandardOut;
import com.boundary.plugin.sdk.Plugin;
import com.boundary.plugin.sdk.PluginRunner;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 *
 * @author gokumar
 */
public class RemedyPlugin implements Plugin<RemedyPluginConfiguration> {

    RemedyPluginConfiguration configuration;
    CollectorDispatcher dispatcher;
    MeasurementSink output;
    EventSink eventOutput;

    @Override
    public void setConfiguration(RemedyPluginConfiguration configuration) {
        this.configuration = configuration;
        this.output = new MeasurementSinkStandardOut();
        this.eventOutput = new EventSinkStandardOutput();
        output.getClass();
    }

    @Override
    public void setEventOutput(final EventSink output) {
        this.eventOutput = output;
    }

    @Override
    public void loadConfiguration() {
        Gson gson = new Gson();
        try {
            RemedyPluginConfiguration configuration = gson.fromJson(new FileReader("param.json"), RemedyPluginConfiguration.class);
            setConfiguration(configuration);
        } catch (JsonParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void setDispatcher(CollectorDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public void run() {

        ArrayList<RemedyPluginConfigurationItem> items = configuration.getItems();
        items.forEach((i) -> {
            dispatcher.addCollector(new RemedyCollector(i));
        });
        dispatcher.run();
    }

    public static void main(String[] args) {
        PluginRunner plugin = new PluginRunner("com.bmc.truesight.meter.plugin.remedy.RemedyPlugin");
        plugin.run();
    }

    @Override
    public void setMeasureOutput(MeasurementSink output) {
        this.output = output;
    }
}
