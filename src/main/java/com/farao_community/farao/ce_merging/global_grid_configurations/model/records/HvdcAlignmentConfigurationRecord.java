/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.model.records;

import com.farao_community.farao.ce_merging.global_grid_configurations.model.dto.VirtualHubsAlignmentCoupleDto;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.dto.ZeroFlowNodeDto;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.FetchType.LAZY;

@Entity
public class HvdcAlignmentConfigurationRecord extends AbstractGridConfigurationRecord {
    @ElementCollection(fetch = LAZY)
    private List<VirtualHubsAlignmentCoupleDto> hvdcXNodeAlignmentCouplesDto = new ArrayList<>();

    @ElementCollection(fetch = LAZY)
    private List<ZeroFlowNodeDto> zeroFlowNodeDtos = new ArrayList<>();

    @ElementCollection(fetch = LAZY)
    private List<String> dkHvdcXnodes = new ArrayList<>();

    private String defaultSlackNode;

    public HvdcAlignmentConfigurationRecord(final String id,
                                            final LocalDateTime validFrom,
                                            final LocalDateTime validTo,
                                            final LocalDateTime publishedOn,
                                            final List<VirtualHubsAlignmentCoupleDto> hvdcXNodeAlignmentCouplesDto,
                                            final List<ZeroFlowNodeDto> zeroFlowNodeDtos,
                                            final List<String> dkHvdcXnodes,
                                            final String defaultSlackNode) {
        super(id, validFrom, validTo, publishedOn);
        this.hvdcXNodeAlignmentCouplesDto = hvdcXNodeAlignmentCouplesDto;
        this.zeroFlowNodeDtos = zeroFlowNodeDtos;
        this.dkHvdcXnodes = dkHvdcXnodes;
        this.defaultSlackNode = defaultSlackNode;
    }

    public HvdcAlignmentConfigurationRecord() {
    }

    public String getDefaultSlackNode() {
        return defaultSlackNode;
    }

    public void setDefaultSlackNode(final String defaultSlackNode) {
        this.defaultSlackNode = defaultSlackNode;
    }

    public List<VirtualHubsAlignmentCoupleDto> getHvdcXNodeAlignmentCouplesDto() {
        return new ArrayList<>(hvdcXNodeAlignmentCouplesDto);
    }

    public void setHvdcXNodeAlignmentCouplesDto(final List<VirtualHubsAlignmentCoupleDto> hvdcXNodeAlignmentCouplesDto) {
        this.hvdcXNodeAlignmentCouplesDto = new ArrayList<>(hvdcXNodeAlignmentCouplesDto);
    }

    public List<ZeroFlowNodeDto> getZeroFlowNodeDtos() {
        return new ArrayList<>(zeroFlowNodeDtos);
    }

    public void setZeroFlowNodeDtos(final List<ZeroFlowNodeDto> zeroFlowNodeDtos) {
        this.zeroFlowNodeDtos = new ArrayList<>(zeroFlowNodeDtos);
    }

    public List<String> getDkHvdcXnodes() {
        return new ArrayList<>(dkHvdcXnodes);
    }

    public void setDkHvdcXnodes(final List<String> dkHvdcXnodes) {
        this.dkHvdcXnodes = new ArrayList<>(dkHvdcXnodes);
    }
}
