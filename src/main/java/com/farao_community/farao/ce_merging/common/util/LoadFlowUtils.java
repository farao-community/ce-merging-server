/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.util;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Injection;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.loadflow.LoadFlowRunParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.AC;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.DC;
import static com.farao_community.farao.ce_merging.common.util.NetworkUtil.getLeavingFlow;
import static com.farao_community.farao.ce_merging.common.util.NetworkUtil.isInMainConnectedComponent;
import static com.powsybl.iidm.network.ComponentConstants.MAIN_NUM;
import static com.powsybl.loadflow.LoadFlowParameters.BalanceType.PROPORTIONAL_TO_GENERATION_P;
import static com.powsybl.loadflow.LoadFlowParameters.BalanceType.PROPORTIONAL_TO_LOAD;
import static com.powsybl.loadflow.LoadFlowParameters.ComponentMode.MAIN_CONNECTED;
import static com.powsybl.loadflow.LoadFlowResult.ComponentResult.Status.CONVERGED;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

public final class LoadFlowUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoadFlowUtils.class);
    private static final String DIVERGENCE_MESSAGE = "%s load flow diverged on network %s";
    private static final String COMPONENT_NUMBER_ERROR = "Component number parameter should be 0 or 1";

    private LoadFlowUtils() {
        /* This utility class should not be instantiated */
    }

    public static LoadFlowResult runLoadFlow(final Network network,
                                             final Supplier<LoadFlow.Runner> runnerSupplier,
                                             final LoadFlowParameters parameters) {
        return runLoadFlow(network, runnerSupplier, new LoadFlowRunParameters().setParameters(parameters));
    }

    public static LoadFlowResult runLoadFlow(final Network network,
                                             final Supplier<LoadFlow.Runner> runnerSupplier,
                                             final LoadFlowRunParameters runParameters) {
        final String id = network.getId();

        final LoadFlowParameters parameters = runParameters.getLoadFlowParameters();
        LoadFlowResult result = runnerSupplier.get().run(network, runParameters);
        boolean isDc = parameters.isDc();

        if (loadFlowHasDiverged(result)) {
            LOGGER.warn(getDivergenceMessage(id, parameters));

            Optional.ofNullable(result.getLogs())
                    .map(log -> new String(log.getBytes(US_ASCII)))
                    .ifPresent(LOGGER::error);

            if (!isDc) { //DC fallback
                LOGGER.warn("Switching to DC mode for network {}", id);
                parameters.setDc(true);
                result = runnerSupplier.get().run(network, runParameters);

                if (loadFlowHasDiverged(result)) {
                    final String errorMessage = getDivergenceMessage(id, parameters);
                    LOGGER.error(errorMessage);
                    throw new CeMergingException(errorMessage);
                }

                parameters.setDc(false); //should put in AC for the next computation
            }
        }

        return result;
    }

    public static void runLoadFlowWithBalanceTypeCorrection(final Network network,
                                                            final Supplier<LoadFlow.Runner> runnerSupplier,
                                                            final LoadFlowParameters parameters) {

        LoadFlowParameters actualParameters = parameters;
        if (parameters.getBalanceType() == PROPORTIONAL_TO_GENERATION_P && hasBalancedGeneration(network)) {
            // We copy the parameters to not impact the next computation
            final LoadFlowParameters withBalanceTypeLoad = parameters.copy();
            withBalanceTypeLoad.setBalanceType(PROPORTIONAL_TO_LOAD);
            LOGGER.info("Running loadflow with BalanceType PROPORTIONAL_TO_LOAD for network {}", network.getNameOrId());
            actualParameters = withBalanceTypeLoad;
        }

        runLoadFlow(network, runnerSupplier, actualParameters);
    }

    public static LoadFlowResult runLoadFlowWithLogs(final Network network,
                                                     final Supplier<LoadFlow.Runner> runnerSupplier,
                                                     final LoadFlowParameters parameters,
                                                     final ReportNode reportNode) {
        parameters.setReadSlackBus(true); // the loadflow will use the slack node of the network for compensation,
        final LoadFlowRunParameters runParameters = new LoadFlowRunParameters().setParameters(parameters).setReportNode(reportNode);
        return runLoadFlow(network, runnerSupplier, runParameters);
    }

    private static boolean hasBalancedGeneration(final Network network) {
        return 0 == network.getGeneratorStream().mapToDouble(Generator::getTargetP).sum();
    }

    private static String getDivergenceMessage(final String networkId,
                                               final LoadFlowParameters parameters) {
        return DIVERGENCE_MESSAGE.formatted(getLoadFlowMode(parameters), networkId);
    }

    public static String getLoadFlowMode(final LoadFlowParameters parameters) {
        return parameters.isDc() ? DC : AC;
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

    private static boolean loadFlowHasDiverged(final List<LoadFlowResult.ComponentResult> results) {
        if (results.size() > 1) {
            throw new CeMergingException("Expecting no more than 1 main synchronous component in LoadFlowResult");
        }
        return results.isEmpty() || results.getFirst().getStatus() != CONVERGED;
    }

    public static Predicate<Injection> isConnected(final LoadFlowParameters.ComponentMode componentMode) {
        return injection -> isTerminalConnected(injection.getTerminal(), componentMode);
    }

    private static boolean isTerminalConnected(final Terminal terminal,
                                               final LoadFlowParameters.ComponentMode componentMode) {
        final Terminal.BusView busView = terminal != null ? terminal.getBusView() : null;
        final Bus bus = busView != null ? busView.getBus() : null;
        final boolean terminalConnectedToBus = terminal != null && terminal.isConnected() && bus != null;

        return switch (componentMode) {
            case MAIN_CONNECTED -> terminalConnectedToBus && bus.isInMainSynchronousComponent();
            case ALL_CONNECTED -> terminalConnectedToBus;
            default -> throw new CeMergingException(COMPONENT_NUMBER_ERROR);
        };
    }

    public static LoadFlowParameters.ComponentMode getComponentMode(final LoadFlowParameters parameters) {
        return Optional.ofNullable(parameters.getComponentMode()).orElse(MAIN_CONNECTED);
    }

    public static double getBorderFlow(final DanglingLine danglingLine,
                                       final LoadFlowParameters.ComponentMode componentMode) {
        return switch (componentMode) {
            case MAIN_CONNECTED -> isInMainConnectedComponent(danglingLine) ? getLeavingFlow(danglingLine) : 0.;
            case ALL_CONNECTED -> getLeavingFlow(danglingLine);
            default -> throw new CeMergingException(COMPONENT_NUMBER_ERROR);
        };
    }
}
