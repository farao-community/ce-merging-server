/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.util;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.loadflow.LoadFlowRunParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Supplier;

import static com.powsybl.iidm.network.ComponentConstants.MAIN_NUM;
import static com.powsybl.loadflow.LoadFlowParameters.BalanceType.PROPORTIONAL_TO_GENERATION_P;
import static com.powsybl.loadflow.LoadFlowParameters.BalanceType.PROPORTIONAL_TO_LOAD;
import static com.powsybl.loadflow.LoadFlowResult.ComponentResult.Status.CONVERGED;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

public final class LoadFlowUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoadFlowUtils.class);
    private static final String DIVERGENCE_MESSAGE = "%s load flow diverged on network %s";

    private LoadFlowUtils() {
        throw new AssertionError("Utility class should not be constructed");
    }

    public static void runLoadFlow(final Network network,
                                   final Supplier<LoadFlow.Runner> loadFlowRunnerSupplier,
                                   final LoadFlowParameters loadFlowParameters) {
        runLoadFlow(network, loadFlowRunnerSupplier, new LoadFlowRunParameters().setParameters(loadFlowParameters));
    }

    public static void runLoadFlowWithBalanceTypeCorrection(final Network network,
                                                            final Supplier<LoadFlow.Runner> runnerSupplier,
                                                            final LoadFlowParameters parameters) {
        final double totalTargetP = network.getGeneratorStream().mapToDouble(Generator::getTargetP).sum();
        if (totalTargetP == 0 && parameters.getBalanceType().equals(PROPORTIONAL_TO_GENERATION_P)) {
            // We copy the loadflowParameters to not impact the next computation
            final LoadFlowParameters withBalanceTypeLoad = parameters.copy();
            withBalanceTypeLoad.setBalanceType(PROPORTIONAL_TO_LOAD);
            LOGGER.info("Running loadflow with BalanceType PROPORTIONAL_TO_LOAD for network {}", network.getNameOrId());
            runLoadFlow(network, runnerSupplier, withBalanceTypeLoad);
        } else {
            runLoadFlow(network, runnerSupplier, parameters);
        }
    }

    private static LoadFlowResult runLoadFlow(final Network network,
                                              final Supplier<LoadFlow.Runner> runnerSupplier,
                                              final LoadFlowRunParameters parameters) {
        try {
            LoadFlowParameters loadFlowParameters = parameters.getLoadFlowParameters();
            boolean initialDcMode = loadFlowParameters.isDc();

            LOGGER.info("Running {} loadflow on network {}",  loadFlowParameters.isDc() ? "DC" : "AC", network.getId());
            LoadFlowResult result = runnerSupplier.get().run(network, parameters);

            if (hasDiverged(result)) {
                final String warningMessage = divergenceMessage(network, initialDcMode);
                LOGGER.warn(warningMessage);

                if (result.getLogs() != null) {
                    final String errorMessage = new String(result.getLogs().getBytes(US_ASCII));
                    LOGGER.error(errorMessage);
                }

                if (!initialDcMode) {
                    return runDcFallback(network, runnerSupplier, parameters);
                }
            }

            return result;
        } catch (final Exception e) {
            throw new CeMergingException(String.format("Loadflow exception on network %s, cause: %s", network.getId(), e.getMessage()), e);
        }
    }

    private static LoadFlowResult runDcFallback(final Network network,
                                                final Supplier<LoadFlow.Runner> runnerSupplier,
                                                final LoadFlowRunParameters parameters) {
        LOGGER.warn("AC loadflow did not converge, Switching to DC loadflow mode for network {}", network.getId());

        // We copy the loadflowParameters to not impact the next computation
        final LoadFlowParameters dcParams = parameters.getLoadFlowParameters().copy();
        dcParams.setDc(true);
        parameters.setParameters(dcParams);

        final LoadFlowResult dcResult = runnerSupplier.get().run(network, parameters);

        if (hasDiverged(dcResult)) {
            final String errorMessage = divergenceMessage(network, true);
            LOGGER.error(errorMessage);
            throw new CeMergingException(errorMessage);
        }

        return dcResult;
    }

    private static String divergenceMessage(final Network network, final boolean isDC) {
        return DIVERGENCE_MESSAGE.formatted(isDC ? "DC" : "AC", network.getId());
    }

    /**
     * In default LF implementation, OK if the largest synchronous component converged
     */
    private static boolean hasDiverged(final LoadFlowResult loadFlowResult) {
        return loadFlowResult.getComponentResults()
            .stream()
            .filter(LoadFlowUtils::isMainComponentResult)
            .collect(collectingAndThen(toList(), LoadFlowUtils::hasDiverged));
    }

    private static boolean isMainComponentResult(final LoadFlowResult.ComponentResult componentResult) {
        return MAIN_NUM == componentResult.getSynchronousComponentNum();
    }

    private static boolean hasDiverged(final List<LoadFlowResult.ComponentResult> loadFlowResults) {
        if (loadFlowResults.size() > 1) {
            throw new CeMergingException("Expecting no more than 1 main synchronous component in LoadFlowResult");
        }
        return loadFlowResults.isEmpty() || loadFlowResults.getFirst().getStatus() != CONVERGED;
    }
}
