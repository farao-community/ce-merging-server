/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 */
package com.farao_community.farao.ce_merging.merging.process.xnode;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.util.BordersUtils;
import com.farao_community.farao.ce_merging.merging.task.entities.VirtualHubRecord;
import com.farao_community.farao.ce_merging.merging.task.entities.Xnode;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.TwoSides;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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

@Service
public class XnodesCalculation {

    private static final Logger LOGGER = LoggerFactory.getLogger(XnodesCalculation.class);

    public void fillXnodesInformation(final Network network, final String tso, final Map<String, XnodeInformation> xnodeInformationMap, final List<VirtualHubRecord> virtualHubList, final List<Xnode> xnodes, final boolean germanMode) {
        final Set<String> xnodesArea1 = xnodes.stream()
                .filter(xnode -> germanMode ? xnode.getArea1().equals(GERMAN_COUNTRY_CODE) && tso.equals(xnode.getSubarea1()) : xnode.getArea1().equals(tso))
                .map(Xnode::getName)
                .collect(Collectors.toSet());

        final Set<String> xnodesArea2 = xnodes.stream().filter(xnode -> germanMode ? xnode.getArea2().equals(GERMAN_COUNTRY_CODE) && tso.equals(xnode.getSubarea2()) : xnode.getArea2().equals(tso))
                .map(Xnode::getName)
                .collect(Collectors.toSet());

        final String alegroVirtualHub = germanMode ? VIRTUAL_HUB_ALEGRO_DE_NODE_NAME : VIRTUAL_HUB_ALEGRO_BE_NODE_NAME;
        final Optional<String> tsoOpt = germanMode ? Optional.of(tso) : Optional.empty();

        processDanglingLines(network, xnodesArea1, virtualHubList, alegroVirtualHub, xnodeInformationMap, 1, tsoOpt);

        processDanglingLines(network, xnodesArea2, virtualHubList, alegroVirtualHub, xnodeInformationMap, 2, tsoOpt);
    }

    private void processDanglingLines(final Network network, final Set<String> xnodes, final List<VirtualHubRecord> virtualHubList, final String virtualHubException, final Map<String, XnodeInformation> xnodeInformationMap, final int areaNumber, Optional<String> tsoOpt) {

        network.getDanglingLineStream()
                .filter(dl -> xnodes.contains(dl.getPairingKey()))
                .filter(dl -> !BordersUtils.isVirtualHubDanglingLine(dl, virtualHubList) ||
                         dl.getPairingKey().equals(virtualHubException))
                .forEach(dl -> addAreaInformation(dl, xnodeInformationMap, areaNumber, tsoOpt));
    }

    public void checkAllXnodesInNetworkArePresentInConfigList(final Network network, final List<VirtualHubRecord> virtualHubList, final List<Xnode> xnodes) {
        final List<String> allXnodesInNetwork = network.getDanglingLineStream().map(DanglingLine::getPairingKey).collect(Collectors.toList());
        final List<String> allXnodesConfig = Stream.concat(xnodes.stream().map(Xnode::getName), virtualHubList.stream().map(VirtualHubRecord::getNodeName)).distinct().collect(Collectors.toList());
        allXnodesInNetwork.forEach(xnodeCode -> {
            if (!allXnodesConfig.contains(xnodeCode)) {
                LOGGER.error("Xnode {} present in network {} is not found in the xnodes config list nor in the virtual hubs list", xnodeCode, network.getNameOrId());
                throw new CeMergingException("Xnode " + xnodeCode + " present in network " + network.getNameOrId() + " is not found in the xnodes config list and in the virtual hubs list");
            }
        });
    }

    private void addAreaInformation(final DanglingLine danglingLine, final Map<String, XnodeInformation> xnodeInformationMap, final int areaNumber, final Optional<String> tsoOpt) {
        String xnodeCode = danglingLine.getPairingKey();
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
        XnodeStatus status = danglingLine.getTerminal().isConnected() ? XnodeStatus.CLOSE : XnodeStatus.OPEN;
        String country = tsoOpt.orElseGet(() -> BordersUtils.getCountry(danglingLine).toString());
        double v = 0;
        final double p = danglingLine.getGeneration() == null ? danglingLine.getP0() : danglingLine.getP0() - danglingLine.getGeneration().getTargetP();
        final String nodeId = danglingLine.getTerminal().getVoltageLevel().getId();
        final double q = danglingLine.getGeneration() == null ? danglingLine.getQ0() : danglingLine.getQ0() - danglingLine.getGeneration().getTargetQ();
        if (danglingLine.getTerminal().getBusBreakerView().getBus() != null && !Double.isNaN(danglingLine.getTerminal().getBusBreakerView().getBus().getV())) {
            v = danglingLine.getTerminal().getBusBreakerView().getBus().getV(); //Always NaN because no loadflow run before
        }
        return new AreaInformation(country, status, nodeId, p, q, v);
    }

