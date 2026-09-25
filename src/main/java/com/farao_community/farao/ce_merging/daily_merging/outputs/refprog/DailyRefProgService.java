/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.ce_merging.daily_merging.outputs.refprog;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.util.DateTimeUtils;
import com.farao_community.farao.ce_merging.common.util.JaxbUtils;
import com.farao_community.farao.ce_merging.daily_merging.DailyMergingRepository;
import com.farao_community.farao.ce_merging.daily_merging.entities.DailyMergingTask;
import com.farao_community.farao.ce_merging.daily_merging.merging_request.RequestInformation;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.Outputs;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.farao_community.farao.ce_merging.xsd.ref_prog.PublicationDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.MERGING_DATE_TIME_END_INDEX;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.MERGING_DAY_START_INDEX;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.XML_EXTENSION;
import static com.farao_community.farao.ce_merging.common.util.OutputUtils.generateOutputFileName;
import static com.farao_community.farao.ce_merging.merging.post_process.common.SchemaLocationNamespace.RESPONSE_XSD;
import static jakarta.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT;
import static jakarta.xml.bind.Marshaller.JAXB_FRAGMENT;
import static jakarta.xml.bind.Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION;
import static java.lang.Boolean.TRUE;
import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toSet;

@Service
public class DailyRefProgService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DailyRefProgService.class);
    private final CeMergingConfiguration configuration;
    private final DailyMergingRepository repository;
    private static final String MESSAGE_TYPE = "";
    private static final String DOCUMENT_TYPE = "A45";
    private static final int REFPROG_FLOW = 101;

    private static final Map<String, Object> JAXB_PROPERTIES = Map.of(JAXB_FORMATTED_OUTPUT, TRUE,
                                                                      JAXB_NO_NAMESPACE_SCHEMA_LOCATION, RESPONSE_XSD.getName(),
                                                                      JAXB_FRAGMENT, TRUE);

    public DailyRefProgService(final CeMergingConfiguration configuration,
                               final DailyMergingRepository repository) {
        this.configuration = configuration;
        this.repository = repository;
    }

    public void computeDailyRefProg(final DailyMergingTask dailyMergingTask,
                                    final List<MergingTask> hourlyTasks,
                                    final RequestInformation requestInformation) {
        try {
            final PublicationDocument dailyRefProg = buildDailyRefProg(dailyMergingTask.getVersion(),
                                                                       hourlyTasks,
                                                                       requestInformation.numberOfPositions());
            saveDailyRefProgInDailyOutputs(dailyMergingTask, dailyRefProg);
        } catch (Exception e) {
            LOGGER.error("Error during creation of daily ref prog file for task '{}' ", dailyMergingTask.getId());
            throw new CeMergingException("Error during creation of daily ref prog file");
        }
    }

    private void saveDailyRefProgInDailyOutputs(final DailyMergingTask dailyTask,
                                                final PublicationDocument dailyRefProg) {

        final OffsetDateTime mergingDay = OffsetDateTime.parse(
                dailyRefProg.getPublicationTimeInterval().getV().substring(MERGING_DAY_START_INDEX, MERGING_DATE_TIME_END_INDEX),
                ISO_DATE_TIME
        );
        final String fileName = generateOutputFileName(mergingDay,
                                                       dailyTask.getVersion(),
                                                       MESSAGE_TYPE,
                                                       DOCUMENT_TYPE,
                                                       REFPROG_FLOW,
                                                       XML_EXTENSION);
        final Path refProgOutPath = Paths.get(configuration.getDailyOutputsDirectoryPath(dailyTask), fileName);
        JaxbUtils.writeToPath(PublicationDocument.class, dailyRefProg, refProgOutPath, JAXB_PROPERTIES);
        final SavedFile dailyRefProgSavedFile = new SavedFile(
                fileName, refProgOutPath.toString(), String.format("/daily-merging/tasks/%d/outputs/ref-prog", dailyTask.getId())
        );
        dailyTask.getDailyOutputs().setRefProg(dailyRefProgSavedFile);
        repository.save(dailyTask);
        LOGGER.info("File '{}' is saved in task '{}' outputs", fileName, dailyTask.getId());
    }

    public PublicationDocument buildDailyRefProg(final int version,
                                                 final List<MergingTask> hourlyTasks,
                                                 final int numberOfPosition) {

        final List<PublicationDocument> hourlyRefProgs = getAllHourlyRefProgs(hourlyTasks);
        checkUniqueRefProgInterval(hourlyRefProgs);
        final PublicationDocument dailyRefProg = hourlyRefProgs.getFirst();
        updateHeader(dailyRefProg, version);

        hourlyRefProgs.stream().skip(1)
                .map(PublicationDocument::getPublicationTimeSeries)
                .flatMap(List::stream)
                .forEach(timeSeries -> addHourlyTimeSeriesToDailyRefProg(timeSeries, dailyRefProg));

        final List<Integer> existingPositions = dailyRefProg.getPublicationTimeSeries()
                .getFirst()
                .getPeriod()
                .getInterval()
                .stream()
                .map(DailyRefProgService::getIntervalPosition)
                .toList();

        for (int position = 1; position < numberOfPosition + 1; position++) {
            if (!existingPositions.contains(position)) {
                addZeroAtPosition(dailyRefProg, position);
            }
        }
        dailyRefProg.getPublicationTimeSeries().forEach(DailyRefProgService::sortIntervalsByPosition);
        return dailyRefProg;
    }

    private static void sortIntervalsByPosition(final PublicationDocument.PublicationTimeSeries timeSeries) {
        timeSeries.getPeriod()
                .getInterval()
                .sort(comparing(DailyRefProgService::getIntervalPosition));
    }

    private void addZeroAtPosition(final PublicationDocument dailyRefProg,
                                   final int position) {
        final PublicationDocument.PublicationTimeSeries.Period.Interval interval = new PublicationDocument.PublicationTimeSeries.Period.Interval();
        final PublicationDocument.PublicationTimeSeries.Period.Interval.Pos pos = new PublicationDocument.PublicationTimeSeries.Period.Interval.Pos();
        final PublicationDocument.PublicationTimeSeries.Period.Interval.Qty qty = new PublicationDocument.PublicationTimeSeries.Period.Interval.Qty();

        pos.setV(BigInteger.valueOf(position));
        interval.setPos(pos);
        qty.setV(BigInteger.valueOf(0));
        interval.setQty(qty);
        dailyRefProg.getPublicationTimeSeries()
                .stream()
                .map(PublicationDocument.PublicationTimeSeries::getPeriod)
                .map(PublicationDocument.PublicationTimeSeries.Period::getInterval)
                .forEach(intervalList -> intervalList.add(interval));
    }

    void checkUniqueRefProgInterval(final List<PublicationDocument> refProgResultsList) {
        final int numberOfDistinctIntervals = refProgResultsList.stream()
                .map(refProgResult -> refProgResult.getPublicationTimeInterval().getV())
                .collect(toSet())
                .size();

        if (numberOfDistinctIntervals != 1) {
            LOGGER.error("Input tasks have not the same publication time interval");
            throw new CeMergingException("Input tasks should have the same publication time interval");
        }
    }

    private void addHourlyTimeSeriesToDailyRefProg(final PublicationDocument.PublicationTimeSeries hourlyTimeSeries,
                                                   final PublicationDocument dailyRefProg) {
        addTimeSeriesIfAbsent(dailyRefProg, hourlyTimeSeries);
        dailyRefProg.getPublicationTimeSeries()
                .stream()
                .filter(dailyTs -> sameAreas(dailyTs, hourlyTimeSeries))
                .forEach(dailyTs -> addHourlyIntervalsToDailyTimeSeries(dailyTs, hourlyTimeSeries));
    }

    private static void addHourlyIntervalsToDailyTimeSeries(final PublicationDocument.PublicationTimeSeries dailyTimeSeries,
                                                            final PublicationDocument.PublicationTimeSeries hourlyTimeSeries) {
        final PublicationDocument.PublicationTimeSeries.Period.Interval hourlyInterval = hourlyTimeSeries.getPeriod().getInterval().getFirst();
        final List<PublicationDocument.PublicationTimeSeries.Period.Interval> dailyIntervals = dailyTimeSeries.getPeriod().getInterval();

        if (isNotInIntervals(hourlyInterval, dailyIntervals)) {
            dailyTimeSeries.getPeriod().getInterval().add(hourlyInterval);
        }
    }

    private static boolean sameAreas(final PublicationDocument.PublicationTimeSeries ts1,
                                     final PublicationDocument.PublicationTimeSeries ts2) {
        return ts1.getInArea().getV().equals(ts2.getInArea().getV())
               && ts1.getOutArea().getV().equals(ts2.getOutArea().getV());
    }

    private void addTimeSeriesIfAbsent(final PublicationDocument dailyRefProg,
                                       final PublicationDocument.PublicationTimeSeries timeSeries) {

        final boolean timeSeriesNotInRefProg = dailyRefProg.getPublicationTimeSeries()
                .stream()
                .map(publication -> publication.getTimeSeriesIdentification().getV())
                .noneMatch(timeSeries.getTimeSeriesIdentification().getV()::equals);

        if (timeSeriesNotInRefProg) {
            dailyRefProg.getPublicationTimeSeries().add(timeSeries);
        }
    }

    private static boolean isNotInIntervals(final PublicationDocument.PublicationTimeSeries.Period.Interval interval,
                                            final List<PublicationDocument.PublicationTimeSeries.Period.Interval> intervals) {
        return intervals.stream()
                .map(DailyRefProgService::getIntervalPosition)
                .noneMatch(dailyPos -> interval.getPos().getV().intValue() == dailyPos);
    }

    private void updateHeader(PublicationDocument refProgResult,
                              int version) {
        final PublicationDocument.CreationDateTime creationDateTime = new PublicationDocument.CreationDateTime();
        creationDateTime.setV(DateTimeUtils.getNowDate());
        refProgResult.setCreationDateTime(creationDateTime);
        final PublicationDocument.DocumentVersion documentVersion = new PublicationDocument.DocumentVersion();
        documentVersion.setV(BigInteger.valueOf(version));
        refProgResult.setDocumentVersion(documentVersion);
        final String docIdentification = String.format("%s-F%d-%02d",
                                                       refProgResult.getDocumentIdentification().getV(),
                                                       REFPROG_FLOW,
                                                       version);
        final PublicationDocument.DocumentIdentification documentIdentification = new PublicationDocument.DocumentIdentification();
        documentIdentification.setV(docIdentification);
        refProgResult.setDocumentIdentification(documentIdentification);
    }

    List<PublicationDocument> getAllHourlyRefProgs(final List<MergingTask> hourlyTasks) {
        return hourlyTasks.stream()
                .map(MergingTask::getOutputs)
                .map(Outputs::getRefProg)
                .map(file -> JaxbUtils.readFromPath(PublicationDocument.class, file.getPath()))
                .toList();
    }

    private static Integer getIntervalPosition(final PublicationDocument.PublicationTimeSeries.Period.Interval interval) {
        return interval.getPos().getV().intValue();
    }

}
