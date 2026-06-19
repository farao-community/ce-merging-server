/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.model.xnodes;

import com.farao_community.farao.ce_merging.global_grid_configurations.model.GridConfigurationRecord;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.FetchType.LAZY;

@Entity
public class XNodeConfigurationRecord extends GridConfigurationRecord {
    @ElementCollection(fetch = LAZY)
    private List<XnodeDto> xNodeList = new ArrayList<>();

    public XNodeConfigurationRecord(final String id,
                                    final LocalDateTime validFrom,
                                    final LocalDateTime validTo,
                                    final LocalDateTime publishedOn,
                                    final List<XnodeDto> xNodeList) {
        this.id = id;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.publishedOn = publishedOn;
        this.xNodeList = xNodeList;
    }

    public XNodeConfigurationRecord() {

    }

    public List<XnodeDto> getXNodeList() {
        return xNodeList;
    }

    public void setXNodeList(final List<XnodeDto> xNodeList) {
        this.xNodeList = xNodeList;
    }
}
