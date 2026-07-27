/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.task.entities;

import com.farao_community.farao.ce_merging.common.util.JsonUtils;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.BecByBoundary;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.RegionConfiguration;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.VirtualHubsAlignmentCouple;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.XnodeConfig;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.ZeroFlowNode;
import com.farao_community.farao.ce_merging.merging.process.recessivity.RecessivityParameters;
import com.powsybl.loadflow.LoadFlowParameters;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.LAZY;

/**
 * WARNING: this class is used by the merging supervisor (EMERGE).
 * Please contact them to check compatibility if any modification is needed
 */
@Embeddable
public class Configurations implements Serializable {
    public static final String RECESSIVITY_DEFAULT_CONFIGURATION = "gridDefaultConfigurations/default-recessivity-parameters.json";

    private static final Logger LOGGER = LoggerFactory.getLogger(Configurations.class);

    @ElementCollection(fetch = LAZY)
    private List<VirtualHubRecord> virtualHubList = new ArrayList<>();
    @ElementCollection(fetch = LAZY)
    private List<BorderDirectionRecord> borderDirectionRecords = new ArrayList<>();
    @OneToOne(cascade = ALL)
    private SavedFile dcLoadFlowParameters = new SavedFile();
    @OneToOne(cascade = ALL)
    private SavedFile acLoadFlowParameters = new SavedFile();
    @OneToOne(cascade = ALL)
    private SavedFile basecaseImprovementParameters = new SavedFile();
    @OneToOne(cascade = ALL)
    private SavedFile balancesAdjustmentParameters = new SavedFile();
    @ElementCollection(fetch = LAZY)
    private List<XnodeConfig> xnodeList = new ArrayList<>();
    @OneToOne(cascade = ALL)
    private SavedFile recessivityParameters = new SavedFile();
    @OneToMany(cascade = ALL)
    private List<BecByBoundary> becMatrixConfig = new ArrayList<>();
    @OneToOne(cascade = ALL)
    private RegionConfiguration regionConfiguration;
    @ElementCollection(fetch = LAZY)
    private List<VirtualHubsAlignmentCouple> virtualHubsAlignmentCouples = new ArrayList<>();
    @ElementCollection(fetch = LAZY)
    private List<ZeroFlowNode> zeroFlowNodes = new ArrayList<>();
    @ElementCollection(fetch = LAZY)
    private List<String> dkHvdcXnodes = new ArrayList<>();

    private String defaultSlackNode;

    private transient LoadFlowParameters loadFlowParameters;

    public List<String> getOrDefaultRecessiveCountries() {
        try {
            final RecessivityParameters params = JsonUtils.read(RecessivityParameters.class,
                                                                recessivityParameters.getPath());
            LOGGER.info("Recessive countries list is retrieved from {} file", recessivityParameters.getPath());
            return params.getRecessiveCountries();
        } catch (final Exception e) {
            try {
                LOGGER.warn("Recessive countries list is retrieved from default configuration file");
                RecessivityParameters params = JsonUtils.read(RecessivityParameters.class,
                                                              new ClassPathResource(RECESSIVITY_DEFAULT_CONFIGURATION)
                                                                  .getInputStream());
                return params.getRecessiveCountries();
            } catch (final Exception ex) {
                LOGGER.warn("Error while reading default recessivity configuration file, no country will be considered recessive");
                return new ArrayList<>();
            }
        }
    }

    // tagged falsely unused in IntelliJ - used for configuration deserialization

    public void setAcLoadFlowParametersFilePath(final String filePath) {
        acLoadFlowParameters.feedPathAndName(filePath);
    }

    public void setBalancesAdjustmentParametersFilePath(final String filePath) {
        balancesAdjustmentParameters.feedPathAndName(filePath);
    }

    public void  setBasecaseImprovementParametersFilePath(final String filePath) {
        basecaseImprovementParameters.feedPathAndName(filePath);
    }

    public void setRecessivityParametersFilePath(final String filePath) {
        recessivityParameters.feedPathAndName(filePath);
    }

