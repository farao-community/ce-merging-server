/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.pst_special_process.output;

import com.farao_community.farao.ce_merging.merging.process.pst_special_process.SpecialPst;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.powsybl.iidm.network.Network;

import java.io.Serializable;

import static com.farao_community.farao.ce_merging.merging.process.pst_special_process.PstUtils.logProcedure;

public class PstOutput implements Serializable {
    @JsonProperty("processNumber")
    private int processNumber;
    @JsonProperty("totalTargetFlow")
    private double totalTargetFlow;
    @JsonProperty("targetFlowDivacaPadriciano")
    private double targetFlowDivacaPadriciano;
    @JsonProperty("targetFlowDivacaRedipuglia")
    private double targetFlowDivacaRedipuglia;
    @JsonProperty("tapPstDivaca")
    private Tap tapPstDivaca;
    @JsonProperty("tapPstPadriciano")
    private Tap tapPstPadriciano;
    @JsonProperty("flowDivacaPadriciano")
    private Flow flowDivacaPadriciano;
    @JsonProperty("flowDivacaRedipuglia")
    private Flow flowDivacaRedipuglia;

    public PstOutput() {
        flowDivacaRedipuglia = new Flow();
        flowDivacaPadriciano = new Flow();
        tapPstDivaca = new Tap();
        tapPstPadriciano = new Tap();
    }

    public Tap getTap(final SpecialPst pst) {
        return switch (pst) {
            case DIVACA -> tapPstDivaca;
            case PADRICIANO -> tapPstPadriciano;
        };
    }

    public void setAndLogProcedure(final SpecialPst pst,
                                   final int processNumber) {
        this.processNumber = processNumber;
        logProcedure(processNumber, pst.getFullName());
    }

    public void setTapIgmFromId(final SpecialPst pst,
                                final String tapId,
                                final Network igm) {
        getTap(pst).setIgmTapFrom(igm.getTwoWindingsTransformer(tapId));
    }

    public void setTapCgmFromId(final SpecialPst pst,
                                final String tapId,
                                final Network cgm) {
        getTap(pst).setCgmTapFrom(cgm.getTwoWindingsTransformer(tapId));
    }

    public Flow getFlowDivacaPadriciano() {
        return flowDivacaPadriciano;
    }

    public Flow getFlowDivacaRedipuglia() {
        return flowDivacaRedipuglia;
    }

    public void setProcessNumber(int processNumber) {
        this.processNumber = processNumber;
    }

    public void setTotalTargetFlow(double totalTargetFlow) {
        this.totalTargetFlow = totalTargetFlow;
    }

    public void setTargetFlowDivacaPadriciano(double targetFlowDivacaPadriciano) {
        this.targetFlowDivacaPadriciano = targetFlowDivacaPadriciano;
    }

    public void setTargetFlowDivacaRedipuglia(double targetFlowDivacaRedipuglia) {
        this.targetFlowDivacaRedipuglia = targetFlowDivacaRedipuglia;
    }

    public void setTapPstDivaca(Tap tapPstDivaca) {
        this.tapPstDivaca = tapPstDivaca;
    }

    public void setTapPstPadriciano(Tap tapPstPadriciano) {
        this.tapPstPadriciano = tapPstPadriciano;
    }

    public void setFlowDivacaPadriciano(Flow flowDivacaPadriciano) {
        this.flowDivacaPadriciano = flowDivacaPadriciano;
    }

    public void setFlowDivacaRedipuglia(Flow flowDivacaRedipuglia) {
        this.flowDivacaRedipuglia = flowDivacaRedipuglia;
    }

    public int getProcessNumber() {
        return processNumber;
    }

    public double getTotalTargetFlow() {
        return totalTargetFlow;
    }

    public double getTargetFlowDivacaPadriciano() {
        return targetFlowDivacaPadriciano;
    }

    public double getTargetFlowDivacaRedipuglia() {
        return targetFlowDivacaRedipuglia;
    }

}
