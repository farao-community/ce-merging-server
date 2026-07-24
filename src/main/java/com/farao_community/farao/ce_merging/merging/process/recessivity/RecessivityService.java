/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.recessivity;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.XnodeConfig;
import com.farao_community.farao.ce_merging.merging.process.recessivity.inconsistencies.XnodeIncomplete;
import com.farao_community.farao.ce_merging.merging.process.recessivity.inconsistencies.XnodeIncorrect;
import com.farao_community.farao.ce_merging.merging.process.recessivity.inconsistencies.XnodesInconsistencies;
import com.farao_community.farao.ce_merging.merging.process.xnode.AreaInformation;
import com.farao_community.farao.ce_merging.merging.process.xnode.XnodeInformation;
import com.farao_community.farao.ce_merging.merging.process.xnode.XnodeStatus;
import com.farao_community.farao.ce_merging.merging.process.xnode.XnodesCheck;
import com.farao_community.farao.ce_merging.merging.task.MergingTaskRepository;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.TwoSides;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.ALEGRO_NODE_PREFIX;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.UCTE_FORMAT;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.VIRTUAL_HUB_ALEGRO_BE_NODE_NAME;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.VIRTUAL_HUB_ALEGRO_DE_NODE_NAME;
import static com.farao_community.farao.ce_merging.common.util.BordersUtils.getCountryOfSide;
import static com.farao_community.farao.ce_merging.common.util.BordersUtils.isConnectedTo;
import static com.farao_community.farao.ce_merging.common.util.CountryUtils.getCountry;
import static com.farao_community.farao.ce_merging.merging.process.FileStorageUtils.saveArtifactFile;
import static com.farao_community.farao.ce_merging.merging.process.FileStorageUtils.saveArtifactNetwork;
import static com.farao_community.farao.ce_merging.merging.process.xnode.XnodeStatus.OPEN;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.TGM_FILE_AFTER_RECESSIVITY;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.TOPOLOGICAL_MERGE_FILE;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.XNODES_INCONSISTENCIES;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.XNODES_INFORMATION_FILE;
import static com.powsybl.iidm.network.Country.BE;
import static com.powsybl.iidm.network.Country.DE;
import static org.apache.commons.lang3.ObjectUtils.anyNull;
import static org.springframework.util.CollectionUtils.isEmpty;

