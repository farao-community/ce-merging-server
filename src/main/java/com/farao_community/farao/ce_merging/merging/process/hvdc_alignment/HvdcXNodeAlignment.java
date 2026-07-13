/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.hvdc_alignment;

import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.VirtualHubsAlignmentCouple;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Network;
import com.powsybl.ucte.network.UcteElementStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.powsybl.ucte.converter.util.UcteConverterConstants.IS_COUPLER_PROPERTY_KEY;

final class HvdcXNodeAlignment {

    private static final Logger LOGGER = LoggerFactory.getLogger(HvdcXNodeAlignment.class);
    private static final List<UcteElementStatus> IN_OPERATION = Arrays.asList(UcteElementStatus.REAL_ELEMENT_IN_OPERATION, UcteElementStatus.EQUIVALENT_ELEMENT_IN_OPERATION, UcteElementStatus.BUSBAR_COUPLER_IN_OPERATION);

    private final Network referenceNetwork;
    private final Network recessiveNetwork;
    private final VirtualHubsAlignmentCouple hvdcAlignmentXNodeCouple;

    private HvdcXNodeAlignment(Network referenceNetwork, Network recessiveNetwork, VirtualHubsAlignmentCouple hvdcAlignmentXNodeCouple) {
        this.referenceNetwork = referenceNetwork;
        this.recessiveNetwork = recessiveNetwork;
        this.hvdcAlignmentXNodeCouple = hvdcAlignmentXNodeCouple;
    }

    public static HvdcXNodeAlignment on(final Network referenceNetwork, final Network recessiveNetwork, final VirtualHubsAlignmentCouple hvdcAlignmentXNodeCouple) {
        return new HvdcXNodeAlignment(referenceNetwork, recessiveNetwork, hvdcAlignmentXNodeCouple);
    }

    void apply() {
        Optional<DanglingLine> referenceDanglingLineOpt = findDanglingLine(referenceNetwork, hvdcAlignmentXNodeCouple.getReferenceXNode());
        if (referenceDanglingLineOpt.isEmpty()) {
            LOGGER.warn("Could not apply HVDC alignment, dangling line for reference node {} not found",
                    hvdcAlignmentXNodeCouple.getReferenceXNode());
            return;
        }

        Optional<DanglingLine> recessiveDanglingLineOpt = findDanglingLine(recessiveNetwork, hvdcAlignmentXNodeCouple.getRecessiveXNode());
        if (recessiveDanglingLineOpt.isEmpty()) {
            LOGGER.warn("Could not apply HVDC alignment, dangling line for recessive node {} not found",
                    hvdcAlignmentXNodeCouple.getRecessiveXNode());
            return;
        }

        LOGGER.info("Applying HVDC alignment on: reference node {} - recessive node {}",
                hvdcAlignmentXNodeCouple.getReferenceXNode(),
                hvdcAlignmentXNodeCouple.getRecessiveXNode());

        DanglingLine referenceDanglingLine = referenceDanglingLineOpt.get();
        DanglingLine recessiveDanglingLine = recessiveDanglingLineOpt.get();

        boolean referenceInOperation = IN_OPERATION.contains(getStatus(referenceDanglingLine));
        boolean recessiveInOperation = IN_OPERATION.contains(getStatus(recessiveDanglingLine));

        if (!referenceInOperation && recessiveInOperation) {
            applyReferenceInOutageAlignment(recessiveDanglingLine);
        } else if (referenceInOperation && !recessiveInOperation) {
            applyRecessiveInOutageAlignment(referenceDanglingLine, recessiveDanglingLine);
        } else if (referenceInOperation && recessiveInOperation) {
            applyAlignment(referenceDanglingLine, recessiveDanglingLine);
        }
    }

    private Optional<DanglingLine> findDanglingLine(final Network network, final String pairingKey) {
        return network.getDanglingLineStream()
                .filter(danglingLine -> pairingKey.equals(danglingLine.getPairingKey()))
                .findFirst();
    }

    private void applyReferenceInOutageAlignment(final DanglingLine recessiveDanglingLine) {
        invertDanglingLineStatus(recessiveDanglingLine);
        recessiveDanglingLine.setP0(0);
        recessiveDanglingLine.setQ0(0);
        recessiveDanglingLine.getGeneration().setTargetP(0);
        recessiveDanglingLine.getGeneration().setTargetQ(0);
    }

    private void applyRecessiveInOutageAlignment(final DanglingLine referenceDanglingLine, final DanglingLine recessiveDanglingLine) {
        invertDanglingLineStatus(recessiveDanglingLine);
        final double shiftPload = referenceDanglingLine.getP0();
        final double shiftPgen = referenceDanglingLine.getGeneration().getTargetP();
        recessiveDanglingLine.setP0(-shiftPload);
        recessiveDanglingLine.getGeneration().setTargetP(-shiftPgen);
    }

    private void applyAlignment(final DanglingLine referenceDanglingLine, final DanglingLine recessiveDanglingLine) {
        recessiveDanglingLine.setP0(-referenceDanglingLine.getP0());
        recessiveDanglingLine.getGeneration().setTargetP(-referenceDanglingLine.getGeneration().getTargetP());
    }

    static UcteElementStatus getStatus(final DanglingLine danglingLine) {
        final boolean isConnected = danglingLine.getTerminal().isConnected();
        if (Boolean.parseBoolean(danglingLine.getProperty(IS_COUPLER_PROPERTY_KEY, "false"))) {
            return isConnected ? UcteElementStatus.BUSBAR_COUPLER_IN_OPERATION : UcteElementStatus.BUSBAR_COUPLER_OUT_OF_OPERATION;
        }
        if (danglingLine.isFictitious()) {
            return isConnected ? UcteElementStatus.EQUIVALENT_ELEMENT_IN_OPERATION : UcteElementStatus.EQUIVALENT_ELEMENT_OUT_OF_OPERATION;
        }
        return isConnected ? UcteElementStatus.REAL_ELEMENT_IN_OPERATION : UcteElementStatus.REAL_ELEMENT_OUT_OF_OPERATION;
    }

    private static void invertDanglingLineStatus(final DanglingLine danglingLine) {
        if (danglingLine.getTerminal().isConnected()) {
            danglingLine.getTerminal().disconnect();
        } else {
            danglingLine.getTerminal().connect();
        }
    }
}
