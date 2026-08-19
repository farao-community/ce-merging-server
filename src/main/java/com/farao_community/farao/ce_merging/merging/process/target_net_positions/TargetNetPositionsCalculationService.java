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
import com.farao_community.farao.ce_merging.common.util.JsonUtils;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.inputs.AlegroData;
import com.farao_community.farao.ce_merging.merging.process.target_net_positions.balances_adjustment.AreaNetPosition;
import com.farao_community.farao.ce_merging.merging.process.target_net_positions.balances_adjustment.BalancesAdjustmentTarget;
import com.farao_community.farao.ce_merging.merging.process.target_net_positions.bci.JsonBciOutputStructure;
import com.farao_community.farao.ce_merging.merging.process.virtuals_hubs.VirtualHubsShifting;
import com.farao_community.farao.ce_merging.merging.task.MergingTaskRepository;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.ALEGRO_NET_POSITIONS;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.BALANCES_ADJUSTMENT_TARGET_FILE;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.BCI_OUTPUT_FILE;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.IGMS_NET_POSITIONS_FILE;

@Service
public class TargetNetPositionsCalculationService {
    private static final String VIRTUAL_HUB_PREFIX = "X";
    private static final String BELGIUM_COUNTRY_CODE = "BE";
    private static final String GERMANY_COUNTRY_CODE = "DE";
    private static final Logger LOGGER = LoggerFactory.getLogger(TargetNetPositionsCalculationService.class);
    private final MergingTaskRepository repository;
    private final CeMergingConfiguration configuration;

    public TargetNetPositionsCalculationService(MergingTaskRepository repository,
                                                CeMergingConfiguration configuration) {
        this.repository = repository;
        this.configuration = configuration;
    }

    public void computeTargetNetPositions(final MergingTask task) {
        try {
            final Map<String, Double> virtualHubsGaps = VirtualHubsShifting.applyVirtualHubFlows(task, configuration);
            final Map<String, Double> targetNetPositionsWithoutHvdc = getTargetNetPositionsFromBciOutput(task.getArtifactPath(BCI_OUTPUT_FILE));
            final Map<String, Double> outBciFlowsByCountry = getAdjustedOutBciNetPositions(task.getArtifactPath(IGMS_NET_POSITIONS_FILE), task.getArtifactPath(ALEGRO_NET_POSITIONS), virtualHubsGaps);
            final List<AreaNetPosition> targetNetPositionsWithHvdc = calculateTargetNetPositionsWithHvdc(targetNetPositionsWithoutHvdc, outBciFlowsByCountry);
            final BalancesAdjustmentTarget balancesAdjustmentTarget = new BalancesAdjustmentTarget(targetNetPositionsWithHvdc);
            FileStorageUtils.saveArtifactFile(BALANCES_ADJUSTMENT_TARGET_FILE, balancesAdjustmentTarget, task, configuration);
            repository.save(task);
        } catch (final Exception e) {
            final String errorMessage = String.format("Balances adjustment: Target net positions calculation failed for task %d with target date %s, cause: %s", task.getId(), task.getInputs().getTargetDate(), e.getMessage());
            LOGGER.error(errorMessage, e);
            throw new CeMergingException(errorMessage, e);
        }
    }

    private static List<AreaNetPosition> calculateTargetNetPositionsWithHvdc(final Map<String, Double> targetNetPositionsWithoutHvdc, final Map<String, Double> outBciFlowsByCountry) {
        return targetNetPositionsWithoutHvdc.keySet().stream()
                .map(country -> new AreaNetPosition(
                        country,
                        targetNetPositionsWithoutHvdc.getOrDefault(country, 0.0)
                                + outBciFlowsByCountry.getOrDefault(country, 0.0)
                ))
                .toList();
    }

    private static Map<String, Double> getAdjustedOutBciNetPositions(final String igmsNetPositionsPath,
                                                                     final String alegroNetPositionsPath,
                                                                     final Map<String, Double> virtualHubsGaps) {
        final Map<String, Double> adjustedOutBciNetPositions = new TreeMap<>();
        final NetPositionsResults initialNetPosition = JsonUtils.read(NetPositionsResults.class, igmsNetPositionsPath);
        final AlegroData alegroNetPosition = JsonUtils.read(AlegroData.class, alegroNetPositionsPath);

        // As Alegro target flow is set in the network  --> for BE and DE : target NP must be used for alegro instead of initial NP
        // As the getOutBciNetPosition contains already Alegro initial NP, we must add the GapNpfInitialFlow (= target NP alegro - initial NP alegro)
        initialNetPosition.netPositionsByCountryMap().forEach((country, netPositions) ->
            adjustedOutBciNetPositions.put(country, calculateOutBciNetPosition(country, netPositions, alegroNetPosition)));
        // Adapt target after virtual hubs shifting
        applyVirtualHubsGaps(adjustedOutBciNetPositions, virtualHubsGaps);

        return adjustedOutBciNetPositions;
    }

    private static void applyVirtualHubsGaps(final Map<String, Double> results, final Map<String, Double> virtualHubsGaps) {

        virtualHubsGaps.forEach((country, gap) ->
                results.put(country, results.getOrDefault(country, 0.0) - gap
                )
        );
    }

    private static double calculateOutBciNetPosition(final String country, final NetPositions netPositions, final AlegroData alegroNetPosition) {
        return switch (country) {
            case BELGIUM_COUNTRY_CODE ->
                    netPositions.getOutBciNetPosition() + alegroNetPosition.albeFlows().gapNpfInitialFlow();
            case GERMANY_COUNTRY_CODE ->
                    // DE contains a virtual hub that does not start with "X" (e.g. D2HWKR1D)
                    // powsybl-balances-adjustment excludes such nodes when calculating the net position
                    // Therefore, their flow must be subtracted from the DE target Balance net position
                    // to remain consistent with powsybl-balances-adjustment
                    netPositions.getOutBciNetPosition() + alegroNetPosition.aldeFlows().gapNpfInitialFlow() - getGermanNonXVirtualHubFlows(netPositions);

            default -> netPositions.getOutBciNetPosition();
        };
    }

    private static double getGermanNonXVirtualHubFlows(final NetPositions netPositions) {
        return netPositions.getVirtualHubsExchanges().entrySet().stream()
                .filter(entry -> !entry.getKey().startsWith(VIRTUAL_HUB_PREFIX))
                .mapToDouble(Map.Entry::getValue)
                .sum();
    }

    private static Map<String, Double> getTargetNetPositionsFromBciOutput(final String bciOutputPath) {
        final Map<String, Double> targetNetPositions = new TreeMap<>();
        final JsonBciOutputStructure bciOutput = JsonUtils.read(JsonBciOutputStructure.class, bciOutputPath);

        bciOutput.getJsonBciComputationResult().getBciResults().forEach((key, value) -> {
            targetNetPositions.put(key, value.getJsonGlobalNetPositions().getTarget());
        });
        targetNetPositions.putAll(bciOutput.getJsonOutRegionResults().getGlobalForecastNetPositions());

        return targetNetPositions;
    }
}

