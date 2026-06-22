/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.model.records;

import jakarta.persistence.Entity;
import jakarta.persistence.Lob;

import java.time.LocalDateTime;

@Entity
public class VirtualHubsConfigurationRecord extends AbstractGridConfigurationRecord {
    @Lob
    private String configurationJson;

    public VirtualHubsConfigurationRecord(final String id,
                                          final LocalDateTime validFrom,
                                          final LocalDateTime validTo,
                                          final LocalDateTime publishedOn,
                                          final String configurationJson) {
        super(id, validFrom, validTo, publishedOn);
        this.configurationJson = configurationJson;
    }

    public VirtualHubsConfigurationRecord() {

    }

    public String getConfigurationJson() {
        return configurationJson;
    }

    public void setConfigurationJson(final String configurationJson) {
        this.configurationJson = configurationJson;
    }
}
