/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.daily_merging.output.cgm;

import com.farao_community.farao.ce_merging.common.exception.ServiceIOException;
import com.farao_community.farao.ce_merging.common.util.DateTimeUtils;
import com.farao_community.farao.ce_merging.daily_merging.ResponseUtils;
import com.farao_community.farao.ce_merging.daily_merging.merging_request.RequestInformation;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.xsd.daily.response.payload.ErrorType;
import com.farao_community.farao.ce_merging.xsd.daily.response.payload.File;
import com.farao_community.farao.ce_merging.xsd.daily.response.payload.Files;
import com.farao_community.farao.ce_merging.xsd.daily.response.payload.ResponseItem;
import com.farao_community.farao.ce_merging.xsd.daily.response.payload.ResponseItems;
import com.farao_community.farao.ce_merging.xsd.merging_request.EventMessageType;
import com.farao_community.farao.ce_merging.xsd.merging_request.HeaderType;
import com.farao_community.farao.ce_merging.xsd.merging_request.PayloadType;
import jakarta.xml.bind.JAXBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.xml.parsers.ParserConfigurationException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.EMPTY;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.FILENAME_DATE_FMT;
import static com.farao_community.farao.ce_merging.common.util.DateTimeUtils.toZFormat;
import static com.farao_community.farao.ce_merging.merging.task.enums.TaskStatus.SUCCESS;
import static java.util.Comparator.comparing;

@Service
public class CgmRecognitionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CgmRecognitionService.class);
    private static final String MESSAGE_ID = "%s-F100-%02d";
    private static final String ERROR_CODE = "1.1";

    byte[] computeCgmRecognition(final RequestInformation requestInformation,
                                 final List<MergingTask> mergingTaskList,
                                 final int version) {
        final EventMessageType mergingResponse;
        try {
            mergingResponse = buildMergingResponseInformation(requestInformation, mergingTaskList, version);
            return ResponseUtils.writeResponseInBytes(mergingResponse);
        } catch (final Exception e) {
            String errorMessage = "Error occurred when creating cgm recognition file";
            LOGGER.error(errorMessage);
            throw new ServiceIOException(errorMessage, e);
        }
    }

    private EventMessageType buildMergingResponseInformation(final RequestInformation requestInformation,
                                                             final List<MergingTask> tasks,
                                                             final int version) throws JAXBException, ParserConfigurationException {
        final EventMessageType response = new EventMessageType();
        final HeaderType responseHeader = fillResponseHeader(requestInformation, version);
        response.setHeader(responseHeader);

        final ResponseItems responseItems = fillResponseItems(requestInformation, tasks);
        final List<ResponseItem> responseItemList = responseItems.getResponseItem();

        requestInformation.findAllIntervals().stream()
                .filter(interval -> responseItemList.stream().map(ResponseItem::getTimeInterval).noneMatch(interval::equals))
                .forEach(interval -> responseItemList.add(fillMissingResponseItemsWithError(interval)));

        responseItemList.sort(comparing(ResponseItem::getTimeInterval));
        final PayloadType responsePayload = ResponseUtils.getResponseElement(responseItems);
        response.setPayload(responsePayload);
        return response;
    }

    private ResponseItem fillMissingResponseItemsWithError(String interval) {
        ResponseItem responseItem = new ResponseItem();
        ErrorType responseItemError = new ErrorType();
        responseItemError.setCode(ERROR_CODE);
        responseItemError.setReason(EMPTY);
        responseItem.setTimeInterval(interval);
        responseItem.setError(responseItemError);
        return responseItem;
    }

    private HeaderType fillResponseHeader(final RequestInformation requestInformation,
                                          final int version) {
        final HeaderType responseHeader = new HeaderType();
        responseHeader.setVerb("created");
        responseHeader.setNoun("CommonGridModel");
        responseHeader.setRevision(Integer.toString(version));
        responseHeader.setContext(requestInformation.context());
        responseHeader.setTimestamp(DateTimeUtils.getNowDate());
        responseHeader.setSource(requestInformation.replyAddress());
        responseHeader.setMessageID(generateMessageId(requestInformation.getMergingDay(), version));
        responseHeader.setCorrelationID(requestInformation.correlationID());
        return responseHeader;
    }

    private String generateMessageId(final OffsetDateTime mergingDateTime,
                                     final int mergingVersion) {
        String mergingDate = FILENAME_DATE_FMT.format(mergingDateTime);
        return String.format(MESSAGE_ID, mergingDate, mergingVersion);
    }

    private ResponseItems fillResponseItems(final RequestInformation mergingRequest,
                                            final List<MergingTask> tasks) {
        final ResponseItems responseItems = new ResponseItems();
        responseItems.setTimeInterval(mergingRequest.requestTimeInterval());

        tasks.stream()
                .map(this::fillResponseItem)
                .forEach(responseItems.getResponseItem()::add);

        return responseItems;
    }

    private ResponseItem fillResponseItem(final MergingTask task) {
        final ResponseItem responseItem = new ResponseItem();
        final OffsetDateTime startDate = task.getInputs().getTargetDate().truncatedTo(ChronoUnit.HOURS);
        final String itemTimeInterval = toZFormat(startDate) + "/" + toZFormat(startDate.plusHours(1));
        if (task.getStatus() == SUCCESS) {
            responseItem.setTimeInterval(itemTimeInterval);
            final File cgmFile = new File();
            cgmFile.setCode("CGM");
            cgmFile.setUrl(String.format("fileName://%s", task.getOutputs().getCgm().getOriginalName()));

            final Files files = new Files();
            files.getFile().add(cgmFile);
            responseItem.setFiles(files);
        } else {
            final ErrorType responseItemError = new ErrorType();
            responseItemError.setCode(ERROR_CODE);
            responseItemError.setReason(task.getStatusDetail());
            responseItem.setTimeInterval(itemTimeInterval);
            responseItem.setError(responseItemError);
        }
        return responseItem;
    }

}
