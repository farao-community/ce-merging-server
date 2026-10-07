/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.hvdc_alignment;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.VirtualHubsAlignmentCouple;
import com.powsybl.iidm.network.BoundaryLine;
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
    public static final String RECESSIVE_DANGLING_LINE = "recessive dangling line";
    public static final String REFERENCE_DANGLING_LINE = "reference dangling line";

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

    void align() {
        Optional<BoundaryLine> referenceBoundaryLineOpt = findBoundaryLine(referenceNetwork, hvdcAlignmentXNodeCouple.getReferenceXNode());
        if (referenceBoundaryLineOpt.isEmpty()) {
            LOGGER.warn("Could not apply HVDC alignment, dangling line for reference node {} not found",
                    hvdcAlignmentXNodeCouple.getReferenceXNode());
            return;
        }

        Optional<BoundaryLine> recessiveBoundaryLineOpt = findBoundaryLine(recessiveNetwork, hvdcAlignmentXNodeCouple.getRecessiveXNode());
        if (recessiveBoundaryLineOpt.isEmpty()) {
            LOGGER.warn("Could not apply HVDC alignment, dangling line for recessive node {} not found",
                    hvdcAlignmentXNodeCouple.getRecessiveXNode());
            return;
        }

        LOGGER.info("Applying HVDC alignment on: reference node {} - recessive node {}",
                hvdcAlignmentXNodeCouple.getReferenceXNode(),
                hvdcAlignmentXNodeCouple.getRecessiveXNode());

        final BoundaryLine referenceBoundaryLine = referenceBoundaryLineOpt.get();
        final BoundaryLine recessiveBoundaryLine = recessiveBoundaryLineOpt.get();

        boolean referenceInOperation = isInOperation(referenceBoundaryLine);
        boolean recessiveInOperation = isInOperation(recessiveBoundaryLine);

        if (!referenceInOperation && recessiveInOperation) {
            applyReferenceInOutageAlignment(recessiveBoundaryLine);
        } else if (referenceInOperation && !recessiveInOperation) {
            applyRecessiveInOutageAlignment(referenceBoundaryLine, recessiveBoundaryLine);
        } else {
            applyAlignment(referenceBoundaryLine, recessiveBoundaryLine);
        }
    }

    private Optional<BoundaryLine> findBoundaryLine(final Network network, final String pairingKey) {
        return network.getBoundaryLineStream()
                .filter(boundaryLine -> pairingKey.equals(boundaryLine.getPairingKey()))
                .findFirst();
    }

    private void applyReferenceInOutageAlignment(final BoundaryLine recessiveBoundaryLine) {
        invertBoundaryLineStatus(recessiveBoundaryLine);
        recessiveBoundaryLine.setP0(0);
        recessiveBoundaryLine.setQ0(0);
        final BoundaryLine.Generation generation = requireGeneration(recessiveBoundaryLine, RECESSIVE_DANGLING_LINE);
        generation.setTargetP(0);
        generation.setTargetQ(0);
    }

    private void applyRecessiveInOutageAlignment(final BoundaryLine referenceBoundaryLine, final BoundaryLine recessiveBoundaryLine) {
        invertBoundaryLineStatus(recessiveBoundaryLine);
        final BoundaryLine.Generation referenceGeneration = requireGeneration(referenceBoundaryLine, REFERENCE_DANGLING_LINE);
        final BoundaryLine.Generation recessiveGeneration = requireGeneration(recessiveBoundaryLine, RECESSIVE_DANGLING_LINE);
        final double referenceTargetP = referenceGeneration.getTargetP();
        final double referenceP0 = referenceBoundaryLine.getP0();
        recessiveBoundaryLine.setP0(-referenceP0);
        recessiveGeneration.setTargetP(-referenceTargetP);
    }

    private void applyAlignment(final BoundaryLine referenceBoundaryLine, final BoundaryLine recessiveBoundaryLine) {
        recessiveBoundaryLine.setP0(-referenceBoundaryLine.getP0());
        final BoundaryLine.Generation referenceGeneration = requireGeneration(referenceBoundaryLine, REFERENCE_DANGLING_LINE);
        final BoundaryLine.Generation  recessiveGeneration = requireGeneration(recessiveBoundaryLine, RECESSIVE_DANGLING_LINE);
        recessiveGeneration.setTargetP(-referenceGeneration.getTargetP());
    }

    static UcteElementStatus getStatus(final BoundaryLine boundaryLine) {
        final boolean isConnected = boundaryLine.getTerminal().isConnected();
        if (Boolean.parseBoolean(boundaryLine.getProperty(IS_COUPLER_PROPERTY_KEY, "false"))) {
            return isConnected ? UcteElementStatus.BUSBAR_COUPLER_IN_OPERATION : UcteElementStatus.BUSBAR_COUPLER_OUT_OF_OPERATION;
        }
        if (boundaryLine.isFictitious()) {
            return isConnected ? UcteElementStatus.EQUIVALENT_ELEMENT_IN_OPERATION : UcteElementStatus.EQUIVALENT_ELEMENT_OUT_OF_OPERATION;
        }
        return isConnected ? UcteElementStatus.REAL_ELEMENT_IN_OPERATION : UcteElementStatus.REAL_ELEMENT_OUT_OF_OPERATION;
    }

    private static void invertBoundaryLineStatus(final BoundaryLine boundaryLine) {
        if (boundaryLine.getTerminal().isConnected()) {
            boundaryLine.getTerminal().disconnect();
        } else {
            boundaryLine.getTerminal().connect();
        }
    }

    private static boolean isInOperation(BoundaryLine boundaryLine) {
        return IN_OPERATION.contains(getStatus(boundaryLine));
    }

    static BoundaryLine.Generation requireGeneration(final BoundaryLine boundaryLine, final String role) {
        final BoundaryLine.Generation generation = boundaryLine.getGeneration();
        if (generation == null) {
            throw new CeMergingException("Generation is missing for " + role + " " + boundaryLine.getId());
        }
        return generation;
    }
}
