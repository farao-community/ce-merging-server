/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.daily_merging.merging_request;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.util.JaxbUtils;
import com.farao_community.farao.ce_merging.daily_merging.entities.DailyMergingTask;

import com.farao_community.farao.ce_merging.xsd.merging_request.HeaderType;
import com.farao_community.farao.ce_merging.xsd.merging_request.PayloadType;
import com.farao_community.farao.ce_merging.xsd.merging_request.RequestMessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MergingRequestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MergingRequestService.class);

    public RequestInformation getMergingRequestInformation(final DailyMergingTask dailyMergingTask) {
        try {
            final RequestMessageType requestMessageType = JaxbUtils.readFromPath(RequestMessageType.class, dailyMergingTask.getDailyInputs().getMergingRequest().getPath());
            final String requestTimeInterval = getRequestTimeInterval(requestMessageType);
            final HeaderType requestHeader = requestMessageType.getHeader();
            return new RequestInformation(
                    requestTimeInterval,
                    requestHeader.getNoun(),
                    requestHeader.getContext(),
                    requestHeader.getReplyAddress(),
                    requestHeader.getCorrelationID()
            );

        } catch (final Exception e) {
            LOGGER.error("Error in merging request file '{}' ", dailyMergingTask.getDailyInputs().getMergingRequest().getOriginalName(), e);
            throw new CeMergingException("Error in merging request file", e);
        }
    }

    private String getRequestTimeInterval(final RequestMessageType mergingRequest) {
        final PayloadType payloadType = mergingRequest.getPayload();
        if (payloadType != null && !payloadType.getAny().isEmpty()) {
            return payloadType.getAny().get(0).getAttribute("timeInterval");
        } else {
            LOGGER.error("Error in merging request file : Payload is empty or doesn't contains timeInterval");
            throw new CeMergingException("Error in merging request file : Payload is empty or doesn't contain timeInterval");
        }
    }
}
