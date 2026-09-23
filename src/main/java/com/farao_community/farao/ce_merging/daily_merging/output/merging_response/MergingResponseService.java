/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.daily_merging.output.merging_response;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.util.DateTimeUtils;
import com.farao_community.farao.ce_merging.common.util.FileStorageUtils;
import com.farao_community.farao.ce_merging.common.util.JaxbUtils;
import com.farao_community.farao.ce_merging.common.util.OutputUtils;
import com.farao_community.farao.ce_merging.daily_merging.DailyMergingRepository;
import com.farao_community.farao.ce_merging.daily_merging.entities.DailyMergingTask;
import com.farao_community.farao.ce_merging.daily_merging.merging_request.RequestInformation;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.farao_community.farao.ce_merging.merging.task.enums.TaskStatus;
import com.farao_community.farao.ce_merging.xsd.merging_request.EventMessageType;
import com.farao_community.farao.ce_merging.xsd.merging_request.ObjectFactory;
import com.farao_community.farao.ce_merging.xsd.merging_response.File;
import com.farao_community.farao.ce_merging.xsd.merging_response.Files;
import com.farao_community.farao.ce_merging.xsd.merging_response.ResponseItem;
import com.farao_community.farao.ce_merging.xsd.merging_response.ResponseItems;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.xml.parsers.ParserConfigurationException;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.JAXB_PROPERTIES;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.XML_EXTENSION;
import static com.farao_community.farao.ce_merging.common.util.DateTimeUtils.toZFormat;
import static com.farao_community.farao.ce_merging.daily_merging.output.common.ResponseUtils.fillResponseHeader;
import static com.farao_community.farao.ce_merging.daily_merging.output.common.ResponseUtils.getResponseElement;

