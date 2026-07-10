/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.balances_adjustment.process;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static com.powsybl.iidm.network.ComponentConstants.MAIN_NUM;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

public final class LoadFlowUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoadFlowUtils.class);
    private static final String DIVERGENCE_MESSAGE = "%s load flow diverged on network %s";

    private LoadFlowUtils() {
        throw new AssertionError("Utility class should not be constructed");
    }

    public static void runLoadflow(final Network network,
                                   final Supplier<LoadFlow.Runner> loadFlowRunnerSupplier,
                                   final LoadFlowParameters loadFlowParameters) {
        final String id = network.getId();

        LoadFlowResult result = loadFlowRunnerSupplier.get().run(network, loadFlowParameters);
        boolean isDc = loadFlowParameters.isDc();

        if (loadFlowHasDiverged(result)) {
            LOGGER.warn(getDivergenceMessage(id, isDc));

            Optional.ofNullable(result.getLogs())
                .map(LoadFlowUtils::fromAsciiLogs)
                .ifPresent(LOGGER::error);

            if (!isDc) {
                LOGGER.warn("Switching to DC mode for network {}", id);
                loadFlowParameters.setDc(true);
                result = loadFlowRunnerSupplier.get().run(network, loadFlowParameters);

                if (loadFlowHasDiverged(result)) {
                    final String errorMessage = getDivergenceMessage(id, true);
                    LOGGER.error(errorMessage);
                    throw new CeMergingException(errorMessage);
                }

                loadFlowParameters.setDc(false); //should put in AC for the next computation
            }
        }

    }

    private static String getDivergenceMessage(final String networkId, final boolean isDc) {
        final String loadflowMode = isDc ? "DC" : "AC";
        return DIVERGENCE_MESSAGE.formatted(loadflowMode, networkId);
    }

    private static String fromAsciiLogs(final String logString) {
        return new String(logString.getBytes(US_ASCII));
    }

    /**
     * In default LF implementation, OK if the largest synchronous component converged
     */
    private static boolean loadFlowHasDiverged(final LoadFlowResult loadFlowResult) {
        return loadFlowResult.getComponentResults()
            .stream()
            .filter(LoadFlowUtils::isMainComponentResult)
            .collect(collectingAndThen(toList(), LoadFlowUtils::loadFlowHasDiverged));
    }

    private static boolean isMainComponentResult(final LoadFlowResult.ComponentResult componentResult) {
        return MAIN_NUM == componentResult.getSynchronousComponentNum();
    }

    private static boolean loadFlowHasDiverged(final List<LoadFlowResult.ComponentResult> loadFlowResults) {
        if (loadFlowResults.size() > 1) {
            throw new CeMergingException("Expecting no more than 1 main synchronous component in LoadFlowResult");
        }
        return loadFlowResults.isEmpty()
               || loadFlowResults.getFirst().getStatus() != LoadFlowResult.ComponentResult.Status.CONVERGED;
    }
}
