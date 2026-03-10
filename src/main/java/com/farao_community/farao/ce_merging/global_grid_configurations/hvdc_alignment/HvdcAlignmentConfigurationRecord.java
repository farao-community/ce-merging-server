/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.hvdc_alignment;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.farao_community.farao.ce_merging.common.util.ListUtils.clonedList;
import static jakarta.persistence.FetchType.LAZY;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Data
public class HvdcAlignmentConfigurationRecord {

    @Id
    private String id;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private LocalDateTime publishedOn;

    @ElementCollection(fetch = LAZY)
    private List<HvdcAlignmentXNodeCoupleDto> hvdcXNodeAlignmentCouplesDto = new ArrayList<>();

    @ElementCollection(fetch = LAZY)
    private List<ZeroFlowNodeDto> zeroFlowNodeDtos = new ArrayList<>();

    @ElementCollection(fetch = LAZY)
    private List<String> dkHvdcXnodes = new ArrayList<>();

    private String defaultSlackNode;

    public List<HvdcAlignmentXNodeCoupleDto> getHvdcXNodeAlignmentCouplesDto() {
        return clonedList(hvdcXNodeAlignmentCouplesDto);
    }

    public void setHvdcXNodeAlignmentCouplesDto(final List<HvdcAlignmentXNodeCoupleDto> hvdcXNodeAlignmentCouplesDto) {
        this.hvdcXNodeAlignmentCouplesDto = clonedList(hvdcXNodeAlignmentCouplesDto);
    }

    public List<ZeroFlowNodeDto> getZeroFlowNodeDtos() {
        return clonedList(zeroFlowNodeDtos);
    }

    public void setZeroFlowNodeDtos(final List<ZeroFlowNodeDto> zeroFlowNodeDtos) {
        this.zeroFlowNodeDtos = clonedList(zeroFlowNodeDtos);
    }

    public List<String> getDkHvdcXnodes() {
        return clonedList(dkHvdcXnodes);
    }

    public void setDkHvdcXnodes(final List<String> dkHvdcXnodes) {
        this.dkHvdcXnodes = clonedList(dkHvdcXnodes);
    }
}
