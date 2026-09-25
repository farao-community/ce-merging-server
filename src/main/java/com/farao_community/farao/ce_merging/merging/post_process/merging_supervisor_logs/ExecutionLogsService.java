/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.post_process.merging_supervisor_logs;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.util.JaxbUtils;
import com.farao_community.farao.ce_merging.merging.post_process.merging_supervisor_logs.entity.LoggingEvent;
import com.farao_community.farao.ce_merging.merging.post_process.merging_supervisor_logs.entity.LoggingEventProperty;
import com.farao_community.farao.ce_merging.merging.post_process.merging_supervisor_logs.model.MergingSupervisorLogsModel;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType;
import com.farao_community.farao.ce_merging.xsd.execution_logs.Context;
import com.farao_community.farao.ce_merging.xsd.execution_logs.Logs;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.EMPTY;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.MERGING_STEP;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.RTE_GSR_URL;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.TSO_LOWER_CASE;

@Service
public class ExecutionLogsService {

    private static final String MERGING_SUPERVISOR_TIMESTAMP_PATTERN = "dd/MM/yyyy HH:mm:ss";
    private static final String LOGS = "logs";
    private final LoggingEventRepository loggingEventRepository;

    public ExecutionLogsService(LoggingEventRepository loggingEventRepository) {
        this.loggingEventRepository = loggingEventRepository;
    }

    public byte[] generateLogsForMergingSupervisor(final MergingTask task) {
        final Set<LoggingEvent> logsByTaskId = loggingEventRepository.findLogsByTaskId(task.getId());
        final List<MergingSupervisorLogsModel> mergingSupervisorLogsRecordsList = adaptLogsForMergingSupervisor(logsByTaskId);

        final Logs logs = new Logs();
        final List<String> mergingSteps = Arrays.stream(MergingStep.values()).map(Enum::toString).toList();
        final Map<String, List<MergingSupervisorLogsModel>> logsByContext = new TreeMap<>();
        mergingSteps.forEach(mergingStep -> {
            List<MergingSupervisorLogsModel> listByContext = mergingSupervisorLogsRecordsList.stream().filter(logsModel -> mergingStep.equals(logsModel.getContext())).toList();
            logsByContext.put(mergingStep, listByContext);
        });

        final List<Context> contextsList = new ArrayList<>();
        logsByContext.forEach((key, value) -> {
            Context context = new Context();
            context.setNom(key);
            List<com.farao_community.farao.ce_merging.xsd.execution_logs.Record> recordsList = new ArrayList<>();

            if (key.equals(MergingStep.INITIAL_IMPORT.toString())) {
                final List<Context> tsoSubContextList = new ArrayList<>();
                final Map<String, List<MergingSupervisorLogsModel>> subRecordsMap = value.stream().collect(Collectors.groupingBy(MergingSupervisorLogsModel::getSubContextLevel1, Collectors.toList()));
                subRecordsMap.forEach((tso, supervisorLogsModelList) -> {
                    Context tsoSubContext = new Context();
                    tsoSubContext.setNom(tso);
                    List<com.farao_community.farao.ce_merging.xsd.execution_logs.Record> subRecordsList = new ArrayList<>();
                    supervisorLogsModelList.forEach(hit -> {
                        com.farao_community.farao.ce_merging.xsd.execution_logs.Record rec = new com.farao_community.farao.ce_merging.xsd.execution_logs.Record();
                        rec.setDt(hit.getTimestamp());
                        rec.setLevel(hit.getLevel());
                        rec.setValue(hit.getMessage());
                        subRecordsList.add(rec);
                    });
                    tsoSubContext.getRecOrCtxt().addAll(subRecordsList);
                    tsoSubContextList.add(tsoSubContext);
                });
                context.getRecOrCtxt().addAll(tsoSubContextList);
                contextsList.add(context);
            } else {

                value.forEach(hit -> {
                    com.farao_community.farao.ce_merging.xsd.execution_logs.Record rec = new com.farao_community.farao.ce_merging.xsd.execution_logs.Record();
                    rec.setDt(hit.getTimestamp());
                    rec.setLevel(hit.getLevel());
                    rec.setValue(hit.getMessage());
                    recordsList.add(rec);
                });

                context.getRecOrCtxt().addAll(recordsList);
                contextsList.add(context);
            }
        });

        contextsList.sort(Comparator.comparing(Context::getNom, Comparator.comparingInt(nom -> MergingStep.valueOf(nom).getOrder())));

        try {
            final Logs artifact = task.getArtifact(ArtifactType.LOAD_FLOW_ON_FINAL_CGM_LOGS, Logs.class);
            if (artifact == null) {
                throw new CeMergingException("No load flow logs");
            }
            final List<Context> openLoadFlowLogs = artifact.getCtxt();
            openLoadFlowLogs.getFirst().setNom("Open Loadflow on final CGM : " + task.getOutputs().getCgm().getOriginalName());
            contextsList.stream()
                    .filter(context -> context.getNom().equals(MergingStep.OPEN_LOAD_FLOW_LOGS.toString()))
                    .findFirst()
                    .ifPresent(context -> context.getRecOrCtxt().addAll(openLoadFlowLogs));
            final List<Context> finalContextList = contextsList.stream().filter(context -> !context.getRecOrCtxt().isEmpty()).toList();
            logs.getCtxt().addAll(finalContextList);
            return JaxbUtils.writeToBytes(Logs.class, logs, RTE_GSR_URL, LOGS);

        } catch (Exception e) {
            final List<Context> finalContextList = contextsList.stream().filter(context -> !context.getRecOrCtxt().isEmpty()).toList();
            logs.getCtxt().addAll(finalContextList);
            return JaxbUtils.writeToBytes(Logs.class, logs, RTE_GSR_URL, LOGS);
        }
    }

    private List<MergingSupervisorLogsModel> adaptLogsForMergingSupervisor(final Set<LoggingEvent> logs) {
        return logs.stream().map(this::convertToLogsModel).toList();
    }

    private MergingSupervisorLogsModel convertToLogsModel(final LoggingEvent log) {
        String mergingSupervisorTimestamp = getTimestampForLog(log);
        String mergingSupervisorContext = log.getProperties().stream()
                .filter(loggingEventProperty -> MERGING_STEP.equals(loggingEventProperty.getMappedKey()))
                .map(LoggingEventProperty::getMappedValue)
                .findFirst()
                .orElse(EMPTY);
        MergingSupervisorLogsModel msLogModel = new MergingSupervisorLogsModel(mergingSupervisorTimestamp, log.getLevelString(), log.getFormattedMessage(), mergingSupervisorContext);
        if (mergingSupervisorContext.equals(MergingStep.INITIAL_IMPORT.toString())) {
            String tso = log.getProperties().stream()
                    .filter(loggingEventProperty -> TSO_LOWER_CASE.equals(loggingEventProperty.getMappedKey()))
                    .map(LoggingEventProperty::getMappedValue)
                    .findFirst()
                    .orElse(EMPTY);
            msLogModel.setSubContextLevel1(tso);
        }
        return msLogModel;
    }

    private static String getTimestampForLog(final LoggingEvent log) {
        Instant instant = Instant.ofEpochMilli(log.getTimestmp());
        OffsetDateTime date = OffsetDateTime.ofInstant(instant, ZoneId.of("CET"));
        return DateTimeFormatter.ofPattern(MERGING_SUPERVISOR_TIMESTAMP_PATTERN).withLocale(Locale.FRANCE).format(date);
    }

}
