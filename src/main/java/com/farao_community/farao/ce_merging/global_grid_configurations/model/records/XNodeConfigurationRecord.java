/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.model.records;

import com.farao_community.farao.ce_merging.global_grid_configurations.model.dto.XnodeConfigDto;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.FetchType.LAZY;

@Entity
public class XNodeConfigurationRecord extends AbstractGridConfigurationRecord {
    @ElementCollection(fetch = LAZY)
    private List<XnodeConfigDto> xNodeList = new ArrayList<>();

    public XNodeConfigurationRecord(final String id,
                                    final LocalDateTime validFrom,
                                    final LocalDateTime validTo,
                                    final LocalDateTime publishedOn,
                                    final List<XnodeConfigDto> xNodeList) {
        super(id, validFrom, validTo, publishedOn);
        this.xNodeList = xNodeList;
    }

    public XNodeConfigurationRecord() {

    }

    public List<XnodeConfigDto> getXNodeList() {
        return xNodeList;
    }

    public void setXNodeList(final List<XnodeConfigDto> xNodeList) {
        this.xNodeList = xNodeList;
    }
}