    public void setDcLoadFlowParametersFilePath(final String filePath) {
        dcLoadFlowParameters.feedPathAndName(filePath);
    }

    // usual accessors

    public List<VirtualHubRecord> getVirtualHubList() {
        return virtualHubList;
    }

    public void setVirtualHubList(final List<VirtualHubRecord> virtualHubList) {
        this.virtualHubList = virtualHubList;
    }

    public List<BorderDirectionRecord> getBorderDirectionRecords() {
        return borderDirectionRecords;
    }

    public void setBorderDirectionRecords(final List<BorderDirectionRecord> borderDirectionRecords) {
        this.borderDirectionRecords = borderDirectionRecords;
    }

    public SavedFile getDcLoadFlowParameters() {
        return dcLoadFlowParameters;
    }

    public void setDcLoadFlowParameters(final SavedFile dcLoadFlowParameters) {
        this.dcLoadFlowParameters = dcLoadFlowParameters;
    }

    public SavedFile getAcLoadFlowParameters() {
        return acLoadFlowParameters;
    }

    public void setAcLoadFlowParameters(final SavedFile acLoadFlowParameters) {
        this.acLoadFlowParameters = acLoadFlowParameters;
    }

    public SavedFile getBasecaseImprovementParameters() {
        return basecaseImprovementParameters;
    }

    public void setBasecaseImprovementParameters(final SavedFile basecaseImprovementParameters) {
        this.basecaseImprovementParameters = basecaseImprovementParameters;
    }

    public SavedFile getBalancesAdjustmentParameters() {
        return balancesAdjustmentParameters;
    }

    public void setBalancesAdjustmentParameters(final SavedFile balancesAdjustmentParameters) {
        this.balancesAdjustmentParameters = balancesAdjustmentParameters;
    }

    public List<XnodeConfig> getXnodeList() {
        return xnodeList;
    }

    public void setXnodeList(final List<XnodeConfig> xnodeList) {
        this.xnodeList = xnodeList;
    }

    public SavedFile getRecessivityParameters() {
        return recessivityParameters;
    }

    public void setRecessivityParameters(final SavedFile recessivityParameters) {
        this.recessivityParameters = recessivityParameters;
    }

    public List<String> getDkHvdcXnodes() {
        return dkHvdcXnodes;
    }

    public void setDkHvdcXnodes(final List<String> dkHvdcXnodes) {
        this.dkHvdcXnodes = dkHvdcXnodes;
    }

    public String getDefaultSlackNode() {
        return defaultSlackNode;
    }

    public void setDefaultSlackNode(final String defaultSlackNode) {
        this.defaultSlackNode = defaultSlackNode;
    }

    public LoadFlowParameters getLoadFlowParameters() {
        return loadFlowParameters;
    }

    public void setLoadFlowParameters(final LoadFlowParameters loadFlowParameters) {
        this.loadFlowParameters = loadFlowParameters;
    }

    public List<ZeroFlowNode> getZeroFlowNodes() {
        return zeroFlowNodes;
    }

    public void setZeroFlowNodes(final List<ZeroFlowNode> zeroFlowNodes) {
        this.zeroFlowNodes = zeroFlowNodes;
    }

    public List<VirtualHubsAlignmentCouple> getVirtualHubsAlignmentCouples() {
        return virtualHubsAlignmentCouples;
    }

    public void setVirtualHubsAlignmentCouples(final List<VirtualHubsAlignmentCouple> virtualHubsAlignmentCouples) {
        this.virtualHubsAlignmentCouples = virtualHubsAlignmentCouples;
    }

    public RegionConfiguration getRegionConfiguration() {
        return regionConfiguration;
    }

    public void setRegionConfiguration(final RegionConfiguration regionConfiguration) {
        this.regionConfiguration = regionConfiguration;
    }

    public List<BecByBoundary> getBecMatrixConfig() {
        return becMatrixConfig;
    }

    public void setBecMatrixConfig(final List<BecByBoundary> becMatrixConfig) {
        this.becMatrixConfig = becMatrixConfig;
    }
}
