/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.model.records;

import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.VirtualHubsAlignmentCouple;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.ZeroFlowNode;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.FetchType.LAZY;

@Entity
public class HvdcAlignmentConfigurationRecord {

    @Id
    protected String id;
    protected LocalDateTime validFrom;
    protected LocalDateTime validTo;
    protected LocalDateTime publishedOn;
    @ElementCollection(fetch = LAZY)
    private List<VirtualHubsAlignmentCouple> hvdcXNodeAlignmentCouples = new ArrayList<>();

    @ElementCollection(fetch = LAZY)
    private List<ZeroFlowNode> zeroFlowNodes = new ArrayList<>();

    @ElementCollection(fetch = LAZY)
    private List<String> dkHvdcXnodes = new ArrayList<>();

    private String defaultSlackNode;

    public HvdcAlignmentConfigurationRecord(final String id,
                                            final LocalDateTime validFrom,
                                            final LocalDateTime validTo,
                                            final LocalDateTime publishedOn,
                                            final List<VirtualHubsAlignmentCouple> hvdcXNodeAlignmentCouples,
                                            final List<ZeroFlowNode> zeroFlowNodes,
                                            final List<String> dkHvdcXnodes,
                                            final String defaultSlackNode) {
        this.id = id;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.publishedOn = publishedOn;
        this.hvdcXNodeAlignmentCouples = new ArrayList<>(hvdcXNodeAlignmentCouples);
        this.zeroFlowNodes = new ArrayList<>(zeroFlowNodes);
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

    public List<VirtualHubsAlignmentCouple> getHvdcXNodeAlignmentCouples() {
        return new ArrayList<>(hvdcXNodeAlignmentCouples);
    }

    public void setHvdcXNodeAlignmentCouples(final List<VirtualHubsAlignmentCouple> hvdcXNodeAlignmentCouples) {
        this.hvdcXNodeAlignmentCouples = new ArrayList<>(hvdcXNodeAlignmentCouples);
    }

    public List<ZeroFlowNode> getZeroFlowNodes() {
        return new ArrayList<>(zeroFlowNodes);
    }

    public void setZeroFlowNodes(final List<ZeroFlowNode> zeroFlowNodes) {
        this.zeroFlowNodes = new ArrayList<>(zeroFlowNodes);
    }

    public List<String> getDkHvdcXnodes() {
        return new ArrayList<>(dkHvdcXnodes);
    }

    public void setDkHvdcXnodes(final List<String> dkHvdcXnodes) {
        this.dkHvdcXnodes = new ArrayList<>(dkHvdcXnodes);
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public LocalDateTime getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(final LocalDateTime validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDateTime getValidTo() {
        return validTo;
    }

    public void setValidTo(final LocalDateTime validTo) {
        this.validTo = validTo;
    }

    public LocalDateTime getPublishedOn() {
        return publishedOn;
    }

    public void setPublishedOn(final LocalDateTime publishedOn) {
        this.publishedOn = publishedOn;
    }
}