@Service
public class MergingResponseService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MergingResponseService.class);
    private static final String MESSAGE_TYPE = "A18";
    private static final String DOCUMENT_TYPE = "A01";
    private static final int FLOW = 121;
    private static final DocumentInfo REF_PROG = new DocumentInfo("REFPROG", "F101");
    private static final DocumentInfo CGM = new DocumentInfo("CGM", "F100");
    private static final DocumentInfo GLSK_QUALITY = new DocumentInfo("QCHECK_GLSK", "F117");
    private static final DocumentInfo MERGING_LOG = new DocumentInfo("MERGINGLOG", "F123");

    private static final String DOCUMENT_IDENTIFICATION_PREFIX = "documentIdentification://";

    private final CeMergingConfiguration configuration;
    private final DailyMergingRepository repository;

    public MergingResponseService(final CeMergingConfiguration configuration, final DailyMergingRepository repository) {
        this.configuration = configuration;
        this.repository = repository;
    }

    public void computeMergingResponse(final DailyMergingTask dailyTask, final List<MergingTask> hourlyTasks, final RequestInformation requestInformation) {
        try {
            final OffsetDateTime mergingDay = requestInformation.getMergingDay();
            final EventMessageType mergingResponse = buildMergingResponseInformation(hourlyTasks, requestInformation, dailyTask.getVersion());
            saveMergingResponseInDailyOutputs(dailyTask, mergingDay, dailyTask.getVersion(), mergingResponse);
        } catch (final Exception e) {
            LOGGER.error("Error during creation of merging response file for task '{}'", dailyTask.getId(), e);
            throw new CeMergingException("Error during creation of merging response file ", e);
        }
    }

    EventMessageType buildMergingResponseInformation(final List<MergingTask> hourlyTasks, final RequestInformation requestInformation, final int version) throws ParserConfigurationException, JAXBException {
        final EventMessageType response = new EventMessageType();
        response.setHeader(fillResponseHeader(requestInformation, version));
        final ResponseItems responseItems = fillResponseItems(hourlyTasks, requestInformation, version);
        response.setPayload(getResponseElement(responseItems));
        return response;
    }

    public ResponseItems fillResponseItems(final List<MergingTask> hourlyTasks, final RequestInformation requestInformation, final int version) {
        final ResponseItems responseItems = new ResponseItems();
        responseItems.setTimeInterval(requestInformation.requestTimeInterval());
        hourlyTasks.forEach(task -> responseItems.getResponseItem().add(createResponseItem(task, version)));
        addMissingResponseItems(responseItems, requestInformation, version);
        responseItems.getResponseItem().sort(Comparator.comparing(ResponseItem::getTimeInterval));
        return responseItems;
    }

    private void addMissingResponseItems(final ResponseItems responseItems, final RequestInformation requestInformation, final int version) {
        final String identificationDate = DateTimeUtils.formatDate(requestInformation.getEndDateTime());
        final List<ResponseItem> responseItemList = responseItems.getResponseItem();
        requestInformation.buildAllHourlyIntervals().stream()
                .filter(interval -> responseItemList.stream()
                        .map(ResponseItem::getTimeInterval)
                        .noneMatch(interval::equals))
                .forEach(interval -> responseItemList.add(createMissingResponseItem(interval, version, identificationDate)));
    }

    private ResponseItem createMissingResponseItem(final String interval, final int version, final String identificationDate) {
        final ResponseItem responseItem = new ResponseItem();
        responseItem.setTimeInterval(interval);
        final Files files = new Files();
        files.getFile().add(createDocumentFile(REF_PROG, identificationDate, version));
        responseItem.setFiles(files);
        return responseItem;
    }

    private ResponseItem createResponseItem(final MergingTask task, final int version) {
        final ResponseItem responseItem = new ResponseItem();
        final String identificationDate = DateTimeUtils.formatDate(task.getInputs().getTargetDate());
        responseItem.setTimeInterval(createHourlyTimeInterval(task.getInputs().getTargetDate()));
        final File refProgFile = createDocumentFile(REF_PROG, identificationDate, version);
        final Files files = new Files();
        if (task.getStatus() == TaskStatus.SUCCESS) {
            files.getFile().addAll(List.of(
                    createDocumentFile(CGM, identificationDate, version),
                    refProgFile,
                    createDocumentFile(GLSK_QUALITY, identificationDate, version),
                    createDocumentFile(MERGING_LOG, identificationDate, version)
            ));
        } else {
            files.getFile().add(refProgFile);
        }
        responseItem.setFiles(files);
        return responseItem;
    }

    private void saveMergingResponseInDailyOutputs(final DailyMergingTask dailyMergingTask, final OffsetDateTime mergingDay, final int version, EventMessageType response) {
        final String fileName = OutputUtils.generateOutputFileName(mergingDay, version, MESSAGE_TYPE, DOCUMENT_TYPE, FLOW, XML_EXTENSION);
        final String location = String.format("/daily-merging/tasks/%d/outputs/merging-response", dailyMergingTask.getId());
        final SavedFile savedFile = FileStorageUtils.save(
                configuration.getDailyOutputsDirectoryPath(dailyMergingTask),
                fileName,
                location,
                path -> writeMergingResponseToPath(
                        response,
                        path
                )
        );
        dailyMergingTask.getDailyOutputs().setMergingResponse(savedFile);
        repository.save(dailyMergingTask);
    }

    private File createDocumentFile(final DocumentInfo documentInfo, final String identificationDate, final int version) {
        return createFile(
                documentInfo.code(),
                createDocumentIdentification(identificationDate, documentInfo.documentType(), version)
        );
    }

    private void writeMergingResponseToPath(final EventMessageType response, final Path path) {
        final JAXBElement<EventMessageType> eventMessage = new ObjectFactory().createEventMessage(response);
        JaxbUtils.writeToPath(
                eventMessage,
                path,
               JAXB_PROPERTIES
        );
    }

    private File createFile(final String code, final String url) {
        final File file = new File();
        file.setCode(code);
        file.setUrl(url);
        return file;
    }

    private String createDocumentIdentification(final String identificationDate, final String documentType, final int version) {
        return String.format("%s%s-%s-%02d", DOCUMENT_IDENTIFICATION_PREFIX, identificationDate, documentType, version);
    }

    private String createHourlyTimeInterval(final OffsetDateTime targetDate) {
        final OffsetDateTime startDate = targetDate.minusMinutes(targetDate.getMinute());
        return toZFormat(startDate) + "/" + toZFormat(startDate.plusHours(1));
    }

    private record DocumentInfo(String code, String documentType) {
    }
}
