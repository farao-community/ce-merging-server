/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.hvdc_alignment;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.util.CountryCodeUtils;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.VirtualHubsAlignmentCouple;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.ZeroFlowNode;
import com.farao_community.farao.ce_merging.merging.task.MergingTaskRepository;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.farao_community.farao.ce_merging.merging.task.entities.VirtualHubRecord;
import com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.*;

@Service
public class HvdcXNodeAlignmentService {
    private static final Logger LOGGER = LoggerFactory.getLogger(HvdcXNodeAlignmentService.class);

    private final CeMergingConfiguration configuration;
    private final MergingTaskRepository repository;

    public HvdcXNodeAlignmentService(CeMergingConfiguration configuration, MergingTaskRepository repository) {
        this.configuration = configuration;
        this.repository = repository;
    }

    public void setZeroFlowNodes(final MergingTask task) {
        final List<ZeroFlowNode> zeroFlowNodes = task.getConfigurations().getZeroFlowNodes();
        final String location = String.format("/tasks/%d/artifacts/igms-after-update-xnode-flows", task.getId());

        zeroFlowNodes.forEach(zeroFlowNode -> {
            final String nodeName = zeroFlowNode.getXnode();
            final String country = zeroFlowNode.getCountryCode();
            getNetworkForCountry(task, country).ifPresentOrElse(
                    network -> updateDanglingLineFlow(task, network, nodeName, country, location),
                    () -> LOGGER.warn("Unable to find the IGM associated to the country {}, The xnode {} flow will not be set to 0", country, nodeName));
        });
        repository.save(task);
    }

    public void applyHvdcXNodeAlignment(final MergingTask task) {
        final List<VirtualHubsAlignmentCouple> alignmentCouples = task.getConfigurations().getVirtualHubsAlignmentCouples();
        final List<VirtualHubRecord> virtualHubRecords = task.getConfigurations().getVirtualHubList();
        final String location = String.format("/tasks/%d/artifacts/igms-after-hvdc-alignment", task.getId());
        alignmentCouples.forEach(couple -> applyAlignment(task, couple, virtualHubRecords, location));
        repository.save(task);
    }

    private void applyAlignment(final MergingTask task,
                                final VirtualHubsAlignmentCouple couple,
                                final List<VirtualHubRecord> virtualHubRecords,
                                final String location) {
        final String referenceXNode = couple.getReferenceXNode();
        final String recessiveXNode = couple.getRecessiveXNode();
        final String referenceCountry = getVirtualHubCountry(virtualHubRecords, referenceXNode);
        final String recessiveCountry = getVirtualHubCountry(virtualHubRecords, recessiveXNode);
        final Network referenceNetwork = getNetworkOrThrow(task, referenceCountry, referenceXNode);
        final Network recessiveNetwork = getNetworkOrThrow(task, recessiveCountry, recessiveXNode);
        HvdcXNodeAlignment.on(referenceNetwork, recessiveNetwork, couple).align();
        saveNetworkInArtifacts(task, referenceNetwork, referenceCountry, location);
        saveNetworkInArtifacts(task, recessiveNetwork, recessiveCountry, location);
    }

    private static String getVirtualHubCountry(final List<VirtualHubRecord> virtualHubRecords,
                                               final String nodeName) {
        VirtualHubRecord virtualHubRecord = virtualHubRecords.stream()
                .filter(vhRecord -> vhRecord.getNodeName().equals(nodeName))
                .findFirst()
                .orElseThrow(() -> {
                    String errorMessage = "Could not find node " + nodeName + " in virtual hub config";
                    LOGGER.error(errorMessage);
                    return new CeMergingException(errorMessage);
                });

        return CountryCodeUtils.mapDk1ToDk(virtualHubRecord.getRelatedMaCode());
    }

    private static Network getNetworkOrThrow(final MergingTask task, final String country, final String nodeName) {
        return getNetworkForCountry(task, country).orElseThrow(() -> {
            String errorMessage = "Unable to find the IGM associated to the country " + country + " for node " + nodeName;
            LOGGER.error(errorMessage);
            return new CeMergingException(errorMessage);
        });
    }

    private static Optional<Network> getNetworkForCountry(final MergingTask task,
                                                          final String country) {
        final String path = switch (country) {
            case GERMAN_COUNTRY_CODE -> task.getArtifacts().getFile(ArtifactType.GERMAN_PRE_MERGED_IGM).getPath();
            case DENMARK_COUNTRY_CODE -> task.getArtifacts().getFile(ArtifactType.DK_CONVERTED_FILE).getPath();
            default -> Optional.ofNullable(task.getArtifacts().getPreTreatedIgmMap().get(country))
                    .orElseGet(() -> getInputIgmPath(task, country))
                    .getPath();
        };
        return path != null ? Optional.of(Network.read(path)) : Optional.empty();
    }

    private static SavedFile getInputIgmPath(final MergingTask task, final String country) {
        return task.getInputs().getIgms().stream()
                .filter(igmData -> igmData.getCountry().equals(country))
                .findFirst()
                .map(igmData -> igmData.getIgmFile())
                .orElse(null);
    }

    private void updateDanglingLineFlow(final MergingTask task, final Network network, final String nodeName, final String country, final String location) {
        network.getDanglingLineStream()
                .filter(danglingLine -> danglingLine.getPairingKey().equals(nodeName))
                .findFirst()
                .ifPresentOrElse(
                        danglingLine -> {
                            setDanglingLineToZeroFlow(danglingLine);
                            LOGGER.info("Set {} flow to 0.0", nodeName);
                            saveNetworkInArtifacts(task, network, country, location);
                        },
                        () -> LOGGER.warn(
                                "Could not update XNode flows, dangling line {} not found in {} network",
                                nodeName,
                                network.getNameOrId()
                        ));
    }

    private static void setDanglingLineToZeroFlow(final DanglingLine danglingLine) {
        danglingLine.setP0(0.0);
        HvdcXNodeAlignment.requireGeneration(danglingLine, "dangling line").setTargetP(0.0);
    }

    private void saveNetworkInArtifacts(final MergingTask task,
                                        final Network network,
                                        final String country,
                                        final String location) {
        final String networkFilename = network.getNameOrId() + ".uct";
        final Path modifiedPath = Paths.get(configuration.getArtifactsDirectoryPath(task), networkFilename);
        network.write(UCTE_FORMAT, null, modifiedPath);
        final SavedFile savedFile = new SavedFile(networkFilename, modifiedPath.toString(), location);
        if (GERMAN_COUNTRY_CODE.equals(country)) {
            task.getArtifacts().putFile(ArtifactType.GERMAN_PRE_MERGED_IGM, savedFile);
        } else if (DENMARK_COUNTRY_CODE.equals(country)) {
            task.getArtifacts().putFile(ArtifactType.DK_CONVERTED_FILE, savedFile);
        } else {
            task.getArtifacts().getPreTreatedIgmMap().put(country, savedFile);
        }
    }
}
