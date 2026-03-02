/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.post_process.handlers;

import ch.iec.tc57._2011.schema.message.HeaderType;
import ch.iec.tc57._2011.schema.message.PayloadType;
import ch.iec.tc57._2011.schema.message.RequestMessageType;
import com.farao_community.farao.ce_merging.common.entities.DailyCoreMergingEntity;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.post_process.PostProcessRequest;
import com.farao_community.farao.ce_merging.common.util.JaxbUtils;
import com.farao_community.farao.ce_merging.common.util.chain.Handler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(0)
@Slf4j
public class O1FeedRequestInformation implements Handler<PostProcessRequest> {

    private static final String NO_INTERVAL_ERROR = "Error in merging request file : Payload is empty or doesn't contains timeInterval";

    @Override
    public boolean handle(final PostProcessRequest request) {
        final DailyCoreMergingEntity mergingEntity = request.getDailyCoreMergingEntity();

        try {
            final RequestMessageType requestMessageType = JaxbUtils.read(RequestMessageType.class,
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
            log.error("Error in merging request file '{}' ",
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
            log.error(NO_INTERVAL_ERROR);
            throw new CeMergingException(NO_INTERVAL_ERROR);
        }
    }

}
