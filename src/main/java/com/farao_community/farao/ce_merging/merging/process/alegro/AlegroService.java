/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.alegro;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.util.JsonUtils;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.inputs.AlegroData;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.inputs.AlegroFlows;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.result.BciAlegroData;
import com.farao_community.farao.ce_merging.merging.process.forecast_netpositions.ReferenceProgram;
import com.farao_community.farao.ce_merging.merging.process.target_net_positions.bci.JsonBciOutputStructure;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.*;
import static com.farao_community.farao.ce_merging.common.util.FileStorageUtils.saveArtifactFile;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.ALEGRO_NET_POSITIONS;

@Service
public class AlegroService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AlegroService.class);
    private final CeMergingConfiguration configuration;

    public AlegroService(final CeMergingConfiguration configuration) {
        this.configuration = configuration;
    }

    public void checkAlegroXnodesQuality(final MergingTask task) {
        final String topologicalMergeFilePath = task.getArtifactPath(ArtifactType.TGM_FILE_AFTER_RECESSIVITY);
        final Network network = Network.read(topologicalMergeFilePath);
        final List<DanglingLine> alegroDanglingLinesList = getAlegroDanglingLines(network);
        final boolean alegroInOutage = isAlegroInOutage(alegroDanglingLinesList);
        final DanglingLine albeDanglingLine = getAlegroDanglingLine(alegroDanglingLinesList, VIRTUAL_HUB_ALEGRO_BE_NODE_NAME);
        final DanglingLine aldeDanglingLine = getAlegroDanglingLine(alegroDanglingLinesList, VIRTUAL_HUB_ALEGRO_DE_NODE_NAME);
        final double albeFlow = getPFromDanglingLine(albeDanglingLine);
        final double aldeFlow = getPFromDanglingLine(aldeDanglingLine);
        final int threshold = task.getInputs().getAlegroThreshold();
        if (alegroInOutage) {
            correctOutage(network, alegroDanglingLinesList, topologicalMergeFilePath);
        } else {
            checkFlowDirection(albeFlow, aldeFlow, threshold);
            checkFlowCompliance(albeFlow, aldeFlow, threshold);
        }
        final ReferenceProgram referenceProgram = JsonUtils.read(ReferenceProgram.class, task.getArtifactPath(ArtifactType.REFERENCE_PROGRAM_FORECAST_FILE));
        final AlegroData alegroData = getAlegroNetPositions(referenceProgram, alegroInOutage, albeFlow, aldeFlow, threshold);
        saveArtifactFile(ALEGRO_NET_POSITIONS, alegroData, task, configuration);
    }

    public void updateAlegroP0(final MergingTask task) {
        final AlegroData alegroData = JsonUtils.read(AlegroData.class, task.getArtifactPath(ArtifactType.ALEGRO_NET_POSITIONS));
        if (alegroData.alegroInOutage()) {
            return;
        }
        final JsonBciOutputStructure bciOutputs = JsonUtils.read(JsonBciOutputStructure.class, task.getArtifactPath(ArtifactType.BCI_OUTPUT_FILE));
        final BciAlegroData bciAlegroData = bciOutputs.getBciAlegroData();
        final double ec = getAlegroEcLimit(bciAlegroData);
        final double minEc = -ec;
        final double maxEc = ec;
        final double albeFinalFlow = calculateAlegroFinalFlow(bciAlegroData.albeFlows().targetFlow(), minEc, maxEc);
        final double aldeFinalFlow = calculateAlegroFinalFlow(bciAlegroData.aldeFlows().targetFlow(), minEc, maxEc);
        final String tgmPath = task.getArtifactPath(ArtifactType.TGM_FILE_AFTER_RECESSIVITY);
        final Network network = Network.read(tgmPath);
        final List<DanglingLine> alegroDanglingLinesList = getAlegroDanglingLines(network);
        final DanglingLine albeDanglingLine = getAlegroDanglingLine(alegroDanglingLinesList, VIRTUAL_HUB_ALEGRO_BE_NODE_NAME);
        albeDanglingLine.setP0(albeFinalFlow);
        LOGGER.info("ALBE load adjusted. Initial: {} Final: {}", alegroData.albeFlows().initialFlow(), albeFinalFlow);
        final DanglingLine aldeDanglingLine = getAlegroDanglingLine(alegroDanglingLinesList, VIRTUAL_HUB_ALEGRO_DE_NODE_NAME);
        aldeDanglingLine.setP0(aldeFinalFlow);
        LOGGER.info("ALDE load adjusted. Initial: {} Final: {}", alegroData.aldeFlows().initialFlow(), aldeFinalFlow);
        alegroDanglingLinesList.forEach(danglingLine -> {
            if (danglingLine.getGeneration() != null) {
                danglingLine.getGeneration().setTargetP(0);
            }
        });
        network.write("UCTE", null, Path.of(tgmPath));
    }

     boolean isAlegroInOutage(final List<DanglingLine> alegroDanglingLinesList) {
        final long numberOfConnectedAlegroXnodes = alegroDanglingLinesList.stream().filter(danglingLine -> danglingLine.getTerminal().isConnected()).count();
        if (numberOfConnectedAlegroXnodes == 0) {
            LOGGER.info("Both X nodes are disconnected, Alegro in outage");
            return true;
        }
        if (numberOfConnectedAlegroXnodes == 1) {
            LOGGER.warn("One X node is connected and one is disconnected, Alegro in outage");
            return true;
        }
        return false;
    }

     void correctOutage(final Network network, final List<DanglingLine> alegroDanglingLinesList, final String filePath) {
        alegroDanglingLinesList.forEach(danglingLine -> {
            danglingLine.setP0(0);
            if (danglingLine.getGeneration() != null) {
                danglingLine.getGeneration().setTargetP(0);
            }
        });
        LOGGER.info("Alegro in outage : ALBE load and ALDE load are set to 0");
        network.write("UCTE", null, Path.of(filePath));
    }

     void checkFlowDirection(final double albeFlow, final double aldeFlow, final int alegroThreshold) {
        if (!isAlegroLoadsBelowOrEqualToThreshold(albeFlow, aldeFlow, alegroThreshold) && isAlegroLoadsHasSameSign(albeFlow, aldeFlow)) {
            LOGGER.error("ALBE load and ALDE load have the same sign");
            throw new CeMergingException("ALBE load and ALDE load have the same sign");
        }
    }

    void checkFlowCompliance(final double albeFlow, final double aldeFlow, final int threshold) {
        final double differenceBetweenFlows = Math.abs(Math.abs(albeFlow) - Math.abs(aldeFlow));;
        if (differenceBetweenFlows > threshold) {
            final String formattedDifference = String.format(Locale.ROOT, "%.2f", differenceBetweenFlows);
            LOGGER.error("The difference between the two flows is {}. It's greater than the threshold which is {}", formattedDifference, threshold);
            throw new CeMergingException("The difference between Alegro loads is " + formattedDifference + ". It's greater than the threshold which is " + threshold);
        }
    }

    AlegroData getAlegroNetPositions(final ReferenceProgram referenceProgram, final boolean alegroInOutage, final double albeInitialFlow, final double aldeInitialFlow, final int threshold) {
        final double aldeTargetFlow = -getReferenceFlow(referenceProgram, VIRTUAL_HUB_ALEGRO_DE_EIC);
        final double albeTargetFlow = -getReferenceFlow(referenceProgram, VIRTUAL_HUB_ALEGRO_BE_EIC);
        final double gapNpfInitialFlowForAlegroBe = albeTargetFlow - albeInitialFlow;
        final double gapNpfInitialFlowForAlegroDe = aldeTargetFlow - aldeInitialFlow;
        if (!alegroInOutage) {
            LOGGER.info("ALBE load : initial: {} , target: {}", albeInitialFlow, albeTargetFlow);
            checkAlegroFlowGap(gapNpfInitialFlowForAlegroBe, threshold, "ALBE");
            LOGGER.info("ALDE load : initial: {} , target: {}", aldeInitialFlow, aldeTargetFlow);
            checkAlegroFlowGap(gapNpfInitialFlowForAlegroDe, threshold, "ALDE");
        }
        final AlegroFlows aldeFlows = new AlegroFlows(aldeInitialFlow, aldeTargetFlow, gapNpfInitialFlowForAlegroDe);
        final AlegroFlows albeFlows = new AlegroFlows(albeInitialFlow, albeTargetFlow, gapNpfInitialFlowForAlegroBe);
        return new AlegroData(alegroInOutage, aldeFlows, albeFlows);
    }

    private boolean isAlegroLoadsHasSameSign(final double albeFlow, final double aldeFlow) {
        return (albeFlow > 0 && aldeFlow > 0) || (albeFlow < 0 && aldeFlow < 0);
    }

    private boolean isAlegroLoadsBelowOrEqualToThreshold(final double albeFlow, final double aldeFlow, final int threshold) {
        return Math.abs(albeFlow) <= threshold && Math.abs(aldeFlow) <= threshold;
    }

    private List<DanglingLine> getAlegroDanglingLines(final Network network) {
        return network.getDanglingLineStream()
                .filter(danglingLine ->
                        VIRTUAL_HUB_ALEGRO_BE_NODE_NAME.equals(danglingLine.getPairingKey())
                                || VIRTUAL_HUB_ALEGRO_DE_NODE_NAME.equals(danglingLine.getPairingKey()))
                .collect(Collectors.toList());
    }

    private DanglingLine getAlegroDanglingLine(final List<DanglingLine> alegroDanglingLinesList, final String pairingKey) {
        return alegroDanglingLinesList.stream()
                .filter(danglingLine -> pairingKey.equals(danglingLine.getPairingKey()))
                .findFirst()
                .orElseThrow(() -> new CeMergingException("No dangling line found for: " + pairingKey));
    }

    private void checkAlegroFlowGap(final double gap, final double threshold, final String alegroNode) {
        if (Math.abs(gap) <= threshold) {
            return;
        }
        final String formattedGap = String.format(Locale.ROOT, "%.2f", Math.abs(gap));
        LOGGER.error("The absolute value of the difference between the net position forecast value and the initial net position for {} is {}. It exceeds the threshold which is {}", alegroNode, formattedGap, threshold);
        throw new CeMergingException("The " + alegroNode + " flow gap is " + formattedGap + ", exceeding the threshold of " + threshold);
    }

    private double calculateAlegroFinalFlow(final double targetFlow, final double minEc, final double maxEc) {
        if (targetFlow > maxEc) {
            return maxEc;
        }
        if (targetFlow < minEc) {
            return minEc;
        }
        return targetFlow;
    }

    private double getAlegroEcLimit(final BciAlegroData bciAlegroData) {
        double ec;
        if (bciAlegroData.albeFlows().targetFlow() < 0) {
            ec = Math.min(Math.abs(bciAlegroData.albeFlows().maxEc()), Math.abs(bciAlegroData.aldeFlows().minEc()));
        } else {
            ec = Math.min(Math.abs(bciAlegroData.albeFlows().minEc()), Math.abs(bciAlegroData.aldeFlows().maxEc()));
        }
        return ec;
    }

    private double getPFromDanglingLine(final DanglingLine danglingLine) {
        return danglingLine.getGeneration() == null ? danglingLine.getP0() : danglingLine.getP0() - danglingLine.getGeneration().getTargetP();
    }

    private double getReferenceFlow(final ReferenceProgram referenceProgram, final String areaOutId) {
        return referenceProgram.getReferenceExchangeDataList()
                .stream()
                .filter(referenceExchangeData ->
                        areaOutId.equals(referenceExchangeData.getAreaOutId()))
                .findFirst()
                .orElseThrow(() -> new CeMergingException("No reference exchange found for : " + areaOutId))
                .getFlow();
    }
}
