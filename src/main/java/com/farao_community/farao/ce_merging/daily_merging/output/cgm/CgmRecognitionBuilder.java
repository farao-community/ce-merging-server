/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.daily_merging.output.cgm;

import com.farao_community.farao.ce_merging.common.util.DateTimeUtils;
import com.farao_community.farao.ce_merging.common.util.JaxbUtils;
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

import javax.xml.parsers.ParserConfigurationException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.EMPTY;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.FILENAME_DATE_FMT;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.RTE_GSR_URL;
import static com.farao_community.farao.ce_merging.common.util.DateTimeUtils.toZFormat;
import static com.farao_community.farao.ce_merging.merging.post_process.common.SchemaLocationNamespace.RESPONSE_XSD;
import static com.farao_community.farao.ce_merging.merging.task.enums.TaskStatus.SUCCESS;
import static jakarta.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT;
import static jakarta.xml.bind.Marshaller.JAXB_FRAGMENT;
import static jakarta.xml.bind.Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION;
import static java.lang.Boolean.TRUE;
import static java.util.Comparator.comparing;

public final class CgmRecognitionBuilder {
    private static final String MESSAGE_ID = "%s-F100-%02d";
    private static final String ERROR_CODE = "1.1";
    private static final Map<String, Object> JAXB_PROPERTIES = Map.of(JAXB_FORMATTED_OUTPUT, TRUE,
                                                                      JAXB_NO_NAMESPACE_SCHEMA_LOCATION, RESPONSE_XSD.getName(),
                                                                      JAXB_FRAGMENT, TRUE);

    private CgmRecognitionBuilder() {
        // no constructor
    }

    public static byte[] computeCgmRecognition(final RequestInformation requestInformation,
                                        final List<MergingTask> hourlyTasks,
                                        final int version) throws JAXBException, ParserConfigurationException {
        final EventMessageType mergingResponse = buildMergingResponseInformation(requestInformation, hourlyTasks, version);
        return JaxbUtils.writeToBytes(EventMessageType.class, mergingResponse, JAXB_PROPERTIES, RTE_GSR_URL, "payload");

    }

    private static EventMessageType buildMergingResponseInformation(final RequestInformation requestInformation,
                                                             final List<MergingTask> hourlyTasks,
                                                             final int version) throws JAXBException, ParserConfigurationException {
        final EventMessageType response = new EventMessageType();
        final HeaderType responseHeader = fillResponseHeader(requestInformation, version);
        response.setHeader(responseHeader);

        final ResponseItems responseItems = fillResponseItems(requestInformation, hourlyTasks);
        final List<ResponseItem> responseItemList = responseItems.getResponseItem();

        requestInformation.buildAllHourlyIntervals().stream()
                .filter(interval -> responseItemList.stream().map(ResponseItem::getTimeInterval).noneMatch(interval::equals))
                .forEach(interval -> responseItemList.add(fillMissingResponseItemsWithError(interval)));

        responseItemList.sort(comparing(ResponseItem::getTimeInterval));
        final PayloadType responsePayload = ResponseUtils.getResponseElement(responseItems);
        response.setPayload(responsePayload);
        return response;
    }

    private static ResponseItem fillMissingResponseItemsWithError(final String interval) {
        final ResponseItem responseItem = new ResponseItem();
        final ErrorType responseItemError = new ErrorType();
        responseItemError.setCode(ERROR_CODE);
        responseItemError.setReason(EMPTY);
        responseItem.setTimeInterval(interval);
        responseItem.setError(responseItemError);
        return responseItem;
    }

    private static HeaderType fillResponseHeader(final RequestInformation requestInformation,
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

    private static String generateMessageId(final OffsetDateTime mergingDateTime,
                                     final int mergingVersion) {
        final String mergingDate = FILENAME_DATE_FMT.format(mergingDateTime);
        return String.format(MESSAGE_ID, mergingDate, mergingVersion);
    }

    private static ResponseItems fillResponseItems(final RequestInformation mergingRequest,
                                            final List<MergingTask> tasks) {
        final ResponseItems responseItems = new ResponseItems();
        responseItems.setTimeInterval(mergingRequest.requestTimeInterval());

        tasks.stream()
                .map(CgmRecognitionBuilder::fillResponseItem)
                .forEach(responseItems.getResponseItem()::add);

        return responseItems;
    }

    private static ResponseItem fillResponseItem(final MergingTask task) {
        final ResponseItem responseItem = new ResponseItem();
        final String itemTimeInterval = getTaskTimeInterval(task);
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

    private static String getTaskTimeInterval(final MergingTask task) {
        final OffsetDateTime startDate =
                task.getInputs().getTargetDate().truncatedTo(ChronoUnit.HOURS);

        return toZFormat(startDate) + "/" + toZFormat(startDate.plusHours(1));
    }

}
