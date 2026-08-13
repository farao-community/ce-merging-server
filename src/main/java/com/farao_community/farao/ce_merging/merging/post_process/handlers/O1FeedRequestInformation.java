/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.post_process.handlers;

import com.farao_community.farao.ce_merging.common.chain.Handler;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.util.JaxbUtils;
import com.farao_community.farao.ce_merging.merging.model.daily.entities.DailyTask;
import com.farao_community.farao.ce_merging.merging.post_process.PostProcessRequest;
import com.farao_community.farao.ce_merging.xsd.xnodes.HeaderType;
import com.farao_community.farao.ce_merging.xsd.xnodes.PayloadType;
import com.farao_community.farao.ce_merging.xsd.xnodes.RequestMessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(10)
public class O1FeedRequestInformation implements Handler<PostProcessRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(O1FeedRequestInformation.class);
    private static final String NO_INTERVAL_ERROR = "Error in merging request file : payload is empty or doesn't contain timeInterval";

    @Override
    public boolean handle(final PostProcessRequest request) {
        final DailyTask mergingEntity = request.getDailyCeMergingEntity();

        try {
            final RequestMessageType requestMessageType = JaxbUtils.readFromPath(RequestMessageType.class,
                                                                                 mergingEntity
                                                                             .getDailyInputs()
                                                                             .getMergingRequest()
                                                                             .getPath());

            final String requestTimeInterval = getRequestTimeInterval(requestMessageType);
            request.setRequestTimeInterval(requestTimeInterval);
            request.setMergingDay(request.getEndDateTime());
            final HeaderType requestHeader = requestMessageType.getHeader();
            request.setContext(requestHeader.getContext());
            request.setCorrelationID(requestHeader.getCorrelationID());
            request.setNoun(requestHeader.getNoun());
            request.setReplyAddress(requestHeader.getReplyAddress());
        } catch (final Exception e) {
            LOGGER.error("Error in merging request file '{}' ",
                      mergingEntity
                          .getDailyInputs()
                          .getMergingRequest()
                          .getOriginalName());
            throw new CeMergingException("Error in merging request file", e);
        }

        return false;
    }

    private String getRequestTimeInterval(final RequestMessageType mergingRequest) {
        final PayloadType payloadType = mergingRequest.getPayload();
        if (payloadType != null && !payloadType.getAny().isEmpty()) {
            return payloadType
                .getAny()
                .getFirst()
                .getAttribute("timeInterval");
        } else {
            LOGGER.error(NO_INTERVAL_ERROR);
            throw new CeMergingException(NO_INTERVAL_ERROR);
        }
    }

}
