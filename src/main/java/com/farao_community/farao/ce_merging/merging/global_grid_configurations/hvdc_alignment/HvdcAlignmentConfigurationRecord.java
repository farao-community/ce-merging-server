/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 */
package com.farao_community.farao.ce_merging.merging.global_grid_configurations.hvdc_alignment;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    private static <T> List<T> copyList(final List<T> original) {
        return (original != null) ? new ArrayList<>(original) : new ArrayList<>();
    }

    public List<HvdcAlignmentXNodeCoupleDto> getHvdcXNodeAlignmentCouplesDto() {
        return copyList(hvdcXNodeAlignmentCouplesDto);
    }

    public void setHvdcXNodeAlignmentCouplesDto(final List<HvdcAlignmentXNodeCoupleDto> hvdcXNodeAlignmentCouplesDto) {
        this.hvdcXNodeAlignmentCouplesDto = copyList(hvdcXNodeAlignmentCouplesDto);
    }

    public List<ZeroFlowNodeDto> getZeroFlowNodeDtos() {
        return copyList(zeroFlowNodeDtos);
    }

    public void setZeroFlowNodeDtos(final List<ZeroFlowNodeDto> zeroFlowNodeDtos) {
        this.zeroFlowNodeDtos = copyList(zeroFlowNodeDtos);
    }

    public List<String> getDkHvdcXnodes() {
        return copyList(dkHvdcXnodes);
    }

    public void setDkHvdcXnodes(final List<String> dkHvdcXnodes) {
        this.dkHvdcXnodes = copyList(dkHvdcXnodes);
    }
}
