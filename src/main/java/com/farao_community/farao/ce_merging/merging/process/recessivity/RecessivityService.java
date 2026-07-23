/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.recessivity;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.util.JsonUtils;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.XnodeConfig;
import com.farao_community.farao.ce_merging.merging.process.recessivity.inconsistent_xnode.XnodeIncomplete;
import com.farao_community.farao.ce_merging.merging.process.recessivity.inconsistent_xnode.XnodeIncorrect;
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
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.DANISH_TSO;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.GERMAN_TSO;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.UCTE_FORMAT;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.VIRTUAL_HUB_ALEGRO_BE_NODE_NAME;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.VIRTUAL_HUB_ALEGRO_DE_NODE_NAME;
import static com.farao_community.farao.ce_merging.common.util.BordersUtils.getCountrySide;
import static com.farao_community.farao.ce_merging.common.util.BordersUtils.isConnectedTo;
import static com.farao_community.farao.ce_merging.merging.process.FileStorageUtils.saveArtifactFile;
import static com.farao_community.farao.ce_merging.merging.process.FileStorageUtils.saveArtifactNetwork;
import static com.farao_community.farao.ce_merging.merging.process.xnode.XnodeStatus.CLOSE;
import static com.farao_community.farao.ce_merging.merging.process.xnode.XnodeStatus.OPEN;
import static com.farao_community.farao.ce_merging.merging.request_metadata.RequestMetadataManager.RECESSIVITY_DEFAULT_CONFIGURATION;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.TGM_FILE_AFTER_RECESSIVITY;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.TOPOLOGICAL_MERGE_FILE;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.XNODES_INCONSISTENCIES;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.XNODES_INFORMATION_FILE;
import static com.powsybl.iidm.network.Country.BE;
import static com.powsybl.iidm.network.Country.DE;
import static com.powsybl.iidm.network.Country.DK;

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
        List<String> recessiveCountries = getRecessiveCountriesFromConfiguration(task);
        LOGGER.info("Recessive countries are {}", recessiveCountries);

        checkAndCorrectInconsistencies(task, recessiveCountries, task.getArtifact(TOPOLOGICAL_MERGE_FILE));
    }

    List<String> getRecessiveCountriesFromConfiguration(final MergingTask task) {
        if (task.getConfigurations().getRecessivityParameters().getPath() != null) {
            try {
                final RecessivityParameters params = JsonUtils.read(RecessivityParameters.class, task.getConfigurations().getRecessivityParameters().getPath());
                LOGGER.info("Recessive countries list is retrieved from {} file", task.getConfigurations().getRecessivityParameters().getPath());
                return params.getRecessiveCountries();
            } catch (final IOException e) {
                try {
                    LOGGER.warn("Recessive countries list is retrieved from default configuration file");
                    RecessivityParameters params = JsonUtils.read(RecessivityParameters.class, new ClassPathResource(RECESSIVITY_DEFAULT_CONFIGURATION).getInputStream());
                    return params.getRecessiveCountries();
                } catch (final IOException ex) {
                    LOGGER.warn("Error while reading default recessivity configuration file, no country will be considered recessive");
                    return new ArrayList<>();
                }
            }
        }
        return new ArrayList<>();
    }

    private void checkAndCorrectInconsistencies(final MergingTask task,
                                                final List<String> recessiveCountries,
                                                final Network network) {
        try {
            final XnodesCheck xnodesCheck = task.getArtifact(XNODES_INFORMATION_FILE, XnodesCheck.class);
            final List<XnodeConfig> xnodesConfigs = task.getConfigurations().getXnodeList();

            final XnodesInconsistencies xnodesInconsistencies = analyzeXnodes(network,
                                                                              xnodesCheck,
                                                                              xnodesConfigs,
                                                                              recessiveCountries);

            saveArtifactNetwork(TGM_FILE_AFTER_RECESSIVITY, network, task, UCTE_FORMAT, null, configuration);
            saveArtifactFile(XNODES_INCONSISTENCIES, xnodesInconsistencies, task, configuration);
            tasksRepository.save(task);
        } catch (Exception e) {
            String errorMessage = String.format("Xnodes check failed for task %d with target date %s, cause: %s", task.getId(), task.getInputs().getTargetDate(), e.getMessage());
            LOGGER.error(errorMessage);
            throw new CeMergingException(errorMessage, e);
        }
    }

    private XnodesInconsistencies analyzeXnodes(final Network network,
                                                final XnodesCheck xnodesCheck,
                                                final List<XnodeConfig> xnodeConfigs,
                                                final List<String> recessiveCountries) {
        List<XnodeIncorrect> incorrect = new ArrayList<>();
        List<XnodeIncomplete> incomplete = new ArrayList<>();

        if (xnodesCheck != null && !xnodesCheck.getXnodeInformationMap().isEmpty()) {
            incorrect = listIncorrectXnodes(network, xnodesCheck.getXnodeInformationMap(), recessiveCountries);
            checkAlegroXnodesInconsistencies(incorrect, xnodesCheck.getXnodeInformationMap(), recessiveCountries);
            incomplete = getIncompleteXnodes(xnodeConfigs, xnodesCheck.getXnodeInformationMap());
        }
        return new XnodesInconsistencies(incorrect, incomplete, new ArrayList<>());

    }

    void checkAlegroXnodesInconsistencies(final List<XnodeIncorrect> xnodeIncorrects,
                                          final Map<String, XnodeInformation> xnodeInfos,
                                          final List<String> recessiveCountries) {
        if (!xnodeInfos.containsKey(VIRTUAL_HUB_ALEGRO_DE_NODE_NAME)
            || !xnodeInfos.containsKey(VIRTUAL_HUB_ALEGRO_BE_NODE_NAME)) {
            return;
        }

        final XnodeStatus germanAlegroStatus = xnodeInfos.get(VIRTUAL_HUB_ALEGRO_DE_NODE_NAME).getArea1Information().getStatus();
        final XnodeStatus belgianAlegroStatus = xnodeInfos.get(VIRTUAL_HUB_ALEGRO_BE_NODE_NAME).getArea1Information().getStatus();

        if (germanAlegroStatus != belgianAlegroStatus) {
            boolean isBelgiumRecessive = recessiveCountries.contains(BE.name());
            boolean isD7Recessive = recessiveCountries.contains("D7");

            xnodeIncorrects.add(new XnodeIncorrect("XLI_OB1", BE.name(), belgianAlegroStatus, isBelgiumRecessive,
                                                   "D7", germanAlegroStatus, isD7Recessive, OPEN));
        }

    }

    private List<XnodeIncorrect> listIncorrectXnodes(final Network network,
                                                     final Map<String, XnodeInformation> xnodeInformationMap,
                                                     final List<String> recessiveCountries) {
        return xnodeInformationMap.entrySet()
            .stream()
            .filter(entry -> entry.getValue().isIncorrect())
            .map(info -> getXnodeIncorrect(network, info.getKey(), info.getValue(), recessiveCountries))
            .toList();
    }

    private XnodeIncorrect getXnodeIncorrect(final Network network,
                                             final String name,
                                             final XnodeInformation info,
                                             final List<String> recessiveCountries) {
        String country1 = "";
        XnodeStatus status1 = null;
        String country2 = "";
        XnodeStatus status2 = null;
        boolean area1IsRecessive = false;
        boolean area2IsRecessive = false;

        final boolean hasArea1Info = info.getArea1Information() != null;
        final boolean hasArea2Info = info.getArea2Information() != null;

        if (hasArea1Info) {
            country1 = info.getArea1Information().getCountry();
            status1 = info.getArea1Information().getStatus();
            area1IsRecessive = recessiveCountries.contains(country1);
        }
        if (hasArea2Info) {
            country2 = info.getArea2Information().getCountry();
            status2 = info.getArea2Information().getStatus();
            area2IsRecessive = recessiveCountries.contains(country2);
        }

        boolean isGermanInternalNode = hasArea1Info && hasArea2Info && isGermanInternalNode(info);

        final XnodeStatus mergedStatus;
        if (area1IsRecessive == area2IsRecessive) {
            mergedStatus = OPEN.equals(status1) || OPEN.equals(status2) ? OPEN : CLOSE;
        } else if (area1IsRecessive) {
            mergedStatus = status2;
            correctStatus(network, name, country1, mergedStatus, isGermanInternalNode);
        } else {
            mergedStatus = status1;
            correctStatus(network, name, country2, mergedStatus, isGermanInternalNode);
        }
        return new XnodeIncorrect(name,
                                  country1, status1, area1IsRecessive,
                                  country2, status2, area2IsRecessive,
                                  mergedStatus);
    }

    private List<XnodeIncomplete> getIncompleteXnodes(final List<XnodeConfig> xnodesConfig,
                                                      final Map<String, XnodeInformation> xnodeInformationMap) {

        return xnodeInformationMap.entrySet()
            .stream()
            .filter(entry -> entry.getValue().isIncomplete() && !isAlegroXnode(entry.getKey()))
            .map(entry -> fromXnodeInfo(xnodesConfig, entry.getKey(), entry.getValue()))
            .toList();
    }

    private XnodeIncomplete fromXnodeInfo(final List<XnodeConfig> xnodesConfig,
                                          final String xNodeName,
                                          final XnodeInformation info) {
        final boolean hasArea1Info = info.getArea1Information() != null;
        final AreaInformation existingInfo = hasArea1Info ? info.getArea1Information() : info.getArea2Information();

        final String countryAbsent = xnodesConfig.stream()
            .filter(xnode -> xnode.getName().equals(xNodeName))
            .findFirst()
            .map(cfg -> hasArea1Info ? cfg.getArea2() : cfg.getArea1())
            .orElse("");

        return new XnodeIncomplete(
            xNodeName, existingInfo.getNode(), existingInfo.getCountry(), countryAbsent, existingInfo.getStatus()
        );

    }

    private boolean isAlegroXnode(final String nodeName) {
        return nodeName.equals(VIRTUAL_HUB_ALEGRO_BE_NODE_NAME) || nodeName.equals(VIRTUAL_HUB_ALEGRO_DE_NODE_NAME);
    }

    private void correctStatus(final Network network,
                               final String nodeId,
                               final String countryName,
                               final XnodeStatus status,
                               final boolean isGermanInternalNode) {

        final Optional<Branch> toCorrect = isGermanInternalNode ?
            Optional.ofNullable(findCorrectBranch(network, nodeId, countryName))
            : network.getBranchStream().filter(isConnectedTo(nodeId)).findFirst();

        toCorrect.ifPresent(branch -> correctStatusBranch(status, getCountry(countryName), branch));
    }

    private Branch<?> findCorrectBranch(final Network network,
                                        final String nodeId,
                                        final String country) {
        // German internal nodes that are renamed in premerge step begins with "D", not "X"
        final String node = nodeId.substring(0, 7);
        final String nodeInBranch = nodeId.substring(1, 8);
        return network.getBranchStream()
            .filter(isConnectedTo(nodeInBranch))
            .filter(branch -> (getVoltageId1(branch).contains(node) && getVoltageId2(branch).startsWith(country))
                              || (getVoltageId2(branch).contains(node) && getVoltageId1(branch).startsWith(country)))
            .findFirst()
            .orElse(null);
    }

    private String getVoltageId1(final Branch<?> branch) {
        return branch.getTerminal1().getVoltageLevel().getId();
    }

    private String getVoltageId2(final Branch<?> branch) {
        return branch.getTerminal2().getVoltageLevel().getId();
    }

    private void correctStatusBranch(final XnodeStatus xnodeStatus,
                                     final Country country,
                                     final Branch<?> branch) {
        final Consumer<Terminal> alignStatusWithXnode = xnodeStatus == OPEN ? Terminal::disconnect : Terminal::connect;

        Arrays.stream(TwoSides.values())
            .filter(side -> country.equals(getCountrySide(branch, side)))
            .map(branch::getTerminal)
            .forEach(alignStatusWithXnode);
    }

    private Country getCountry(final String countryName) {
        if (GERMAN_TSO.contains(countryName)) {
            return DE;
        } else if (DANISH_TSO.equals(countryName)) {
            return DK;
        } else {
            return Country.valueOf(countryName);
        }
    }


    private boolean isGermanInternalNode(final XnodeInformation xnodeInformation) {
        return getCountry(xnodeInformation.getArea1Information().getCountry()) == DE
               && getCountry(xnodeInformation.getArea2Information().getCountry()) == DE;
    }
}
