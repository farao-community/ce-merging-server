/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.target_net_positions;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.model.netpositions.NetPositions;
import com.farao_community.farao.ce_merging.common.model.netpositions.NetPositionsResults;
import com.farao_community.farao.ce_merging.common.util.FileStorageUtils;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.FlowByAreaMap;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.inputs.AlegroData;
import com.farao_community.farao.ce_merging.merging.process.target_net_positions.balances_adjustment.AreaNetPosition;
import com.farao_community.farao.ce_merging.merging.process.target_net_positions.balances_adjustment.BalancesAdjustmentTarget;
import com.farao_community.farao.ce_merging.merging.process.target_net_positions.bci.JsonBciOutputStructure;
import com.farao_community.farao.ce_merging.merging.process.virtuals_hubs.VirtualHubsShifting;
import com.farao_community.farao.ce_merging.merging.task.MergingTaskRepository;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.powsybl.iidm.network.Country;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;

import static com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.FlowByAreaMap.toFlowByAreaMap;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.ALEGRO_NET_POSITIONS;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.BALANCES_ADJUSTMENT_TARGET_FILE;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.BCI_OUTPUT_FILE;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.IGMS_NET_POSITIONS_FILE;

@Service
public class TargetNetPositionsCalculationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TargetNetPositionsCalculationService.class);
    private static final String XNODES_PREFIX = "X";
    private final MergingTaskRepository repository;
    private final CeMergingConfiguration configuration;

    public TargetNetPositionsCalculationService(final MergingTaskRepository repository,
                                                final CeMergingConfiguration configuration) {
        this.repository = repository;
        this.configuration = configuration;
    }

    public void computeTargetNetPositions(final MergingTask task) {
        try {
            final BalancesAdjustmentTarget balancesAdjustmentTarget = new BalancesAdjustmentTarget(
                    calculateTargetNetPositionsWithHvdc(getTargetNetPositionsFromBciOutput(task),
                                                        getAdjustedOutBciNetPositions(task))
            );
            FileStorageUtils.saveArtifactFile(BALANCES_ADJUSTMENT_TARGET_FILE, balancesAdjustmentTarget, task, configuration);
            repository.save(task);
        } catch (final Exception e) {
            final String errorMessage = String.format("Balances adjustment: Target net positions calculation failed for task %d with target date %s, cause: %s", task.getId(), task.getInputs().getTargetDate(), e.getMessage());
            LOGGER.error(errorMessage, e);
            throw new CeMergingException(errorMessage, e);
        }
    }

    private static List<AreaNetPosition> calculateTargetNetPositionsWithHvdc(final FlowByAreaMap targetNetPositionsWithoutHvdc,
                                                                             final FlowByAreaMap outBciFlowsByCountry) {
        return targetNetPositionsWithoutHvdc
                .withValuesShiftedBy(outBciFlowsByCountry::getOrZero)
                .toAreaNetPositions();
    }

    private FlowByAreaMap getAdjustedOutBciNetPositions(final MergingTask task) throws FileNotFoundException {
        final NetPositionsResults initialNetPosition = task.getArtifact(IGMS_NET_POSITIONS_FILE, NetPositionsResults.class);
        final AlegroData alegroNetPosition = task.getArtifact(ALEGRO_NET_POSITIONS, AlegroData.class);
        final FlowByAreaMap virtualHubsGaps = new FlowByAreaMap(VirtualHubsShifting.applyVirtualHubFlows(task, configuration));

        // As Alegro target flow is set in the network  --> for BE and DE : target NP must be used for alegro instead of initial NP
        // As the getOutBciNetPosition contains already Alegro initial NP, we must add the GapNpfInitialFlow (= target NP alegro - initial NP alegro)

        final FlowByAreaMap adjustedOutBciNetPositions = initialNetPosition.netPositionsByCountryMap()
                .entrySet().stream()
                .collect(toFlowByAreaMap(Map.Entry::getKey,
                                         e -> calculateOutBciNetPosition(e.getKey(), e.getValue(), alegroNetPosition)));

        // Adapt target after virtual hubs shifting
        applyVirtualHubsGaps(adjustedOutBciNetPositions, virtualHubsGaps);

        return adjustedOutBciNetPositions;
    }

    private static void applyVirtualHubsGaps(final FlowByAreaMap results,
                                             final FlowByAreaMap virtualHubsGaps) {
        results.shiftAllFlowsWith(country -> -virtualHubsGaps.get(country));
    }

    private static double calculateOutBciNetPosition(final String country,
                                                     final NetPositions netPositions,
                                                     final AlegroData alegroNetPosition) {
        // DE contains a virtual hub that does not start with "X" (e.g. D2HWKR1D)
        // powsybl-balances-adjustment excludes such nodes when calculating the net position
        // Therefore, their flow must be subtracted from the DE target Balance net position
        // to remain consistent with powsybl-balances-adjustment
        return netPositions.getOutBciNetPosition() + switch (Country.valueOf(country)) {
            case BE -> alegroNetPosition.albeFlows().gapNpfInitialFlow();
            case DE -> alegroNetPosition.aldeFlows().gapNpfInitialFlow() - getGermanNonXVirtualHubFlows(netPositions);
            default -> 0;
        };
    }

    private static double getGermanNonXVirtualHubFlows(final NetPositions netPositions) {
        return netPositions.getVirtualHubsExchanges().entrySet().stream()
                .filter(entry -> !entry.getKey().startsWith(XNODES_PREFIX))
                .mapToDouble(Map.Entry::getValue)
                .sum();
    }

    private static FlowByAreaMap getTargetNetPositionsFromBciOutput(final MergingTask task) throws FileNotFoundException {

        final JsonBciOutputStructure bciOutput = task.getArtifact(BCI_OUTPUT_FILE, JsonBciOutputStructure.class);

        final FlowByAreaMap targetNetPositions = bciOutput.getJsonBciComputationResult()
                .getBciResults()
                .entrySet().stream()
                .collect(toFlowByAreaMap(Map.Entry::getKey,
                                         e -> e.getValue().getJsonGlobalNetPositions().getTarget()));

        targetNetPositions.putAll(bciOutput.getJsonOutRegionResults().getGlobalForecastNetPositions());

        return targetNetPositions;
    }
}

