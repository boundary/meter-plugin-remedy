// Copyright 2014-2015 Boundary, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.bmc.truesight.meter.plugin.remedy;

import com.bmc.arsys.api.ARException;
import com.bmc.arsys.api.ARServerUser;
import com.bmc.arsys.api.ArithmeticOrRelationalOperand;
import com.bmc.arsys.api.DataType;
import com.bmc.arsys.api.Entry;
import com.bmc.arsys.api.OutputInteger;
import com.bmc.arsys.api.QualifierInfo;
import com.bmc.arsys.api.RelationalOperationInfo;
import com.bmc.arsys.api.SortInfo;
import com.bmc.arsys.api.Timestamp;
import com.bmc.arsys.api.Value;
import com.bmc.truesight.meter.plugin.remedy.beans.ARConstants;

import com.boundary.plugin.sdk.Collector;
import com.boundary.plugin.sdk.Event;
import com.boundary.plugin.sdk.EventSinkStandardOutput;
import com.boundary.plugin.sdk.Measurement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class RemedyCollector implements Collector {

    private RemedyPluginConfigurationItem config;
    private static final String MY_NAME = "arapi";
    private static final String SUMMARY_FIELD = "DESCRIPTION";
    private static final String DETAILS_FIELD = "DETAILED_DESCRIPTION";
    private static final String REPORTED_DATE = "REPORTED_DATE";
    private static final String CLOSED_DATE = "CLOSED_DATE";
    private static final String OWNER_GROUP = "OWNER_GROUP";
    private static final String SERVICE = "SERVICE";
    private static final String PRIORITY = "PRIORITY";
    private static final String REPORTED_SOURCE = "REPORTED_SOURCE";
    private static final String STATUS = "STATUS";
    private static final String ASSIGNEE = "ASSIGNEE";
    private static final int CHUNK_SIZE = 2000;

    public RemedyCollector(RemedyPluginConfigurationItem config) {
        this.config = config;
    }

    private ARServerUser createARServerContext() {
        ARServerUser arServerContext = new ARServerUser();

        arServerContext.setServer(this.config.getHostName());
        arServerContext.setPort(this.config.getPort());
        arServerContext.setUser(this.config.getUserName());
        arServerContext.setPassword(this.config.getPassword());

        return arServerContext;
    }

    @Override
    public Measurement[] getMeasures() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void run() {
        ARServerUser arServerContext = createARServerContext();

        EventSinkStandardOutput output = new EventSinkStandardOutput();

        Calendar cal = null;
        while (true) {
            try {
                //arServerContext.login();
                cal = Calendar.getInstance();
                cal.add(Calendar.MINUTE, (0 - config.getPollInterval()));
                Collection<Event> events = fetchData(arServerContext, cal.getTime(), config.getMaxRecords());
                events.stream().forEach(event -> {
                    output.emit(event);
                });

                Thread.sleep(config.getPollInterval());
            } catch (InterruptedException | ARException e) {
                e.printStackTrace();
            } finally {
                arServerContext.logout();
            }
        }
    }

    private Collection<Event> fetchData(ARServerUser arServerContext, Date date, long maxRecords) throws ARException {

        String strShortSummary = null;
        String strRequestId = null;
        String strSummary = null;
        String strSubmitDate = null;
        String strCloseDate = null;
        String strOwningGroup = null;
        String strService = null;
        String strPriority = null;
        String strReportedSource = null;
        String strStauts = null;
        String strAssignee = null;

        List<Event> events = new ArrayList<>();

        int[] queryFieldsList = {
            ARConstants.INCIDENT_ID_FIELD,
            ARConstants.SUMMARY_FIELD,
            ARConstants.SHORT_SUMMARY_FIELD,
            ARConstants.SUBMIT_DATE_FIELD,
            ARConstants.CLOSE_DATE_FIELD,
            ARConstants.OWNING_GROUP_FIELD,
            ARConstants.SERVICE_FIELD,
            ARConstants.PRIORITY_FIELD,
            ARConstants.REPORTED_SOURCE_FIELD,
            ARConstants.STATUS_FIELD,
            ARConstants.ASSIGNEE_FIELD
        };

        Date newDate = new Date();

        QualifierInfo qualInfo1 = buildFieldValueQualification(
                ARConstants.SUBMIT_DATE_FIELD, new Value(new Timestamp(date), DataType.TIME),
                RelationalOperationInfo.AR_REL_OP_GREATER_EQUAL);

        QualifierInfo qualInfo2 = buildFieldValueQualification(
                ARConstants.SUBMIT_DATE_FIELD, new Value(new Timestamp(newDate), DataType.TIME),
                RelationalOperationInfo.AR_REL_OP_LESS_EQUAL);

        QualifierInfo qualInfo = new QualifierInfo(QualifierInfo.AR_COND_OP_AND, qualInfo1, qualInfo2);

        OutputInteger nMatches = new OutputInteger();
        List<SortInfo> sortOrder = new ArrayList<SortInfo>();

        long totalIterations = 1;
        if (maxRecords > (long) CHUNK_SIZE) {
            totalIterations = maxRecords / (long) CHUNK_SIZE;
        }

        boolean stopProcessing = false;
        int totalDocuments = 0;
        for (int count = 0; count < totalIterations + 1; count++) {
            List<Entry> entryList = arServerContext.getListEntryObjects(
                    ARConstants.HELP_DESK_FORM, qualInfo, CHUNK_SIZE * (count),
                    CHUNK_SIZE, sortOrder,
                    queryFieldsList,
                    false, nMatches);

            if (entryList.size() < 1) {
                break;
            }

            for (Entry queryEntry : entryList) {

                strShortSummary = null;
                strRequestId = null;
                strSummary = null;

                for (Map.Entry<Integer, Value> fieldIdVal : queryEntry
                        .entrySet()) {
                    if ((fieldIdVal.getKey()).toString().equals(
                            Integer.toString(ARConstants.INCIDENT_ID_FIELD))) {
                        if (fieldIdVal.getValue().getValue() != null) {
                            strRequestId = fieldIdVal.getValue().getValue()
                                    .toString();
                        }
                    }
                    if ((fieldIdVal.getKey()).toString().equals(
                            Integer.toString(ARConstants.SHORT_SUMMARY_FIELD))) {
                        if (fieldIdVal.getValue().getValue() != null) {
                            strShortSummary = fieldIdVal.getValue().getValue()
                                    .toString();
                        }
                    }

                    if ((fieldIdVal.getKey()).toString().equals(
                            Integer.toString(ARConstants.SUMMARY_FIELD))) {
                        if (fieldIdVal.getValue().getValue() != null) {
                            strSummary = fieldIdVal.getValue().getValue()
                                    .toString();
                        }
                    }
                    if ((fieldIdVal.getKey()).toString().equals(
                            Integer.toString(ARConstants.SUBMIT_DATE_FIELD))) {
                        if (fieldIdVal.getValue().getValue() != null) {
                            strSubmitDate = fieldIdVal.getValue().getValue()
                                    .toString();
                        }
                    }
                    if ((fieldIdVal.getKey()).toString().equals(
                            Integer.toString(ARConstants.CLOSE_DATE_FIELD))) {
                        if (fieldIdVal.getValue().getValue() != null) {
                            strCloseDate = fieldIdVal.getValue().getValue()
                                    .toString();
                        }
                    }
                    if ((fieldIdVal.getKey()).toString().equals(
                            Integer.toString(ARConstants.OWNING_GROUP_FIELD))) {
                        if (fieldIdVal.getValue().getValue() != null) {
                            strOwningGroup = fieldIdVal.getValue().getValue()
                                    .toString();
                        }
                    }
                    if ((fieldIdVal.getKey()).toString().equals(
                            Integer.toString(ARConstants.SERVICE_FIELD))) {
                        if (fieldIdVal.getValue().getValue() != null) {
                            strService = fieldIdVal.getValue().getValue()
                                    .toString();
                        }
                    }
                    if ((fieldIdVal.getKey()).toString().equals(
                            Integer.toString(ARConstants.PRIORITY_FIELD))) {
                        if (fieldIdVal.getValue().getValue() != null) {
                            strPriority = fieldIdVal.getValue().getValue()
                                    .toString();
                        }
                    }
                    if ((fieldIdVal.getKey()).toString().equals(
                            Integer.toString(ARConstants.REPORTED_SOURCE_FIELD))) {
                        if (fieldIdVal.getValue().getValue() != null) {
                            strReportedSource = fieldIdVal.getValue().getValue()
                                    .toString();
                        }
                    }
                    if ((fieldIdVal.getKey()).toString().equals(
                            Integer.toString(ARConstants.STATUS_FIELD))) {
                        if (fieldIdVal.getValue().getValue() != null) {
                            strStauts = fieldIdVal.getValue().getValue()
                                    .toString();
                        }
                    }
                    if ((fieldIdVal.getKey()).toString().equals(
                            Integer.toString(ARConstants.ASSIGNEE_FIELD))) {
                        if (fieldIdVal.getValue().getValue() != null) {
                            strAssignee = fieldIdVal.getValue().getValue()
                                    .toString();
                        }
                    }
                }

                //EventSeverity severity, String title, String message, String host, String source, List<String> tags
                Event event = new Event(Event.EventSeverity.INFO, strShortSummary,
                        strSummary, config.getHostName(), config.getSource(), null);

                events.add(event);
                totalDocuments++;

                if (totalDocuments == this.config.getMaxRecords()) {
                    stopProcessing = true;
                    break;
                }
            }

            if (stopProcessing == true) {
                break;
            }
        }
        return events;
    }

    /**
     * Prepare qualification "<fieldId>=<Value>"
     *
     * @return QualifierInfo
     */
    public static QualifierInfo buildFieldValueQualification(int fieldId, Value value, int relationalOperation) {
        ArithmeticOrRelationalOperand leftOperand = new ArithmeticOrRelationalOperand(fieldId);
        ArithmeticOrRelationalOperand rightOperand = new ArithmeticOrRelationalOperand(value);
        RelationalOperationInfo relationalOperationInfo = new RelationalOperationInfo(relationalOperation, leftOperand, rightOperand);
        QualifierInfo qualification = new QualifierInfo(relationalOperationInfo);
        return qualification;
    }

}
