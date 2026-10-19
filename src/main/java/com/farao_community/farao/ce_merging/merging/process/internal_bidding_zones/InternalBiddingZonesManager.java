/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.ce_merging.merging.process.internal_bidding_zones;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;import com.farao_community.farao.ce_merging.common.util.JaxbUtils;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.xsd.internal_bidding_zones.BiddingZoneUCTEDocument;
import com.farao_community.farao.ce_merging.xsd.internal_bidding_zones.BranchType;
import com.farao_community.farao.ce_merging.xsd.internal_bidding_zones.InternalTieLineType;
import com.farao_community.farao.ce_merging.xsd.internal_bidding_zones.TextOnlyType;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Terminal;

import java.util.Objects;
import java.util.function.Predicate;

import static com.farao_community.farao.ce_merging.common.util.NetworkUtil.getUcteNode;

public class InternalBiddingZonesManager {
    private final BiddingZoneUCTEDocument biddingZoneData;
    private final Network cgm;


    public InternalBiddingZonesManager(final MergingTask task) {
        this.biddingZoneData = JaxbUtils.readFromPath(BiddingZoneUCTEDocument.class,
                                                      task.getInputs().getInternalBiddingZones().getPath());
        this.cgm = Network.read(task.getOutputs().getCgm().getPath());
    }


    public double getTotalInternalExchange(final ItalianBiddingZone from,
                                           final ItalianBiddingZone to) {
        return biddingZoneData.getInternalTieLines().getInternalTieLine()
                .stream()
                .filter(links(from, to))
                .map(InternalTieLineType::getBranch)
                .map(this::toNetworkBranch)
                .map(Branch::getTerminal1)
                .mapToDouble(Terminal::getP)
                .sum();
    }

    private static Predicate<InternalTieLineType> links(final ItalianBiddingZone from,
                                                        final ItalianBiddingZone to) {
        return line -> areSameBiddingZones(line.getBiddingZoneFrom(), from)
                       && areSameBiddingZones(line.getBiddingZoneTo(), to);
    }

    private static boolean areSameBiddingZones(final TextOnlyType inXml,
                                               final ItalianBiddingZone italianBiddingZone) {
        return Objects.equals(inXml.getValue(), italianBiddingZone.getItalianName());
    }

    private Branch<?> toNetworkBranch(final BranchType branchType) {
        return cgm.getBranchStream()
                .filter(branch -> areSameBranches(branchType, branch))
                .findFirst()
                .orElseThrow(CeMergingException.withMessage("%s not found in CGM", branchType.getElementName()));
    }

    private static boolean areSameBranches(final BranchType branchType,
                                           final Branch<?> branch) {
        return Objects.equals(getUcteNode(branch.getTerminal1()), branchType.getFrom())
               && Objects.equals(getUcteNode(branch.getTerminal2()), branchType.getTo());
    }
}
