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

public class PstOutput implements Serializable {
    @JsonProperty("processNumberDivaca")
    private int processNumberDivaca;
    @JsonProperty("totalTargetFlowDivaca")
    private double totalTargetFlowDivaca;
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

    @JsonProperty("appliedProcedureLipst")
    private Integer appliedProcedureLipst;
    @JsonProperty("targetFlowLipst")
    private double targetFlowLipst;
    @JsonProperty("tapPstLienz")
    private Tap tapPstLienz;
    @JsonProperty("flowLipst")
    private Flow flowLipst;

    @JsonProperty("appliedProcedureNrpst")
    private Integer appliedProcedureNrpst;
    @JsonProperty("targetFlowNrpst21")
    private double targetFlowNrpst21;

    public double getTargetFlowNrpst21() {
        return targetFlowNrpst21;
    }

    public void setTargetFlowNrpst21(double targetFlowNrpst21) {
        this.targetFlowNrpst21 = targetFlowNrpst21;
    }

    public double getTargetFlowNrpst22() {
        return targetFlowNrpst22;
    }

    public void setTargetFlowNrpst22(double targetFlowNrpst22) {
        this.targetFlowNrpst22 = targetFlowNrpst22;
    }

    @JsonProperty("flowNrpst21")
    private Flow flowNrpst21;
    @JsonProperty("tapPstNr21")
    private Tap tapPstNr21;
    @JsonProperty("targetFlowNrpst22")
    private double targetFlowNrpst22;
    @JsonProperty("flowNrpst22")
    private Flow flowNrpst22;
    @JsonProperty("tapPstNr22")
    private Tap tapPstNr22;

    public PstOutput() {
        flowDivacaRedipuglia = new Flow();
        flowDivacaPadriciano = new Flow();
        flowLipst = new Flow();
        flowNrpst21 = new Flow();
        flowNrpst22 = new Flow();
        tapPstDivaca = new Tap();
        tapPstPadriciano = new Tap();
        tapPstLienz = new Tap();
        tapPstNr21 = new Tap();
        tapPstNr22 = new Tap();
    }

    public Tap getTap(final SpecialPst pst) {
        return switch (pst) {
            case LIENZ -> tapPstLienz;
            case DIVACA -> tapPstDivaca;
            case PADRICIANO -> tapPstPadriciano;
            case NAUDERS1 -> tapPstNr21;
            case NAUDERS2 -> tapPstNr22;
        };
    }

    public Flow getFlow(final SpecialPst pst) {
        return switch (pst) {
            case LIENZ -> flowLipst;
            case NAUDERS1 -> flowNrpst21;
            case NAUDERS2 -> flowNrpst22;
            default -> throw new IllegalArgumentException("Call to getFlow is ambiguous for Divača or Padriciano");
        };
    }

    public void setTapIgmFromId(final SpecialPst pst, final String tapId, final Network igm) {
        getTap(pst).setIgmTapFrom(igm.getTwoWindingsTransformer(tapId));
    }

    public void setTapCgmFromId(final SpecialPst pst, final String tapId, final Network cgm) {
         getTap(pst).setCgmTapFrom(cgm.getTwoWindingsTransformer(tapId));
    }

    public Flow getFlowDivacaPadriciano() {
        return flowDivacaPadriciano;
    }

    public Flow getFlowDivacaRedipuglia() {
        return flowDivacaRedipuglia;
    }

    public void setProcessNumberDivaca(int processNumberDivaca) {
        this.processNumberDivaca = processNumberDivaca;
    }

    public void setTotalTargetFlowDivaca(double totalTargetFlowDivaca) {
        this.totalTargetFlowDivaca = totalTargetFlowDivaca;
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

    public Integer getAppliedProcedureLipst() {
        return appliedProcedureLipst;
    }

    public void setAppliedProcedureLipst(Integer appliedProcedureLipst) {
        this.appliedProcedureLipst = appliedProcedureLipst;
    }

    public Integer getAppliedProcedureNrpst() {
        return appliedProcedureNrpst;
    }

    public void setAppliedProcedureNrpst(Integer appliedProcedureNrpst) {
        this.appliedProcedureNrpst = appliedProcedureNrpst;
    }

    public void setFlowLipst(Flow flowLipst) {
        this.flowLipst = flowLipst;
    }

    public Flow getFlowLipst() {
        return flowLipst;
    }

    public void setTapPstLienz(Tap tapPstLienz) {
        this.tapPstLienz = tapPstLienz;
    }

    public void setFlowNrpst21(Flow flowNrpst21) {
        this.flowNrpst21 = flowNrpst21;
    }

    public Flow getFlowNrpst21() {
        return flowNrpst21;
    }

    public void setTapPstNauders21(Tap tapPstNr21) {
        this.tapPstNr21 = tapPstNr21;
    }

    public void setFlowNrpst22(Flow flowNrpst22) {
        this.flowNrpst22 = flowNrpst22;
    }

    public Flow getFlowNrpst22() {
        return flowNrpst22;
    }

    public void setTapPstNauders22(Tap tapPstNr22) {
        this.tapPstNr22 = tapPstNr22;
    }

    public int getProcessNumberDivaca() {
        return processNumberDivaca;
    }

    public double getTotalTargetFlowDivaca() {
        return totalTargetFlowDivaca;
    }

    public double getTargetFlowDivacaPadriciano() {
        return targetFlowDivacaPadriciano;
    }

    public double getTargetFlowDivacaRedipuglia() {
        return targetFlowDivacaRedipuglia;
    }

    public double getTargetFlowLipst() {
        return targetFlowLipst;
    }

    public void setTargetFlowLipst(double targetFlowLipst) {
        this.targetFlowLipst = targetFlowLipst;
    }

}
