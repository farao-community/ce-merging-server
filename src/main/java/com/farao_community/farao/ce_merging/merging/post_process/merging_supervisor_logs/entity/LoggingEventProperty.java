/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.post_process.merging_supervisor_logs.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "logging_event_property")
public class LoggingEventProperty {

    @Id
    @ManyToOne()
    @JoinColumn(name = "event_id")
    private LoggingEvent loggingEvent;

    @Id
    @Column(name = "mapped_key", nullable = false, columnDefinition = "VARCHAR(254)")
    private String mappedKey;

    @Column(name = "mapped_value", columnDefinition = "LONGVARCHAR")
    private String mappedValue;

    public LoggingEvent getLoggingEvent() {
        return loggingEvent;
    }

    public void setLoggingEvent(final LoggingEvent loggingEvent) {
        this.loggingEvent = loggingEvent;
    }

    public String getMappedKey() {
        return mappedKey;
    }

    public void setMappedKey(final String mappedKey) {
        this.mappedKey = mappedKey;
    }

    public String getMappedValue() {
        return mappedValue;
    }

    public void setMappedValue(final String mappedValue) {
        this.mappedValue = mappedValue;
    }
}
