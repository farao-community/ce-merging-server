///*
// * Copyright (c) 2026, RTE (http://www.rte-france.com)
// * This Source Code Form is subject to the terms of the Mozilla Public
// * License, v. 2.0. If a copy of the MPL was not distributed with this
// * file, You can obtain one at http://mozilla.org/MPL/2.0/.
// */
//package com.farao_community.farao.ce_merging.merging.post_process.ref_prog;
//
//import com.farao_community.farao.ce_merging.common.util.OutputUtils;
//import com.farao_community.farao.ce_merging.merging.post_process.common.SchemaLocationNamespace;
//import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
//import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
//import com.farao_community.farao.ce_merging.common.exception.ServiceIOException;
//import com.farao_community.farao.ce_merging.common.util.JaxbUtils;
//import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
//import com.farao_community.farao.ce_merging.DailyCoreMergingRepository;
//import com.farao_community.farao.ce_merging.entities.DailyCoreMergingEntity;
//import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
//import com.farao_community.farao.ce_merging.post_process.merging_request.RequestInformation;
//import com.farao_community.farao.ce_merging.xsd.ref_prog.PublicationDocument;
//import jakarta.xml.bind.JAXBContext;
//import jakarta.xml.bind.JAXBException;
//import jakarta.xml.bind.Marshaller;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.stereotype.Service;
//
//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.OutputStream;
//import java.io.StringWriter;
//import java.math.BigInteger;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.time.Duration;
//import java.time.OffsetDateTime;
//import java.time.format.DateTimeFormatter;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.stream.Collectors;
//
///**
// * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
// * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
// * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
// */
//@Service
//public class OneDayRefProgBuilder {
//    private static final Logger LOGGER = LoggerFactory.getLogger(OneDayRefProgBuilder.class);
//    private final CeMergingConfiguration configuration;
//    private final DailyCoreMergingRepository dailyCoreMergingRepository;
//    private static final String MESSAGE_TYPE = "";
//    private static final String DOCUMENT_TYPE = "A45";
//    private static final int FLOW = 101;
//    private static final String XML_EXTENSION = "xml";
//
//    public OneDayRefProgBuilder(final CeMergingConfiguration configuration, final DailyCoreMergingRepository dailyCoreMergingRepository) {
//        this.configuration = configuration;
//        this.dailyCoreMergingRepository = dailyCoreMergingRepository;
//
//    }
//
//    public void computeOneDayRefProg(final DailyCoreMergingEntity dailyCoreMergingEntity, final List<MergingTask> tasksList, final RequestInformation requestInformation) {
//        try {
//            final OffsetDateTime startDate = requestInformation.getStartDateTime();
//            final OffsetDateTime endDate = requestInformation.getEndDateTime();
//            final int numberOfPosition = calculateNumberofPositionforTheDay(startDate, endDate);
//            final PublicationDocument oneDayRefProg = buildOneDayRefProg(dailyCoreMergingEntity.getVersion(), tasksList, numberOfPosition);
//            saveDailyRefProgInDailyOutputs(dailyCoreMergingEntity, oneDayRefProg);
//        } catch (Exception e) {
//            LOGGER.error("Error during creation of daily ref prog file for task '{}' ", dailyCoreMergingEntity.getDailyMergingTaskId());
//            throw new CeMergingException("Error during creation of daily ref prog file");
//        }
//    }
//
//    private int calculateNumberofPositionforTheDay(final OffsetDateTime startDate, final OffsetDateTime endDate) {
//        return (int) Duration.between(startDate, endDate).toHours();
//    }
//
//    private void saveDailyRefProgInDailyOutputs(final DailyCoreMergingEntity dailyCoreMergingEntity, final PublicationDocument oneDayRefProg) {
//
//        final OffsetDateTime mergingDay = OffsetDateTime.parse(oneDayRefProg.getPublicationTimeInterval().getV().substring(18, 35), DateTimeFormatter.ISO_DATE_TIME);
//        final String refProgOutputFileName = OutputUtils.generateOutputFileName(mergingDay, dailyCoreMergingEntity.getVersion(), MESSAGE_TYPE, DOCUMENT_TYPE, FLOW, XML_EXTENSION);
//        final Path filePath = Paths.get(configuration.getDailyOutputsDirectoryPath(dailyCoreMergingEntity), refProgOutputFileName);
//        writeInPath(oneDayRefProg, filePath);
//        final SavedFile dailyRefProgSavedFile = new SavedFile(refProgOutputFileName, filePath.toString(), String.format("/daily-merging/tasks/%d/outputs/ref-prog", dailyCoreMergingEntity.getDailyMergingTaskId()));
//        dailyCoreMergingEntity.getDailyOutputs().setRefProg(dailyRefProgSavedFile);
//        dailyCoreMergingRepository.save(dailyCoreMergingEntity);
//        LOGGER.info("File '{}' is saved in task '{}' outputs", refProgOutputFileName, dailyCoreMergingEntity.getDailyMergingTaskId());
//    }
//
//    public PublicationDocument buildOneDayRefProg(final int version, final List<MergingTask> tasksList, final int numberOfPosition) {
//        final List<PublicationDocument> refProgResultsList = getAllRefProgResult(tasksList);
//        checkThatAllTasksAreInTheSameInterval(refProgResultsList);
//        final PublicationDocument oneDayRefProg = refProgResultsList.get(0);
//        updateHeader(oneDayRefProg, version);
//        for (int i = 1; i < refProgResultsList.size(); i++) {
//            final PublicationDocument publicationDocumentElement = refProgResultsList.get(i);
//            publicationDocumentElement.getPublicationTimeSeries().forEach(timeSeries -> addPublicationTimeSeriesToOneDayRefProg(timeSeries, oneDayRefProg));
//        }
//        //find positions with quantity 0 to add
//        final List<Integer> listExistingPos = oneDayRefProg.getPublicationTimeSeries().get(0).getPeriod().getInterval().stream().map(interval -> interval.getPos().getV().intValue()).collect(Collectors.toList());
//        for (int position = 1; position < numberOfPosition + 1; position++) {
//            if (!listExistingPos.contains(position)) {
//                fillMissingPosition(oneDayRefProg, position);
//            }
//        }
//        //sort intervals
//        oneDayRefProg.getPublicationTimeSeries().forEach(publicationTimeSeries -> publicationTimeSeries.getPeriod().getInterval().sort((interval, interval2) -> interval.getPos().getV().compareTo(interval2.getPos().getV())));
//        return oneDayRefProg;
//    }
//
//    private void fillMissingPosition(final PublicationDocument oneDayRefProg, final int position) {
//        final PublicationDocument.PublicationTimeSeries.Period.Interval interval = new PublicationDocument.PublicationTimeSeries.Period.Interval();
//        final PublicationDocument.PublicationTimeSeries.Period.Interval.Pos pos = new PublicationDocument.PublicationTimeSeries.Period.Interval.Pos();
//        pos.setV(BigInteger.valueOf(position));
//        interval.setPos(pos);
//        final PublicationDocument.PublicationTimeSeries.Period.Interval.Qty qty = new PublicationDocument.PublicationTimeSeries.Period.Interval.Qty();
//        qty.setV(BigInteger.valueOf(0));
//        interval.setQty(qty);
//        oneDayRefProg.getPublicationTimeSeries().forEach(timeSeries -> timeSeries.getPeriod().getInterval().add(interval));
//    }
//
//    void checkThatAllTasksAreInTheSameInterval(final List<PublicationDocument> refProgResultsList) {
//        int publicationTimeIntervalNumber = refProgResultsList.stream().map(refProgResult -> refProgResult.getPublicationTimeInterval().getV()).collect(Collectors.toSet()).size();
//        if (publicationTimeIntervalNumber != 1) {
//            LOGGER.error("Input tasks have not the same Publication time interval");
//            throw new CeMergingException("Input tasks should have the same Publication time interval");
//        }
//    }
//
//    private void addPublicationTimeSeriesToOneDayRefProg(final PublicationDocument.PublicationTimeSeries timeSeries, final PublicationDocument oneDayRefProg) {
//        addNewPublicationTimeSeriesInOneDayRefProg(oneDayRefProg, timeSeries);
//        for (PublicationDocument.PublicationTimeSeries oneDayTimeSeries : oneDayRefProg.getPublicationTimeSeries()) {
//            if (oneDayTimeSeries.getInArea().getV().equals(timeSeries.getInArea().getV())
//                    && oneDayTimeSeries.getOutArea().getV().equals(timeSeries.getOutArea().getV())) {
//
//                final PublicationDocument.PublicationTimeSeries.Period.Interval interval = timeSeries.getPeriod().getInterval().get(0);
//                final List<PublicationDocument.PublicationTimeSeries.Period.Interval> intervals = oneDayTimeSeries.getPeriod().getInterval();
//                boolean flag = checkPositionNotPresentInInterval(interval.getPos().getV().intValue(), intervals);
//                if (!flag) {
//                    oneDayTimeSeries.getPeriod().getInterval().add(interval);
//                }
//            }
//        }
//    }
//
//    private void addNewPublicationTimeSeriesInOneDayRefProg(final PublicationDocument oneDayRefProg, final PublicationDocument.PublicationTimeSeries timeSeries) {
//        final List<String> timeSeriesIdentificationList = oneDayRefProg.getPublicationTimeSeries().stream().map(publication -> publication.getTimeSeriesIdentification().getV()).collect(Collectors.toList());
//        if (!timeSeriesIdentificationList.contains(timeSeries.getTimeSeriesIdentification().getV())) {
//            oneDayRefProg.getPublicationTimeSeries().add(timeSeries);
//        }
//    }
//
//    private boolean checkPositionNotPresentInInterval(final Integer position, final List<PublicationDocument.PublicationTimeSeries.Period.Interval> intervals) {
//        final List<Integer> positionsList = new ArrayList<>();
//        intervals.forEach(interval -> positionsList.add(interval.getPos().getV().intValue()));
//        return positionsList.contains(position);
//    }
//
//    private void updateHeader(final PublicationDocument refProgResult, final int version) {
//        final PublicationDocument.CreationDateTime creationDateTime = new PublicationDocument.CreationDateTime();
//        creationDateTime.setV(OutputUtils.getXMLGregorianCurrentTime());
//        refProgResult.setCreationDateTime(creationDateTime);
//        final PublicationDocument.DocumentVersion documentVersion = new PublicationDocument.DocumentVersion();
//        documentVersion.setV(BigInteger.valueOf(version));
//        refProgResult.setDocumentVersion(documentVersion);
//        String docIdentification = String.format("%s-F%d-%02d", refProgResult.getDocumentIdentification().getV(), FLOW, version);
//        final PublicationDocument.DocumentIdentification documentIdentification = new PublicationDocument.DocumentIdentification();
//        documentIdentification.setV(docIdentification);
//        refProgResult.setDocumentIdentification(documentIdentification);
//    }
//
//    List<PublicationDocument> getAllRefProgResult(final List<MergingTask> tasksList) {
//        final List<SavedFile> finalRefProgResultSavedFileList = new ArrayList<>();
//        final List<PublicationDocument> finalRefProgResultList = new ArrayList<>();
//        tasksList.forEach(taskEntity -> finalRefProgResultSavedFileList.add(taskEntity.getOutputs().getRefProg()));
//        finalRefProgResultSavedFileList.forEach(finalRefProgResultSavedFile -> {
//            PublicationDocument finalRefProgResult = JaxbUtils.readFromPath(PublicationDocument.class, finalRefProgResultSavedFile.getPath());
//            finalRefProgResultList.add(finalRefProgResult);
//
//        });
//        return finalRefProgResultList;
//    }
//
//    private static void writeInPath(final PublicationDocument publicationDocument, final Path filePath) {
//        try (OutputStream os = new FileOutputStream(new File(filePath.toString()))) {
//            final JAXBContext context = JAXBContext.newInstance(PublicationDocument.class);
//            final Marshaller marshaller = context.createMarshaller();
//            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
//            marshaller.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, SchemaLocationNamespace.REFPROG_XSD.getName());
//            // remove remove original header containing "standalone=yes"
//            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
//            // set a new header without "standalone=yes"
//            StringWriter writer = new StringWriter();
//            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
//            marshaller.marshal(publicationDocument, writer);
//            byte[] bytes = writer.toString().replace("xmlns=\"refprog\"", "").getBytes();
//            os.write(bytes);
//        } catch (JAXBException | IOException e) {
//            String errorMessage = "Error occurred when writing content of object in Ref prog xml document";
//            LOGGER.error(errorMessage);
//            throw new ServiceIOException(errorMessage, e);
//        }
//    }
//}
