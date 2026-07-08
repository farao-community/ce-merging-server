/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.xnode;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.util.BordersUtils;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.XnodeConfig;
import com.farao_community.farao.ce_merging.merging.task.entities.VirtualHubRecord;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.TwoSides;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.Optional;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.GERMAN_TSO;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.GERMAN_COUNTRY_CODE;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.VIRTUAL_HUB_ALEGRO_BE_NODE_NAME;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.VIRTUAL_HUB_ALEGRO_DE_NODE_NAME;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.DANISH_TSO;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.DENMARK_COUNTRY_CODE;
import static com.farao_community.farao.ce_merging.common.util.BordersUtils.zeroIfNan;

@Service
public class XnodesCalculation {

    private static final Logger LOGGER = LoggerFactory.getLogger(XnodesCalculation.class);

    public void fillXnodesInformation(final Network network, final String tso, final Map<String, XnodeInformation> xnodeInformationMap, final List<VirtualHubRecord> virtualHubList, final List<XnodeConfig> xnodes, final boolean germanMode) {
        final Set<String> xnodesArea1 = xnodes.stream()
                .filter(xnode -> matchesArea1(xnode, tso, germanMode))
                .map(XnodeConfig::getName)
                .collect(Collectors.toSet());

        final Set<String> xnodesArea2 = xnodes.stream()
                .filter(xnode -> matchesArea2(xnode, tso, germanMode))
                .map(XnodeConfig::getName)
                .collect(Collectors.toSet());

        final String alegroVirtualHub = germanMode ? VIRTUAL_HUB_ALEGRO_DE_NODE_NAME : VIRTUAL_HUB_ALEGRO_BE_NODE_NAME;
        final Optional<String> tsoOpt = germanMode ? Optional.of(tso) : Optional.empty();

        processDanglingLines(network, xnodesArea1, virtualHubList, alegroVirtualHub, xnodeInformationMap, 1, tsoOpt);

        processDanglingLines(network, xnodesArea2, virtualHubList, alegroVirtualHub, xnodeInformationMap, 2, tsoOpt);
    }

    public void checkXnodesConfigConsistency(final Network network, final List<VirtualHubRecord> virtualHubList, final List<XnodeConfig> xnodes) {
        final List<String> allXnodesConfig = Stream.concat(xnodes.stream().map(XnodeConfig::getName), virtualHubList.stream().map(VirtualHubRecord::getNodeName)).distinct().toList();
        network.getDanglingLineStream().map(DanglingLine::getPairingKey).forEach(xnodeCode -> {
            if (!allXnodesConfig.contains(xnodeCode)) {
                LOGGER.error("Xnode {} present in network {} is not found in the xnodes config list nor in the virtual hubs list", xnodeCode, network.getNameOrId());
                throw new CeMergingException("Xnode " + xnodeCode + " present in network " + network.getNameOrId() + " is not found in the xnodes config list and in the virtual hubs list");
            }
        });
    }

    private void addAreaInformation(final Map<String, XnodeInformation> xnodeInformationMap, final DanglingLine danglingLine, final int areaNumber, final Optional<String> tsoOpt) {
        final String xnodeCode = danglingLine.getPairingKey();
        switch (areaNumber) {
            case 1:
                if (xnodeInformationMap.containsKey(xnodeCode)) {
                    xnodeInformationMap.get(xnodeCode).setArea1Information(fillAreaInformation(danglingLine, tsoOpt));
                } else {
                    xnodeInformationMap.put(xnodeCode, new XnodeInformation(fillAreaInformation(danglingLine, tsoOpt)));
                }
                break;
            case 2:
                if (xnodeInformationMap.containsKey(xnodeCode)) {
                    xnodeInformationMap.get(xnodeCode).setArea2Information(fillAreaInformation(danglingLine, tsoOpt));
                } else {
                    xnodeInformationMap.put(xnodeCode, new XnodeInformation(null, fillAreaInformation(danglingLine, tsoOpt)));
                }
                break;
            default:
                break;
        }
    }

    private AreaInformation fillAreaInformation(final DanglingLine danglingLine, final Optional<String> tsoOpt) {
        final XnodeStatus status = danglingLine.getTerminal().isConnected() ? XnodeStatus.CLOSE : XnodeStatus.OPEN;
        final String country = tsoOpt.orElseGet(() -> BordersUtils.getCountry(danglingLine).toString());
        double v = 0;
        final DanglingLine.Generation generation = danglingLine.getGeneration();
        final double p0 = danglingLine.getP0();
        final double q0 = danglingLine.getQ0();
        final double p = generation == null ? p0 : p0 - generation.getTargetP();
        final String nodeId = danglingLine.getTerminal().getVoltageLevel().getId();
        final double q = generation == null ? q0 : q0 - generation.getTargetQ();
        final Bus bus = danglingLine.getTerminal().getBusBreakerView().getBus();
        if (bus != null && !Double.isNaN(bus.getV())) {
            v = bus.getV(); //Always NaN because no loadflow run before
        }
        return new AreaInformation(country, status, nodeId, p, q, v);
    }