@Service
public class RecessivityService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecessivityService.class);

    private final MergingTaskRepository tasksRepository;
    private final CeMergingConfiguration configuration;

    public RecessivityService(final MergingTaskRepository tasksRepository,
                              final CeMergingConfiguration configuration) {
        this.tasksRepository = tasksRepository;
        this.configuration = configuration;
    }

    public void applyRecessivity(final MergingTask task) {
        try {
            final List<String> recessiveCountries = task.getConfigurations().getOrDefaultRecessiveCountries();
            LOGGER.info("Recessive countries are {}", recessiveCountries);

            checkAndCorrectInconsistencies(
                task, recessiveCountries, task.getArtifact(TOPOLOGICAL_MERGE_FILE, Network.class)
            );
        } catch (final Exception e) {
            String errorMessage = String.format("Xnodes check failed for task %d with target date %s, cause: %s", task.getId(), task.getInputs().getTargetDate(), e.getMessage());
            LOGGER.error(errorMessage);
            throw new CeMergingException(errorMessage, e);
        }
    }

    private void checkAndCorrectInconsistencies(final MergingTask task,
                                                final List<String> recessiveCountries,
                                                final Network network) throws FileNotFoundException {

        final XnodesCheck xnodesCheck = task.getArtifact(XNODES_INFORMATION_FILE, XnodesCheck.class);
        final List<XnodeConfig> xnodesConfigs = task.getConfigurations().getXnodeList();

        final XnodesInconsistencies xnodesInconsistencies = analyzeXnodes(network,
                                                                          xnodesCheck,
                                                                          xnodesConfigs,
                                                                          recessiveCountries);

        saveArtifactNetwork(TGM_FILE_AFTER_RECESSIVITY, network, task, UCTE_FORMAT, null, configuration);
        saveArtifactFile(XNODES_INCONSISTENCIES, xnodesInconsistencies, task, configuration);
        tasksRepository.save(task);

    }

    private XnodesInconsistencies analyzeXnodes(final Network network,
                                                final XnodesCheck xnodesCheck,
                                                final List<XnodeConfig> xnodeConfigs,
                                                final List<String> recessiveCountries) {

        if (xnodesCheck == null || isEmpty(xnodesCheck.getXnodeInformationMap())) {
            return new XnodesInconsistencies();
        }

        final Map<String, XnodeInformation> xnodeInfos = xnodesCheck.getXnodeInformationMap();

        return new XnodesInconsistencies(getIncorrectXnodes(network, xnodeInfos, recessiveCountries),
                                         getIncompleteXnodes(xnodeConfigs, xnodeInfos));

    }

    void checkAlegroXnodes(final List<XnodeIncorrect> xnodeIncorrects,
                           final Map<String, XnodeInformation> infosByName,
                           final List<String> recessiveCountries) {

        final XnodeInformation deAlInfo = infosByName.get(VIRTUAL_HUB_ALEGRO_DE_NODE_NAME);
        final XnodeInformation beAlInfo = infosByName.get(VIRTUAL_HUB_ALEGRO_BE_NODE_NAME);

        if (anyNull(deAlInfo, beAlInfo)) {
            return;
        }

        final XnodeStatus alegroDeStatus = deAlInfo.getArea1Information().getStatus();
        final XnodeStatus alegroBeStatus = beAlInfo.getArea1Information().getStatus();

        if (alegroDeStatus != alegroBeStatus) {
            final AreaInformation alegroDeInfo = new AreaInformation("D7", VIRTUAL_HUB_ALEGRO_DE_NODE_NAME, alegroDeStatus);
            final AreaInformation alegroBeInfo = new AreaInformation(BE.name(), VIRTUAL_HUB_ALEGRO_BE_NODE_NAME, alegroBeStatus);

            xnodeIncorrects.add(new XnodeIncorrect(ALEGRO_NODE_PREFIX, alegroBeInfo, alegroDeInfo, recessiveCountries));
        }

    }

    private List<XnodeIncorrect> getIncorrectXnodes(final Network network,
                                                    final Map<String, XnodeInformation> infosByName,
                                                    final List<String> recessiveCountries) {
        final List<XnodeIncorrect> incorrect = infosByName.entrySet()
            .stream()
            .filter(e -> e.getValue().isIncorrect())
            .map(e -> getXnodeIncorrect(network, e.getKey(), e.getValue(), recessiveCountries))
            .toList();

        checkAlegroXnodes(incorrect, infosByName, recessiveCountries);

        return incorrect;
    }

    private XnodeIncorrect getXnodeIncorrect(final Network network,
                                             final String name,
                                             final XnodeInformation infos,
                                             final List<String> recessiveCountries) {

        final XnodeIncorrect xNode = XnodeIncorrect.buildFrom(name, infos, recessiveCountries);

        // if there is a difference in recessivity, fix it
        if (xNode.isRecessive1() && !xNode.isRecessive2()) {
            alignBranchStatusWithXnode(network, name,
                                       xNode.getCountry1(), xNode.getStatus2(),
                                       isGermanInternalNode(infos));
        } else if (xNode.isRecessive2() && !xNode.isRecessive1()) {
            alignBranchStatusWithXnode(network, name,
                                       xNode.getCountry2(), xNode.getStatus1(),
                                       isGermanInternalNode(infos));
        }

        return xNode;
    }

    private List<XnodeIncomplete> getIncompleteXnodes(final List<XnodeConfig> xNodesConfig,
                                                      final Map<String, XnodeInformation> infosByName) {

        return infosByName.entrySet()
            .stream()
            .filter(e -> e.getValue().isIncomplete() && !isAlegroXnode(e.getKey()))
            .map(e -> getIncompleteXnode(xNodesConfig, e.getKey(), e.getValue()))
            .toList();
    }

    private XnodeIncomplete getIncompleteXnode(final List<XnodeConfig> xNodesConfig,
                                               final String xNodeName,
                                               final XnodeInformation info) {
        final boolean hasArea1Info = info.getArea1Information() != null;
        final AreaInformation existingInfo = hasArea1Info ? info.getArea1Information() : info.getArea2Information();

        final String countryAbsent = xNodesConfig.stream()
            .filter(xNode -> xNode.getName().equals(xNodeName))
            .findFirst()
            .map(cfg -> hasArea1Info ? cfg.getArea2() : cfg.getArea1())
            .orElse("");

        return new XnodeIncomplete(xNodeName, existingInfo, countryAbsent);

    }

    private boolean isAlegroXnode(final String nodeName) {
        return nodeName.equals(VIRTUAL_HUB_ALEGRO_BE_NODE_NAME) || nodeName.equals(VIRTUAL_HUB_ALEGRO_DE_NODE_NAME);
    }

    private void alignBranchStatusWithXnode(final Network network,
                                            final String nodeId,
                                            final String countryName,
                                            final XnodeStatus status,
                                            final boolean isGermanInternalNode) {

        final Optional<Branch> branchToCorrect = isGermanInternalNode ?
            Optional.ofNullable(findCorrectGermanBranch(network, countryName, nodeId))
            : network.getBranchStream().filter(isConnectedTo(nodeId)).findFirst();

        branchToCorrect.ifPresent(branch -> alignBranchStatusWithXnode(status, getCountry(countryName), branch));
    }

    private Branch<?> findCorrectGermanBranch(final Network network,
                                              final String germanyRegionCode,
                                              final String nodeId) {
        // German internal nodes that are renamed in pre-merge step begin with "D", not "X"
        final String node = nodeId.substring(0, 7);
        final String nodeInBranch = nodeId.substring(1, 8);

        return network.getBranchStream()
            .filter(isConnectedTo(nodeInBranch))
            .filter(linksGermanyRegionToNode(germanyRegionCode, node))
            .findFirst()
            .orElse(null);
    }

    private Predicate<Branch> linksGermanyRegionToNode(final String germanyRegionCode, final String node) {
        return branch ->
            branch.getTerminal1().getVoltageLevel().getId().contains(node)
            && branch.getTerminal2().getVoltageLevel().getId().startsWith(germanyRegionCode)
            ||
            branch.getTerminal2().getVoltageLevel().getId().contains(node)
            && branch.getTerminal1().getVoltageLevel().getId().startsWith(germanyRegionCode);
    }

    private void alignBranchStatusWithXnode(final XnodeStatus xnodeStatus,
                                            final Country country,
                                            final Branch<?> branch) {
        final Consumer<Terminal> setToXnodeStatus = xnodeStatus == OPEN ? Terminal::disconnect : Terminal::connect;

        Arrays.stream(TwoSides.values())
            .filter(side -> country.equals(getCountryOfSide(branch, side)))
            .map(branch::getTerminal)
            .forEach(setToXnodeStatus);
    }

    private boolean isGermanInternalNode(XnodeInformation xnodeInformation) {
        final AreaInformation info1 = xnodeInformation.getArea1Information();
        final AreaInformation info2 = xnodeInformation.getArea2Information();
        return info1 != null && getCountry(info1.getCountry()) == DE
               && info2 != null && getCountry(info2.getCountry()) == DE;
    }
}
