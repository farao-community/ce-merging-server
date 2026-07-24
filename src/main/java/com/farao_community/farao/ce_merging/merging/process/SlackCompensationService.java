/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process;

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
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Supplier;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.FILENAME_DATETIME_FMT;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.PARIS_ZONE_ID;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.UCTE_FORMAT;
import static com.farao_community.farao.ce_merging.common.util.LoadFlowUtils.getComponentModeLfParameter;
import static com.farao_community.farao.ce_merging.common.util.LoadFlowUtils.isConnected;
import static com.farao_community.farao.ce_merging.common.util.LoadFlowUtils.runLoadflow;
import static com.farao_community.farao.ce_merging.common.util.NetworkUtil.zeroIfNaN;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.CGM_FILE_AFTER_PST;
import static com.powsybl.ucte.network.UcteNodeTypeCode.UT;
import static java.util.Locale.FRANCE;

@Service
public class SlackCompensationService {

    private CeMergingConfiguration configuration;
    private final Supplier<LoadFlow.Runner> loadFlowRunnerSupplier;
    private static final Logger LOGGER = LoggerFactory.getLogger(SlackCompensationService.class);

    public SlackCompensationService(final Supplier<LoadFlow.Runner> loadFlowRunnerSupplier,
                                    final CeMergingConfiguration configuration) {
        this.loadFlowRunnerSupplier = loadFlowRunnerSupplier;
        this.configuration = configuration;
    }

    public void compensateFinalCgmSlackImbalance(final MergingTask task) {
        final Network compensatedNetwork = compensateNetwork(task);
        addSlackNode(compensatedNetwork, task);
        saveCgmInOutputs(compensatedNetwork, task);
    }

    public Network compensateNetwork(final MergingTask task) {
        //This post-processing consists in saving the values calculated by the loadflow during the compensation in the cgm
        //Warning: This is compatible with the current behavior of the loadflow which does not compensate on xnodes
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

    private static void compensateLoad(final Load load) {
        load.setP0(load.getTerminal().getP());
        load.setQ0(zeroIfNaN(load.getTerminal().getQ()));
    }

    private static void compensateGenerator(final Generator generator) {
        generator.setTargetP(-generator.getTerminal().getP());
        generator.setTargetQ(-zeroIfNaN(generator.getTerminal().getQ()));
    }

    void addSlackNode(final Network cgm, final MergingTask task) {
        /*
            setWriteSlackBus van't be used before correction of bilanPV=false on PowSyBl (ADNHelper class)
            Otherwise the slack node of the loadflow "TKEBAN1" is not of type UT (3)
        */

        final String defaultSlackNode = task.getConfigurations().getDefaultSlackNode();
        UcteNetwork spain = null;
        try (final FileInputStream fis = new FileInputStream(task.getInputs().getIgm("ES").getIgmFile().getPath());
             final InputStreamReader isr = new InputStreamReader(fis);
             final BufferedReader spanishIgm = new BufferedReader(isr)) {
            spain = new UcteReader().read(spanishIgm, new ReportNodeNoOp());
        } catch (IOException e) {
            LOGGER.warn("Error while reading slack node in ES IGM, default slack node '{}' will be added to the final CGM", defaultSlackNode);
        }

        final String slackNode = Optional.ofNullable(spain)
            .map(UcteNetwork::getNodes)
            .stream().flatMap(Collection::stream)
            .filter(ucteNode -> ucteNode.getTypeCode() == UT)
            .findFirst()
            .map(UcteNode::getCode).map(UcteNodeCode::toString)
            .orElse(getWithWarning(defaultSlackNode, "No slack node defined in ES IGM, default slack node %s will be added to the final CGM".formatted(defaultSlackNode)));

        updateSlackBus(cgm, slackNode);

    }

    final String getWithWarning(final String toGet, final String warning) {
        LOGGER.warn(warning);
        return toGet;
    }

    private void updateSlackBus(final Network cgm, final String busId) {
        SlackTerminal.reset(cgm);

        LOGGER.info("Adding slack node of ES IGM '{}' to the final CGM", busId);
        final String notFoundWarning = "Cannot add slack node to the final CGM : node %s absent from CGM ".formatted(busId);

        Optional.ofNullable(cgm.getBusBreakerView().getBus(busId))
            .ifPresentOrElse(SlackTerminal::attach,
                             () -> LOGGER.warn(notFoundWarning));
    }

    private void saveCgmInOutputs(final Network network, MergingTask task) {

        final ZonedDateTime targetZdtParis = task.getTargetDate().atZoneSameInstant(PARIS_ZONE_ID);
        final String dateAndTime = FILENAME_DATETIME_FMT.withLocale(FRANCE).format(targetZdtParis);

        /* !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            Never change this file name without CORESO's agreement because it's interfaced with CCCTool
           !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! */
        final String fileName = String.format("%s_2D%s_UX0.uct", dateAndTime, targetZdtParis.getDayOfWeek().getValue());

        SavedFile savedFile = FileStorageUtils.save(configuration.getOutputsDirectoryPath(task),
                                                    fileName,
                                                    String.format("/tasks/%d/outputs/cgm", task.getId()),
                                                    path -> network.write(UCTE_FORMAT, null, path));

        task.getOutputs().setCgm(savedFile);
    }
}