    public Map<String, XnodeInformation> completeXnodeMergedInformation(final Network network, final Map<String, XnodeInformation> xnodeInformationMap) {
        xnodeInformationMap.entrySet().stream().filter(e -> e.getValue().getArea1Information() != null && e.getValue().getArea2Information() != null)
                .forEach(e -> processXnodeEntry(network, e));
        return xnodeInformationMap;
    }

    private void processXnodeEntry(Network network, Map.Entry<String, XnodeInformation> e) {
        final String nodeId = e.getKey();
        final XnodeInformation xnodeInformation = e.getValue();
        if (!isGermanInternalNode(xnodeInformation)) {
            final Optional<Branch> branchOpt = network.getBranchStream().filter(branch -> branch.getId().contains(nodeId.substring(0, 8))).findFirst();
            branchOpt.ifPresent(branch -> addMergedInformation(branch, xnodeInformation));
        } else {
            // for german internal node that were renamed in germany premerge step: begin with "D" not "X"
            final List<Branch> branches = network.getBranchStream().filter(branch -> branch.getId().contains(nodeId.substring(1, 8))).collect(Collectors.toList());
            if (branches.size() == 1) {
                addMergedInformation(branches.getFirst(), xnodeInformation);
            } else if (branches.size() == 2) {
                final Branch branchFrom = findCorrectBranch(branches, nodeId, xnodeInformation.getArea1Information().getCountry());
                final Branch branchTo = findCorrectBranch(branches, nodeId, xnodeInformation.getArea2Information().getCountry());
                addMergedInformationForGermanNode(branchFrom, branchTo, xnodeInformation);
            }
        }
    }

    private void addMergedInformation(Branch branch, XnodeInformation xnodeInformation) {
        boolean isConnected = isConnected(branch);
        XnodeStatus status = isConnected ? XnodeStatus.CLOSE : XnodeStatus.OPEN;
        MergedXnodeInformation mergedXnodeInformation = new MergedXnodeInformation(status, 0, 0, 0, 0);
        if (isConnected) {
            final Country country1 = getCountry(xnodeInformation.getArea1Information());
            final boolean country1IsSideOne = country1.equals(BordersUtils.getCountrySide(branch, TwoSides.ONE));
            //We take the xnode flow in the direction country 1 to country 2
            final Terminal terminalFrom = country1IsSideOne ? branch.getTerminal1() : branch.getTerminal2();
            final Terminal terminalTo = country1IsSideOne ? branch.getTerminal2() : branch.getTerminal1();
            if (terminalFrom.isConnected() && terminalTo.isConnected()) {
                mergedXnodeInformation.setP((getP(terminalFrom) - getP(terminalTo)) / 2);
                mergedXnodeInformation.setQ((getQ(terminalFrom) - getQ(terminalTo)) / 2);
                mergedXnodeInformation.setV1(getV(terminalFrom));
                mergedXnodeInformation.setV2(getV(terminalTo));
            }
        }
        xnodeInformation.setMergedInformation(mergedXnodeInformation);
    }

    private boolean isConnected(final Branch branch) {
        return branch.getTerminal1().isConnected() && branch.getTerminal2().isConnected();
    }

    private void addMergedInformationForGermanNode(final Branch branch1, final Branch branch2, final XnodeInformation xnodeInformation) {
        final boolean isConnected = isConnected(branch1) && isConnected(branch2);
        final XnodeStatus status = isConnected ? XnodeStatus.CLOSE : XnodeStatus.OPEN;
        double p = 0;
        double q = 0;
        double v1 = 0;
        double v2 = 0;

        if (isConnected) {
            //We take the xnode flow in the direction branch 1 to branch 2
            final MergedXnodeInformation mergedInformation1 = getMergedValuesFromGermanBranch(branch1, xnodeInformation.getArea1Information().getCountry());
            final MergedXnodeInformation mergedInformation2 = getMergedValuesFromGermanBranch(branch2, xnodeInformation.getArea2Information().getCountry());

            p = (mergedInformation1.getP() - mergedInformation2.getP()) / 2;
            q = (mergedInformation1.getQ() - mergedInformation2.getQ()) / 2;
            v1 = mergedInformation1.getV1();
            v2 = mergedInformation2.getV2();
        }
        xnodeInformation.setMergedInformation(new MergedXnodeInformation(status, p, q, v1, v2));
    }

