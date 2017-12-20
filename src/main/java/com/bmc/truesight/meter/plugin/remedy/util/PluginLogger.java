package com.bmc.truesight.meter.plugin.remedy.util;

import java.text.MessageFormat;

import com.bmc.truesight.meter.plugin.remedy.RemedyPluginConfigurationItem;

public class PluginLogger {

    private RemedyPluginConfigurationItem config;

    public PluginLogger(RemedyPluginConfigurationItem config) {
        super();
        this.config = config;
    }

    public void info(String messageTemplate, Object[] args) {
        if (LogLevel.INFO.isEnabled(config.getLogLevel())) {
            MessageFormat mf = new MessageFormat(messageTemplate);
            MessageFormat prefix = new MessageFormat("[{0}][{1}][{2}]:");
            String prefixString = prefix.format(new Object[]{config.getHostName(), getInstanceType(config.getRequestType()), "info"});
            System.err.println(prefixString + mf.format(args));
        }
    }

    public void info(String messageTemplate) {
        if (LogLevel.INFO.isEnabled(config.getLogLevel())) {
            MessageFormat prefix = new MessageFormat("[{0}][{1}][{2}]:");
            String prefixString = prefix.format(new Object[]{config.getHostName(), getInstanceType(config.getRequestType()), "info"});
            System.err.println(prefixString + messageTemplate);
        }
    }

    public void error(String messageTemplate, Object[] args) {
        if (LogLevel.ERROR.isEnabled(config.getLogLevel())) {
            MessageFormat mf = new MessageFormat(messageTemplate);
            MessageFormat prefix = new MessageFormat("[{0}][{1}][{2}]:");
            String prefixString = prefix.format(new Object[]{config.getHostName(), getInstanceType(config.getRequestType()), "error"});
            System.err.println(prefixString + mf.format(args));
        }
    }

    public void error(String messageTemplate) {
        if (LogLevel.ERROR.isEnabled(config.getLogLevel())) {
            MessageFormat prefix = new MessageFormat("[{0}][{1}][{2}]:");
            String prefixString = prefix.format(new Object[]{config.getHostName(), getInstanceType(config.getRequestType()), "error"});
            System.err.println(prefixString + messageTemplate);
        }
    }

    public void debug(String messageTemplate, Object[] args) {
        if (LogLevel.DEBUG.isEnabled(config.getLogLevel())) {
            MessageFormat mf = new MessageFormat(messageTemplate);
            MessageFormat prefix = new MessageFormat("[{0}][{1}][{2}]:");
            String prefixString = prefix.format(new Object[]{config.getHostName(), getInstanceType(config.getRequestType()), "debug"});
            System.err.println(prefixString + mf.format(args));
        }
    }

    public void debug(String messageTemplate) {
        if (LogLevel.DEBUG.isEnabled(config.getLogLevel())) {
            MessageFormat prefix = new MessageFormat("[{0}][{1}][{2}]:");
            String prefixString = prefix.format(new Object[]{config.getHostName(), getInstanceType(config.getRequestType()), "debug"});
            System.err.println(prefixString + messageTemplate);
        }
    }

    private String getInstanceType(String instanceType) {
        if (instanceType.equalsIgnoreCase(RequestType.IM.getValues())) {
            return "incident";
        }
        if (instanceType.equalsIgnoreCase(RequestType.CM.getValues())) {
            return "change";
        }
        return "";
    }

}
