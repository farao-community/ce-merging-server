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
import com.farao_community.farao.ce_merging.xsd.internal_bidding_zones.TextOnlyType;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.ELEMENT_NAME_PROPERTY;
import static com.farao_community.farao.ce_merging.common.util.LoadFlowUtils.runLoadFlow;
import static com.farao_community.farao.ce_merging.common.util.NetworkUtil.getLineFlow;

public class InternalBiddingZonesManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(InternalBiddingZonesManager.class);
    private final BiddingZoneUCTEDocument biddingZoneData;
    private final Network cgm;

    public InternalBiddingZonesManager(final MergingTask task,
                                       final Supplier<LoadFlow.Runner> loadFlowRunnerSupplier) {
        this.biddingZoneData = JaxbUtils.readFromPath(BiddingZoneUCTEDocument.class,
                                                      task.getInputs().getInternalBiddingZones().getPath());
        this.cgm = Network.read(task.getOutputs().getCgm().getPath());

        final LoadFlowParameters loadFlowParameters = task.getConfigurations().getLoadFlowParameters();

        runLoadFlow(cgm, loadFlowRunnerSupplier, loadFlowParameters);
    }

    public double getTotalFlow(final ItalianBiddingZone bzFrom,
                               final ItalianBiddingZone bzTo) {
        double sum = 0.0;
        for (final InternalTieLineType tieLine : biddingZoneData.getInternalTieLines().getInternalTieLine()) {
            final boolean linksOppositeWay = links(tieLine, bzTo, bzFrom);
            if (links(tieLine, bzFrom, bzTo) || linksOppositeWay) {
                final BranchType tieBranch = tieLine.getBranch();
                final Optional<Line> optionalLine = toLine(tieBranch);
                if (optionalLine.isPresent()) {
                    final double flowOnTieLine = getLineFlow(optionalLine.get());
                    sum += linksOppositeWay ? -flowOnTieLine : flowOnTieLine;
                } else {
                    LOGGER.warn("line %s linking %s to %s either doesn't exist or is in outage"
                                        .formatted(tieBranch.getElementName(), tieBranch.getFrom(), tieBranch.getTo()));
                }
            }
        }
        return sum;
    }

    private static boolean links(final InternalTieLineType line,
                                 final ItalianBiddingZone from,
                                 final ItalianBiddingZone to) {
        return areSameBiddingZones(line.getBiddingZoneFrom(), from) && areSameBiddingZones(line.getBiddingZoneTo(), to);
    }

    private static boolean areSameBiddingZones(final TextOnlyType inXml,
                                               final ItalianBiddingZone italianBiddingZone) {
        return Objects.equals(inXml.getValue(), italianBiddingZone.name());
    }

    private Optional<Line> toLine(final BranchType branch) {
        return cgm.getLineStream()
                .filter(line -> areSameBranches(branch, line))
                .findFirst();
    }

    private static boolean areSameBranches(final BranchType branch,
                                           final Line line) {
        return branch.getElementName().equals(line.getProperty(ELEMENT_NAME_PROPERTY))
               || Objects.equals(line.getTerminal1().getVoltageLevel().getId(), branch.getFrom())
                   && Objects.equals(line.getTerminal2().getVoltageLevel().getId(), branch.getTo());

    }
}
