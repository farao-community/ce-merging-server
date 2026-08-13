/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.slack_compensation;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.util.NetworkUtil;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.powsybl.commons.report.ReportNodeNoOp;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.SlackTerminal;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.ucte.network.UcteNetwork;
import com.powsybl.ucte.network.UcteNode;
import com.powsybl.ucte.network.UcteNodeCode;
import com.powsybl.ucte.network.io.UcteReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.function.Supplier;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.UCTE_FORMAT;
import static com.farao_community.farao.ce_merging.common.util.FileStorageUtils.save;
import static com.farao_community.farao.ce_merging.common.util.LoadFlowUtils.getComponentModeLfParameter;
import static com.farao_community.farao.ce_merging.common.util.LoadFlowUtils.isConnected;
import static com.farao_community.farao.ce_merging.common.util.LoadFlowUtils.runLoadflow;
import static com.farao_community.farao.ce_merging.common.util.NetworkUtil.zeroIfNaN;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.CGM_FILE_AFTER_PST;
import static com.powsybl.iidm.network.Country.ES;
import static com.powsybl.ucte.network.UcteNodeTypeCode.UT;

@Service
public class SlackCompensationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SlackCompensationService.class);
    private final Supplier<LoadFlow.Runner> loadFlowRunnerSupplier;
    private final CeMergingConfiguration configuration;

    public SlackCompensationService(final Supplier<LoadFlow.Runner> loadFlowRunnerSupplier,
                                    final CeMergingConfiguration configuration) {
        this.loadFlowRunnerSupplier = loadFlowRunnerSupplier;
        this.configuration = configuration;
    }

    private static void compensateLoad(final Load load) {
        load.setP0(load.getTerminal().getP());
        load.setQ0(zeroIfNaN(load.getTerminal().getQ()));
    }

    private static void compensateGenerator(final Generator generator) {
        generator.setTargetP(-generator.getTerminal().getP());
        generator.setTargetQ(-zeroIfNaN(generator.getTerminal().getQ()));
    }

    public void compensateFinalCgmSlackImbalance(final MergingTask task) {
        final Network compensatedNetwork = compensateNetwork(task);
        addSlackNode(compensatedNetwork, task);

        final SavedFile cgmFile = save(configuration.getOutputsDirectoryPath(task),
                                       task.getOutputCgmFileName(),
                                       String.format("/tasks/%d/outputs/cgm", task.getId()),
                                       path -> compensatedNetwork.write(UCTE_FORMAT, null, path));

        task.getOutputs().setCgm(cgmFile);
    }

    private Network compensateNetwork(final MergingTask task) {
        final SavedFile cgmFileToCompensate = task.getArtifacts().getFile(CGM_FILE_AFTER_PST);
        final Network cgm = Network.read(cgmFileToCompensate.getPath());
        final LoadFlowParameters loadFlowParameters = task.getConfigurations().getLoadFlowParameters();

        runLoadflow(cgm, loadFlowRunnerSupplier, loadFlowParameters);

        final LoadFlowParameters.ComponentMode componentMode = getComponentModeLfParameter(loadFlowParameters);

        cgm.getLoadStream()
                .filter(isConnected(componentMode).and(NetworkUtil::hasActivePower))
                .forEach(SlackCompensationService::compensateLoad);

        cgm.getGeneratorStream()
                .filter(isConnected(componentMode).and(NetworkUtil::hasActivePower))
                .forEach(SlackCompensationService::compensateGenerator);

        return cgm;
    }

    void addSlackNode(final Network cgm,
                      final MergingTask task) {
        /*
            setWriteSlackBus can't be used before correction of bilanPV=false on PowSyBl (ADNHelper class)
            Otherwise the slack node of the loadflow "TKEBAN1" is not of type UT (3)
        */

        final String defaultSlackNode = task.getConfigurations().getDefaultSlackNode();

        final UcteNetwork spanishNetwork = getSpanishNetwork(task);

        final Optional<String> slackNode = spanishNetwork == null ? Optional.empty() :
                spanishNetwork.getNodes()
                        .stream()
                        .filter(ucteNode -> ucteNode.getTypeCode() == UT)
                        .map(UcteNode::getCode)
                        .map(UcteNodeCode::toString)
                        .findFirst();

        if (slackNode.isPresent()) {
            updateSlackBus(cgm, slackNode.get());
        } else {
            updateSlackBus(cgm, defaultSlackNode);
            LOGGER.warn("No slack node defined in ES IGM, default slack node %s will be added to the final CGM"
                                .formatted(defaultSlackNode));
        }

    }

    private UcteNetwork getSpanishNetwork(final MergingTask task) {
        final String defaultSlackNode = task.getConfigurations().getDefaultSlackNode();
        try (final FileInputStream fis = new FileInputStream(task.getInputs().getIgm(ES).getIgmFile().getPath());
             final InputStreamReader isr = new InputStreamReader(fis);
             final BufferedReader spanishIgm = new BufferedReader(isr)) {
            return new UcteReader().read(spanishIgm, new ReportNodeNoOp());
        } catch (final IOException e) {
            LOGGER.warn("Error while reading slack node in ES IGM, default slack node '{}' will be added to the final CGM", defaultSlackNode);
            return null;
        }
    }

    private void updateSlackBus(final Network cgm,
                                final String busId) {
        SlackTerminal.reset(cgm);

        LOGGER.info("Adding slack node of Spanish IGM '{}' to the final CGM", busId);
        final String notFoundWarning = "Cannot add slack node to the final CGM : node %s absent from CGM ".formatted(busId);

        Optional.ofNullable(cgm.getBusBreakerView().getBus(busId))
                .ifPresentOrElse(SlackTerminal::attach,
                                 () -> LOGGER.warn(notFoundWarning));
    }

}
