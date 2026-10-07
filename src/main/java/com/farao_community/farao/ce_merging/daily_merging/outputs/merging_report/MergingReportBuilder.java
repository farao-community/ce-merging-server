/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.daily_merging.outputs.merging_report;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.daily_merging.merging_request.RequestInformation;
import com.farao_community.farao.ce_merging.daily_merging.outputs.merging_report.sheets.FilesSheet;
import com.farao_community.farao.ce_merging.daily_merging.outputs.merging_report.sheets.MergeSheet;
import com.farao_community.farao.ce_merging.daily_merging.outputs.merging_report.sheets.XNodeInconsistenciesSheet;
import com.farao_community.farao.ce_merging.merging.process.final_cgm_result.FinalCgmResult;
import com.farao_community.farao.ce_merging.merging.process.final_cgm_result.LoadFlowOutput;
import com.farao_community.farao.ce_merging.merging.process.xnode.inconsistencies.XnodesInconsistencies;
import com.farao_community.farao.ce_merging.merging.task.entities.IgmData;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.powsybl.iidm.network.Country;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.extra.Interval;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.AC;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.EMPTY;
import static com.farao_community.farao.ce_merging.common.util.OutputUtils.OUTPUT_DATE_FORMATTER;
import static com.farao_community.farao.ce_merging.daily_merging.outputs.merging_report.sheets.XNodeInconsistenciesSheet.fromXnodeIncorrect;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.CGM_NET_POSITIONS_FILE;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.XNODES_INCONSISTENCIES;
import static com.farao_community.farao.ce_merging.merging.task.enums.IgmType.D2CF;
import static com.farao_community.farao.ce_merging.merging.task.enums.IgmType.DACF;
import static com.farao_community.farao.ce_merging.merging.task.enums.TaskStatus.ERROR;
import static com.powsybl.iidm.network.Country.CH;
import static com.powsybl.iidm.network.Country.IT;

public class MergingReportBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(MergingReportBuilder.class);

    private final String businessDay;
    private final String interval;
    private final List<MergingTask> hourlyTasks;

    public MergingReportBuilder(final List<MergingTask> hourlyTasks,
                                final RequestInformation requestInformation) {
        this.businessDay = requestInformation.getMergingDay().format(OUTPUT_DATE_FORMATTER);
        this.interval = requestInformation.requestTimeInterval();
        this.hourlyTasks = hourlyTasks;
    }

    public void handle(final String filePath) {
        ExcelWriter.export(filePath, getMergeSheets(), getXNodeInconsistenciesSheets(), getFilesSheets());
    }

    public List<MergeSheet> getMergeSheets() {
        return hourlyTasks.stream().map(this::getMergeSheet).toList();
    }

    public List<XNodeInconsistenciesSheet> getXNodeInconsistenciesSheets() {
        return hourlyTasks.stream().map(this::getXNodeInconsistenciesSheet).flatMap(List::stream).toList();
    }

    public List<FilesSheet> getFilesSheets() {
        return hourlyTasks.stream().map(this::getFilesSheet).toList();
    }

    private MergeSheet getMergeSheet(final MergingTask task) {
        int iterationsNumber;
        double slackCompensation;
        try {
            final LoadFlowOutput loadFlowOutput = task.getArtifact(CGM_NET_POSITIONS_FILE, FinalCgmResult.class).getLoadFlowResults();
            iterationsNumber = loadFlowOutput.getIterationNumber();
            slackCompensation = loadFlowOutput.getSlackCompensation();
        } catch (final Exception e) {
            LOGGER.warn("Hourly task failed for interval: '{}' before CGM result calculation, iterationsNumber and slackCompensation will be set to 0",
                        task.getTargetDate());
            iterationsNumber = 0;
            slackCompensation = 0;
        }

        final boolean taskFailed = task.getStatus() == ERROR;
        return new MergeSheet(businessDay,
                              getTaskPosition(task),
                              taskFailed ? "Failure" : AC,
                              iterationsNumber,
                              slackCompensation,
                              taskFailed ? task.getStatusDetail() : EMPTY,
                              EMPTY);
    }

    private List<XNodeInconsistenciesSheet> getXNodeInconsistenciesSheet(final MergingTask task) {
        final String instant = getTaskPosition(task);
        try {
            return task.getArtifact(XNODES_INCONSISTENCIES, XnodesInconsistencies.class)
                    .getXnodeIncorrectList()
                    .stream()
                    .map(xnodeIncorrect -> fromXnodeIncorrect(businessDay, instant, xnodeIncorrect))
                    .toList();
        } catch (final Exception e) {
            LOGGER.warn("Hourly task failed for interval '{}' before xNodes inconsistencies check, so these will not be displayed for this timestamp in merging report file",
                        task.getTargetDate());
            return List.of(new XNodeInconsistenciesSheet(businessDay, instant, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY));
        }
    }

    private FilesSheet getFilesSheet(final MergingTask task) {
        return new FilesSheet(businessDay, getTaskPosition(task), EMPTY, getIgmKind(task, CH), getIgmKind(task, IT));
    }

    private String getIgmKind(final MergingTask task,
                              final Country country) {

        final boolean hasCountryInCore = task.getInputs().getIgms().stream()
                .map(IgmData::getIgmName)
                .anyMatch(name -> name.contains(country.toString()) && name.contains(D2CF.getTypeCode()));

        return hasCountryInCore ? D2CF.name() : DACF.name();
    }

    private Map<Integer, Interval> getIntervalsByPosition() {
        final Interval intervalObj = Interval.parse(interval);
        Instant startDate = intervalObj.getStart();
        final Instant endDate = intervalObj.getEnd();
        final Map<Integer, Interval> positionsMap = new TreeMap<>();
        int position = 1;
        while (startDate.isBefore(endDate)) {
            positionsMap.put(position, Interval.of(startDate, startDate.plus(Duration.ofHours(1))));
            startDate = startDate.plus(Duration.ofHours(1));
            position++;
        }
        return positionsMap;
    }

    String getTaskPosition(final MergingTask task) {
        final Instant instant = task.getTargetDate().toInstant();
        return getIntervalsByPosition().entrySet().stream()
                .filter(entry -> entry.getValue().contains(instant)) // not a collection type contains
                .findFirst()
                .orElseThrow(() -> new CeMergingException(String.format("Instant %s not found in interval %s", instant, interval)))
                .getKey()
                .toString();
    }
}