    private Branch findCorrectBranch(final List<Branch> branches, final String nodeId, final String countryName) {
        if (nodeId == null || nodeId.length() < 7) {
            throw new CeMergingException("Invalid nodeId: " + nodeId);
        }
        for (final Branch branch : branches) {
            final String voltageLevel1 = branch.getTerminal1().getVoltageLevel().getId();
            final String voltageLevel2 = branch.getTerminal2().getVoltageLevel().getId();
            final String internalNodeId = nodeId.substring(1, 7);
            if (voltageLevel1.contains(internalNodeId) && voltageLevel2.startsWith(countryName) || voltageLevel2.contains(internalNodeId) && voltageLevel1.startsWith(countryName)) {
                return branch;
            }
        }
        LOGGER.warn("No matching branch found for node {} and country {}", nodeId, countryName);
        return null;
    }

    private MergedXnodeInformation getMergedValuesFromGermanBranch(final Branch branch, final String tsoName) {
        double p = 0;
        double q = 0;
        double v = 0;

        final Terminal terminalFrom = isTerminal1MatchingTso(branch, tsoName) ? branch.getTerminal1() : branch.getTerminal2();
        final Terminal terminalTo = isTerminal1MatchingTso(branch, tsoName) ? branch.getTerminal2() : branch.getTerminal1();

        if (terminalFrom.isConnected() && terminalTo.isConnected()) {
            p = (getP(terminalFrom) - getP(terminalTo)) / 2;
            q = (getQ(terminalFrom) - getQ(terminalTo)) / 2;
            v = getV(terminalFrom);
        }

        return new MergedXnodeInformation(null, p, q, v, 0);
    }

    private boolean isTerminal1MatchingTso(final Branch branch, final String tsoName) {
        return branch.getTerminal1().getVoltageLevel().getId().startsWith(tsoName);
    }

    private double getV(final Terminal terminal) {
        return (terminal.getBusBreakerView().getBus() != null && !Double.isNaN(terminal.getBusBreakerView().getBus().getV())) ? terminal.getBusBreakerView().getBus().getV() : 0;
    }

    private double getQ(final Terminal terminal) {
        return zeroIfNan(terminal.getQ());
    }

    private double getP(final Terminal terminal) {
        return zeroIfNan(terminal.getP());
    }

    private Country getCountry(final AreaInformation xnodeAreaInformation) {
        if (GERMAN_TSO.contains(xnodeAreaInformation.getCountry())) {
            return Country.valueOf(GERMAN_COUNTRY_CODE);
        } else if (DANISH_TSO.equals(xnodeAreaInformation.getCountry())) {
            return Country.valueOf(DENMARK_COUNTRY_CODE);
        } else {
            return Country.valueOf(xnodeAreaInformation.getCountry());
        }
    }

    private boolean isGermanInternalNode(final XnodeInformation xnodeInformation) {
        final AreaInformation area1Information = xnodeInformation.getArea1Information();
        final AreaInformation area2Information = xnodeInformation.getArea2Information();
        return area1Information != null && getCountry(area1Information).equals(Country.DE)
                && area2Information != null && getCountry(area2Information).equals(Country.DE);
    }

    private boolean matchesArea1(XnodeConfig xnode, String tso, boolean germanMode) {
        return germanMode
                ? GERMAN_COUNTRY_CODE.equals(xnode.getArea1()) && tso.equals(xnode.getSubarea1())
                : tso.equals(xnode.getArea1());
    }

    private boolean matchesArea2(XnodeConfig xnode, String tso, boolean germanMode) {
        return germanMode
                ? GERMAN_COUNTRY_CODE.equals(xnode.getArea2()) && tso.equals(xnode.getSubarea2())
                : tso.equals(xnode.getArea2());
    }

    private void processDanglingLines(final Network network, final Set<String> xnodes, final List<VirtualHubRecord> virtualHubList, final String virtualHubException, final Map<String, XnodeInformation> xnodeInformationMap, final int areaNumber, Optional<String> tsoOpt) {
        network.getDanglingLineStream()
                .filter(dl -> xnodes.contains(dl.getPairingKey()))
                .filter(dl -> !BordersUtils.isVirtualHubDanglingLine(dl, virtualHubList) ||
                        dl.getPairingKey().equals(virtualHubException))
                .forEach(dl -> addAreaInformation(xnodeInformationMap, dl, areaNumber, tsoOpt));
    }

}
