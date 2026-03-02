/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common;

import java.time.OffsetDateTime;

import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;

public interface GenericMergingRequest {

    default OffsetDateTime getStartDateTime() {
        return OffsetDateTime.parse(getRequestTimeInterval().substring(0, 17),
                                    ISO_DATE_TIME);
    }

    default OffsetDateTime getEndDateTime() {
        return OffsetDateTime.parse(getRequestTimeInterval().substring(18, 35),
                                    ISO_DATE_TIME);
    }

    String getNoun();

    void setNoun(final String noun);

    String getRequestTimeInterval();

    void setRequestTimeInterval(final String requestTimeInterval);

    OffsetDateTime getMergingDay();

    void setMergingDay(final OffsetDateTime mergingDay);

    String getContext();

    void setContext(final String context);

    String getReplyAddress();

    void setReplyAddress(final String replyAddress);

    String getCorrelationID();

    void setCorrelationID(final String correlationID);
}
