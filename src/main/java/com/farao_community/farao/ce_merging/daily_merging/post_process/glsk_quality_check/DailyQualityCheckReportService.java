/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.daily_merging.post_process.glsk_quality_check;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.util.DateTimeUtils;
import com.farao_community.farao.ce_merging.common.util.FileStorageUtils;
import com.farao_community.farao.ce_merging.common.util.JaxbUtils;
import com.farao_community.farao.ce_merging.common.util.OutputUtils;
import com.farao_community.farao.ce_merging.daily_merging.DailyMergingRepository;
import com.farao_community.farao.ce_merging.daily_merging.entities.DailyMergingTask;
import com.farao_community.farao.ce_merging.merging.post_process.common.SchemaLocationNamespace;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.IdentificationType;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.QualityCheckReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.XML_EXTENSION;

@Service
public class DailyQualityCheckReportService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DailyQualityCheckReportService.class);
    private final CeMergingConfiguration configuration;
    private final DailyMergingRepository repository;
    private static final String MESSAGE_TYPE = "A16";
    private static final String DOCUMENT_TYPE = "A48";
    private static final int FLOW = 117;

    public DailyQualityCheckReportService(final CeMergingConfiguration configuration, final DailyMergingRepository repository) {
        this.configuration = configuration;
        this.repository = repository;
    }

    public void computeDailyGlskQualityReport(final DailyMergingTask task, final List<MergingTask> tasksList, final String requestInterval) {
        try {
            final QualityCheckReport oneDayGlskReport = buildDailyGlskReport(task.getVersion(), tasksList, requestInterval);
            saveDailyGlskReportInOutputs(oneDayGlskReport, task);
        } catch (Exception e) {
            LOGGER.error("Error during creation of daily Glsk quality report for task '{}' ", task.getId(), e);
            throw new CeMergingException("Error during creation of daily Glsk quality report");
        }
    }

    QualityCheckReport buildDailyGlskReport(final int version, final List<MergingTask> tasksList, final String requestInterval) {
        final List<QualityCheckReport> reports = getAllResult(tasksList);
        if (reports.isEmpty()) {
            throw new CeMergingException("No GLSK quality report available");
        }
        final QualityCheckReport oneDayGlskReport = reports.get(0);
        updateHeader(oneDayGlskReport, requestInterval, version);
        reports.stream()
                .skip(1)
                .map(QualityCheckReport::getQualityChecks)
                .forEach(oneDayGlskReport.getQualityChecks()::addAll);
        return oneDayGlskReport;
    }

    private void updateHeader(final QualityCheckReport oneDayGlskReport, final String requestInterval, final int version) {
        oneDayGlskReport.getMessageVersion().setV(version);
        oneDayGlskReport.getMessageDateTime().setV(DateTimeUtils.getNowDate());
        oneDayGlskReport.getQualityCheckTimeInterval().setV(requestInterval);
        final String mergingDay = requestInterval.substring(18, 28).replace("-", "");
        final String messageIdentification = String.format("%s-F%d-%02d", mergingDay, FLOW, version);
        IdentificationType identificationType = new IdentificationType();
        identificationType.setV(messageIdentification);
        oneDayGlskReport.setMessageIdentification(identificationType);
    }

    private void saveDailyGlskReportInOutputs(final QualityCheckReport qualityCheckReport, final DailyMergingTask task) {
        final OffsetDateTime mergingDate = OffsetDateTime.parse(qualityCheckReport.getQualityCheckTimeInterval().getV().substring(18, 35), DateTimeFormatter.ISO_DATE_TIME);
        final String qualityReportFileName = OutputUtils.generateOutputFileName(mergingDate, task.getVersion(), MESSAGE_TYPE, DOCUMENT_TYPE, FLOW, XML_EXTENSION);
        final String fileLocation = String.format("/daily-merging/tasks/%d/outputs/glsk-quality-report", task.getId());
        final SavedFile dailyQualityReportSavedFile = FileStorageUtils.save(
                configuration.getDailyOutputsDirectoryPath(task),
                qualityReportFileName,
                fileLocation,
                path -> JaxbUtils.writeToPath(
                        QualityCheckReport.class,
                        qualityCheckReport,
                        path,
                        SchemaLocationNamespace.GLSK_QUALITY_CHECK_XSD.getName(),
                        true
                )
        );
        task.getDailyOutputs().setGlskQualityReport(dailyQualityReportSavedFile);
        repository.save(task);
    }

    private List<QualityCheckReport> getAllResult(final List<MergingTask> tasksList) {
        return tasksList.stream()
                .map(task -> task.getArtifacts().getFile(ArtifactType.GLSK_QUALITY_REPORT))
                .map(savedFile -> JaxbUtils.readFromPath(
                        QualityCheckReport.class,
                        savedFile.getPath()
                ))
                .toList();
    }
}