    public Map<String, XnodeInformation> completeXnodeMergedInformation(final Network network, final Map<String, XnodeInformation> xnodeInformationMap) {
        for (Map.Entry<String, XnodeInformation> e : xnodeInformationMap.entrySet()) {
            if (e.getValue().getArea1Information() != null && e.getValue().getArea2Information() != null) {
                if (!isGermanInternalNode(e.getValue())) {
                    final Optional<Branch> branchOpt = network.getBranchStream().filter(branch -> branch.getId().contains(e.getKey().substring(0, 8))).findFirst();
                    branchOpt.ifPresent(branch -> addMergedInformation(branch, e.getValue()));
                } else {
                    // for german internal node are renamed in germany premerge step: begin with "D" not "X"
                    final List<Branch> branches = network.getBranchStream().filter(branch -> branch.getId().contains(e.getKey().substring(1, 8))).collect(Collectors.toList());
                    if (branches.size() == 1) {
                        addMergedInformation(branches.get(0), e.getValue());
                    } else if (branches.size() == 2) { // should be always 2 for internal german nodes
                        final Branch branchFrom = findCorrectBranch(branches, e.getKey(), e.getValue().getArea1Information().getCountry());
                        final Branch branchTo = findCorrectBranch(branches, e.getKey(), e.getValue().getArea2Information().getCountry());
                        addMergedInformationForGermanNode(branchFrom, branchTo, e.getValue());
                    }
                }
            }
        }
        return xnodeInformationMap;
    }

    private void addMergedInformation(Branch branch, XnodeInformation xnodeInformation) {
        boolean isConnected = isConnected(branch);
        XnodeStatus status = isConnected ? XnodeStatus.CLOSE : XnodeStatus.OPEN;
        MergedInformation mergedInformation = new MergedInformation(status, 0, 0, 0, 0);
        if (isConnected) {
            final Country country1 = getCountry(xnodeInformation.getArea1Information());
            final boolean country1IsSideOne = country1.equals(BordersUtils.getCountrySide(branch, TwoSides.ONE));
            //We take the xnode flow in the direction country 1 to country 2
            final Terminal terminalFrom = country1IsSideOne ? branch.getTerminal1() : branch.getTerminal2();
            final Terminal terminalTo = country1IsSideOne ? branch.getTerminal2() : branch.getTerminal1();
            if (terminalFrom.isConnected() && terminalTo.isConnected()) {
                mergedInformation.setP((getP(terminalFrom) - getP(terminalTo)) / 2);
                mergedInformation.setQ((getQ(terminalFrom) - getQ(terminalTo)) / 2);
                mergedInformation.setV1(getV(terminalFrom));
                mergedInformation.setV2(getV(terminalTo));
            }
        }
        xnodeInformation.setMergedInformation(mergedInformation);
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
            final List<Double> mergedValues1 = getMergedValuesFromGermanBranch(branch1, xnodeInformation.getArea1Information().getCountry());
            final List<Double> mergedValues2 = getMergedValuesFromGermanBranch(branch2, xnodeInformation.getArea2Information().getCountry());

            p = (mergedValues1.get(0) - mergedValues2.get(0)) / 2;
            q = (mergedValues1.get(1) - mergedValues2.get(1)) / 2;
            v1 = mergedValues1.get(2);
            v2 = mergedValues2.get(2);
        }
        xnodeInformation.setMergedInformation(new MergedInformation(status, p, q, v1, v2));
    }

    private Branch findCorrectBranch(List<Branch> branches, String nodeId, String countryName) {
        if (nodeId == null || nodeId.length() < 7) {
            throw new CeMergingException("Invalid nodeId: " + nodeId);
        }
        for (Branch branch : branches) {
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

    private List<Double> getMergedValuesFromGermanBranch(Branch branch, String tsoName) {
        //We take the average between the two flow side to compensate the losses of the line
        List<Double> mergedValues = new ArrayList<>();
        double p = 0;
        double q = 0;
        double v = 0;

        Terminal terminalFrom = branch.getTerminal1().getVoltageLevel().getId().startsWith(tsoName) ? branch.getTerminal1() : branch.getTerminal2();
        Terminal terminalTo = branch.getTerminal1().getVoltageLevel().getId().startsWith(tsoName) ? branch.getTerminal2() : branch.getTerminal1();
        if (terminalFrom.isConnected() && terminalTo.isConnected()) {
            p = (getP(terminalFrom) - getP(terminalTo)) / 2;
            q = (getQ(terminalFrom) - getQ(terminalTo)) / 2;
            v = getV(terminalFrom);
        }
        mergedValues.add(p);
        mergedValues.add(q);
        mergedValues.add(v);
        return mergedValues;
    }

    private double getV(Terminal terminal) {
        return (terminal.getBusBreakerView().getBus() != null && !Double.isNaN(terminal.getBusBreakerView().getBus().getV())) ? terminal.getBusBreakerView().getBus().getV() : 0;
    }

    private double getQ(Terminal terminal) {
        return Double.isNaN(terminal.getQ()) ? 0 : terminal.getQ();
    }

    private double getP(Terminal terminal) {
        return Double.isNaN(terminal.getP()) ? 0 : terminal.getP();
    }

    private Country getCountry(AreaInformation xnodeAreaInformation) {
        if (GERMAN_TSO.contains(xnodeAreaInformation.getCountry())) {
            return Country.valueOf(GERMAN_COUNTRY_CODE);
        } else if (DANISH_TSO.equals(xnodeAreaInformation.getCountry())) {
            return Country.valueOf(DENMARK_COUNTRY_CODE);
        } else {
            return Country.valueOf(xnodeAreaInformation.getCountry());
        }
    }

    private boolean isGermanInternalNode(XnodeInformation xnodeInformation) {
        AreaInformation area1Information = xnodeInformation.getArea1Information();
        AreaInformation area2Information = xnodeInformation.getArea2Information();
        return area1Information != null && getCountry(area1Information).equals(Country.DE)
                && area2Information != null && getCountry(area2Information).equals(Country.DE);
    }

}
