/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.ce_merging.merging.process.last_load_flow;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.powsybl.loadflow.LoadFlowResult;

public class LoadFlowOutput {
    private final String cgmFileName;
    private boolean loadflowStatus;
    private String loadflowMode;
    private int iterationNumber;
    private String slackNode;
    private double slackCompensation;
    private int componentNumber;
    private double slackNodeGap;

    @JsonCreator
    public LoadFlowOutput(@JsonProperty("cgmFileName") final String cgmFileName,
                          @JsonProperty("loadflowStatus") final boolean loadflowStatus,
                          @JsonProperty("loadflowMode") final String loadflowMode,
                          @JsonProperty("iterationNumber") final int iterationNumber,
                          @JsonProperty("componentNumber") final int componentNumber,
                          @JsonProperty("slackNode") final String slackNode,
                          @JsonProperty("slackCompensation") final double slackCompensation,
                          @JsonProperty("slackNodeGap") final double slackNodeGap) {
        this.cgmFileName = cgmFileName;
        this.loadflowStatus = loadflowStatus;
        this.loadflowMode = loadflowMode;
        this.iterationNumber = iterationNumber;
        this.slackNode = slackNode;
        this.slackCompensation = slackCompensation;
        this.componentNumber = componentNumber;
        this.slackNodeGap = slackNodeGap;
    }

    public LoadFlowOutput(@JsonProperty("cgmFileName") final String cgmFileName) {
        this(cgmFileName, false, "", 0, 0, "", 0.f, 0.f);
    }

    public String getCgmFileName() {
        return cgmFileName;
    }

    public boolean isLoadflowStatus() {
        return loadflowStatus;
    }

    public int getIterationNumber() {
        return iterationNumber;
    }

    public String getSlackNode() {
        return slackNode;
    }

    public double getSlackCompensation() {
        return slackCompensation;
    }

    public void setLoadflowStatus(boolean loadflowStatus) {
        this.loadflowStatus = loadflowStatus;
    }

    public void setIterationNumber(int iterationNumber) {
        this.iterationNumber = iterationNumber;
    }

    public void setSlackNode(String slackNode) {
        this.slackNode = slackNode;
    }

    public void setSlackCompensation(double slackCompensation) {
        this.slackCompensation = slackCompensation;
    }

    public void setComponentNumber(int numcc) {
        this.componentNumber = numcc;
    }

    public int getComponentNumber() {
        return componentNumber;
    }

    public void setSlackNodeGap(double ecartNoeudBilan) {
        this.slackNodeGap = ecartNoeudBilan;
    }

    public double getSlackNodeGap() {
        return slackNodeGap;
    }

    public String getLoadflowMode() {
        return loadflowMode;
    }

    public void setLoadflowMode(String loadflowMode) {
        this.loadflowMode = loadflowMode;
    }

    public static LoadFlowOutput from(final String fileName,
                                      final String loadFlowMode,
                                      final LoadFlowResult loadFlowResult) {
        final LoadFlowOutput loadflowOutput = new LoadFlowOutput(fileName);
        loadflowOutput.setLoadflowStatus(loadFlowResult.isOk());
        final LoadFlowResult.ComponentResult result = loadFlowResult.getComponentResults().getFirst(); // result for the main component
        loadflowOutput.setIterationNumber(result.getIterationCount());
        loadflowOutput.setComponentNumber(result.getConnectedComponentNum()); // default connected component Number: 0 == componentMode: ALL_CONNECTED
        loadflowOutput.setSlackCompensation(result.getDistributedActivePower());
        loadflowOutput.setSlackNode(result.getSlackBusResults().stream().findFirst()
                                            .map(LoadFlowResult.SlackBusResult::getId)
                                            .orElse("SLACK_NODE_NOT_FOUND"));
        loadflowOutput.setSlackNodeGap(result.getSlackBusResults().stream().findFirst()
                                               .map(LoadFlowResult.SlackBusResult::getActivePowerMismatch)
                                               .orElse(0.));
        loadflowOutput.setLoadflowMode(loadFlowMode);

        return loadflowOutput;
    }
}
