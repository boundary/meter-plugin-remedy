package com.bmc.truesight.meter.plugin.remedy.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bmc.arsys.api.Field;
import com.bmc.truesight.saas.remedy.integration.TemplateValidator;
import com.bmc.truesight.saas.remedy.integration.beans.Configuration;
import com.bmc.truesight.saas.remedy.integration.beans.EventSource;
import com.bmc.truesight.saas.remedy.integration.beans.FieldItem;
import com.bmc.truesight.saas.remedy.integration.beans.TSIEvent;
import com.bmc.truesight.saas.remedy.integration.beans.Template;
import com.bmc.truesight.saas.remedy.integration.exception.ValidationException;
import com.bmc.truesight.saas.remedy.integration.util.Constants;
import com.bmc.truesight.saas.remedy.integration.util.StringUtil;

/**
 *
 * @author vitiwari
 */
public class PluginTemplateValidator implements TemplateValidator {

    private static final Logger log = LoggerFactory.getLogger(PluginTemplateValidator.class);

    private Map<Integer, Field> fieldmap;

    public PluginTemplateValidator(Map<Integer, Field> fieldmap) {
        this.fieldmap = fieldmap;
    }

    @Override
    public boolean validate(Template template) throws ValidationException {
        Configuration config = template.getConfig();
        TSIEvent payload = template.getEventDefinition();
        Map<String, FieldItem> baseFieldItemMap = template.getFieldDefinitionMap();

        List<String> invalidDefinitionList = new ArrayList<>();
        Map<Integer, Field> usableFieldMap = new HashMap<>();
        Map<String, FieldItem> fieldItemMap = new HashMap<>();
        baseFieldItemMap.keySet().forEach(key -> {
            Field field = fieldmap.get(baseFieldItemMap.get(key).getFieldId());
            if (field == null) {
                invalidDefinitionList.add(key);
            } else {
                fieldItemMap.put(key, baseFieldItemMap.get(key));
                usableFieldMap.put(field.getFieldID(), field);
            }
        });
        fieldmap.clear();
        fieldmap.putAll(usableFieldMap);
        if (invalidDefinitionList.size() > 0) {
            System.err.println("The following field definitions are dropped because they have invalid fieldId. " + invalidDefinitionList.toString());
        }
        if (config.getConditionFields().size() == 0) {
            throw new ValidationException(StringUtil.format(Constants.CONFIG_VALIDATION_FAILED, new Object[]{}));
        }

        // validate payload configuration
        if (payload.getTitle() != null && payload.getTitle().startsWith("@") && !fieldItemMap.containsKey(payload.getTitle())) {
            throw new ValidationException(StringUtil.format(Constants.PAYLOAD_PLACEHOLDER_DEFINITION_MISSING,
                    new Object[]{payload.getTitle()}));
        }

        for (String fpField : payload.getFingerprintFields()) {
            if (fpField != null) {
                if (fpField.startsWith("@")) {
                    String field = fpField.substring(1);
                    if (!Constants.FINGERPRINT_EVENT_FIELDS.contains(field)) {
                        throw new ValidationException(StringUtil.format(Constants.EVENT_FIELD_MISSING, new Object[]{field, Constants.FINGERPRINT_EVENT_FIELDS}));
                    }
                } else {
                    if (!payload.getProperties().containsKey(fpField)) {
                        throw new ValidationException(StringUtil.format(Constants.EVENT_PROPERTY_FIELD_MISSING, new Object[]{fpField}));
                    }
                }
            }
        }

        Map<String, String> properties = payload.getProperties();
        if (properties.keySet().size() > Constants.MAX_PROPERTY_FIELD_SUPPORTED) {
            throw new ValidationException(StringUtil.format(Constants.PROPERTY_FIELD_COUNT_EXCEEDS, new Object[]{properties.keySet().size(), Constants.MAX_PROPERTY_FIELD_SUPPORTED}));
        }
        if (!properties.containsKey(Constants.PROPERTY_APP_ID)) {
            throw new ValidationException(StringUtil.format(Constants.APPLICATION_NAME_NOT_FOUND, new Object[0]));
        }
        if (!properties.containsKey(Constants.PROPERTY_LAST_MODIFIED_DATE)) {
            throw new ValidationException(StringUtil.format(Constants.LAST_MODIFIED_DATE_NOT_FOUND, new Object[0]));
        }

        for (String key : properties.keySet()) {
            if (!StringUtil.isValidJavaIdentifier(key)) {
                throw new ValidationException(StringUtil.format(Constants.PROPERTY_NAME_INVALID, new Object[]{key.trim()}));
            }
            if (properties.get(key).startsWith("@") && !fieldItemMap.containsKey(properties.get(key))) {
                throw new ValidationException(StringUtil.format(Constants.PAYLOAD_PLACEHOLDER_DEFINITION_MISSING, new Object[]{properties.get(key)}));
            }
            if (properties.get(key).startsWith("#")) {
                validateConfigField(config, properties.get(key).substring(1));
            }
        }

        if (payload.getSeverity() != null && payload.getSeverity().startsWith("@") && !fieldItemMap.containsKey(payload.getSeverity())) {
            throw new ValidationException(StringUtil.format(Constants.PAYLOAD_PLACEHOLDER_DEFINITION_MISSING, new Object[]{payload.getSeverity()}));
        }

        if (payload.getStatus() != null && payload.getStatus().startsWith("@") && !fieldItemMap.containsKey(payload.getStatus())) {
            throw new ValidationException(
                    StringUtil.format(Constants.PAYLOAD_PLACEHOLDER_DEFINITION_MISSING, new Object[]{payload.getStatus()}));
        }
        if (payload.getCreatedAt() != null && payload.getCreatedAt().startsWith("@") && !fieldItemMap.containsKey(payload.getCreatedAt())) {
            throw new ValidationException(StringUtil.format(Constants.PAYLOAD_PLACEHOLDER_DEFINITION_MISSING, new Object[]{payload.getCreatedAt()}));
        }

        if (payload.getEventClass() != null && payload.getEventClass().startsWith("@") && !fieldItemMap.containsKey(payload.getEventClass())) {
            throw new ValidationException(StringUtil.format(Constants.PAYLOAD_PLACEHOLDER_DEFINITION_MISSING, new Object[]{payload.getEventClass()}));
        }

        //validating source
        EventSource source = payload.getSource();
        if (source.getName() != null && source.getName().startsWith("@") && !fieldItemMap.containsKey(source.getName())) {
            throw new ValidationException(StringUtil.format(Constants.PAYLOAD_PLACEHOLDER_DEFINITION_MISSING, new Object[]{source.getName()}));
        }
        if (source.getType() != null && source.getType().startsWith("@") && !fieldItemMap.containsKey(source.getType())) {
            throw new ValidationException(StringUtil.format(Constants.PAYLOAD_PLACEHOLDER_DEFINITION_MISSING, new Object[]{source.getType()}));
        }
        if (source.getRef() != null && source.getRef().startsWith("@") && !fieldItemMap.containsKey(source.getRef())) {
            throw new ValidationException(StringUtil.format(Constants.PAYLOAD_PLACEHOLDER_DEFINITION_MISSING, new Object[]{source.getRef()}));
        }

        // validate Config entries
        if (payload.getTitle() != null && payload.getTitle().startsWith("#")) {
            validateConfigField(config, payload.getTitle().substring(1));
        }
        if (payload.getSeverity() != null && payload.getSeverity().startsWith("#")) {
            validateConfigField(config, payload.getSeverity().substring(1));
        }

        if (payload.getStatus() != null && payload.getStatus().startsWith("#")) {
            validateConfigField(config, payload.getStatus().substring(1));
        }
        if (payload.getCreatedAt() != null && payload.getCreatedAt().startsWith("#")) {
            validateConfigField(config, payload.getCreatedAt().substring(1));
        }

        if (payload.getEventClass() != null && payload.getEventClass().startsWith("#")) {
            validateConfigField(config, payload.getEventClass().substring(1));
        }

        //validating source
        source = payload.getSource();
        if (source.getName() != null && source.getName().startsWith("#")) {
            validateConfigField(config, source.getName().substring(1));
        }
        if (source.getType() != null && source.getType().startsWith("#")) {
            validateConfigField(config, source.getType().substring(1));
        }
        if (source.getRef() != null && source.getRef().startsWith("#")) {
            validateConfigField(config, source.getRef().substring(1));
        }

        return true;

    }

    private void validateConfigField(Configuration config, String fieldName) throws ValidationException {
        try {
            config.getClass().getDeclaredField(fieldName);
        } catch (NoSuchFieldException ex) {
            throw new ValidationException(StringUtil.format(Constants.PAYLOAD_PLACEHOLDER_FIELD_MISSING, new Object[]{fieldName}));
        } catch (SecurityException se) {
            log.error("accessing the config field {} failed, skipping this field for validation", fieldName);
        }
    }
}
