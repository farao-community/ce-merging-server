/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.ce_merging.merging.process.internal_bidding_zones;

import com.farao_community.farao.ce_merging.common.util.JaxbUtils;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.xsd.internal_bidding_zones.BiddingZoneUCTEDocument;
import com.farao_community.farao.ce_merging.xsd.internal_bidding_zones.BranchType;
import com.farao_community.farao.ce_merging.xsd.internal_bidding_zones.InternalTieLineType;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.function.Supplier;

import static com.farao_community.farao.ce_merging.common.util.LoadFlowUtils.runLoadFlow;
import static com.farao_community.farao.ce_merging.common.util.NetworkUtil.getLineFlow;
import static com.farao_community.farao.ce_merging.common.util.NetworkUtil.getVlIdOnSide;
import static com.farao_community.farao.ce_merging.merging.process.internal_bidding_zones.ItalianBiddingZone.lineJoinsZones;
import static com.powsybl.iidm.network.TwoSides.ONE;
import static com.powsybl.iidm.network.TwoSides.TWO;

public class InternalBiddingZonesManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(InternalBiddingZonesManager.class);
    private final BiddingZoneUCTEDocument biddingZoneData;
    private final Network cgm;

    public InternalBiddingZonesManager(final MergingTask task,
                                       final Supplier<LoadFlow.Runner> loadFlowRunnerSupplier) {
        this.biddingZoneData = JaxbUtils.readFromPath(BiddingZoneUCTEDocument.class,
                                                      task.getInputs().getInternalBiddingZones().getPath());
        this.cgm = Network.read(task.getOutputs().getCgm().getPath());
        runLoadFlow(cgm, loadFlowRunnerSupplier, task.getConfigurations().getLoadFlowParameters());
    }

    public double getTotalFlow(final ItalianBiddingZone bzFrom,
                               final ItalianBiddingZone bzTo) {
        double sum = 0.0;
        for (final InternalTieLineType tieLine : biddingZoneData.getInternalTieLines().getInternalTieLine()) {
            if (lineJoinsZones(tieLine, bzFrom, bzTo)) {
                // from -> to and to -> from must be counted with opposite signs (respectively +/-)
                final double flowSign = bzFrom.getFlowSign(tieLine);
                sum += toLine(tieLine.getBranch()).map(line -> flowSign * getLineFlow(line)).orElse(0.);
            }
        }
        return sum;
    }

    private Optional<Line> toLine(final BranchType branch) {
        final Optional<Line> optLine = cgm.getLineStream()
                .filter(line -> areSameBranches(branch, line))
                .findFirst();

        if (optLine.isEmpty()) {
            LOGGER.warn("line {} linking {} to {} either doesn't exist or is in outage",
                        branch.getElementName(), branch.getFrom(), branch.getTo());
        }

        return optLine;
    }

    private static boolean areSameBranches(final BranchType branch,
                                           final Line line) {
        return getVlIdOnSide(line, ONE).contains(branch.getFrom())
               && getVlIdOnSide(line, TWO).contains(branch.getTo());

    }
}
