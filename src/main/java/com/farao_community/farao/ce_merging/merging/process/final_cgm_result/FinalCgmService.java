/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.final_cgm_result;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.model.netpositions.NetPositionsResults;
import com.farao_community.farao.ce_merging.merging.process.xnode.XnodesCalculation;
import com.farao_community.farao.ce_merging.merging.process.xnode.XnodesCheck;
import com.farao_community.farao.ce_merging.merging.task.entities.Configurations;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.openloadflow.util.report.PowsyblOpenLoadFlowReportResourceBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.REPORT_BASE_NAME;
import static com.farao_community.farao.ce_merging.common.util.FileStorageUtils.saveArtifactFile;
import static com.farao_community.farao.ce_merging.common.util.LoadFlowUtils.getLoadFlowMode;
import static com.farao_community.farao.ce_merging.common.util.LoadFlowUtils.runLoadFlowWithLogs;
import static com.farao_community.farao.ce_merging.merging.process.final_cgm_result.OpenLoadFlowReportToXmlConverter.fromOlfReportToXmlLogs;
import static com.farao_community.farao.ce_merging.merging.process.netpositions.CountryNetPositionHandler.computeCountryNetPositions;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.CGM_NET_POSITIONS_FILE;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.LOAD_FLOW_ON_FINAL_CGM_LOGS;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.XNODES_INFORMATION_FILE;
import static java.util.function.Predicate.not;

@Service
public class FinalCgmService {

    private static final String LOADFLOW_LOGS = "loadflow.logs";
    private static final Logger LOGGER = LoggerFactory.getLogger(FinalCgmService.class);
    private final CeMergingConfiguration configuration;
    private final Supplier<LoadFlow.Runner> runnerSupplier;
    private final XnodesCalculation xnodesCalculation;

    public FinalCgmService(final CeMergingConfiguration configuration,
                           final Supplier<LoadFlow.Runner> runnerSupplier,
                           final XnodesCalculation xnodesCalculation) {
        this.configuration = configuration;
        this.runnerSupplier = runnerSupplier;
        this.xnodesCalculation = xnodesCalculation;
    }

    public void computeFinalCgmResult(final MergingTask task) {
        try {
            final Configurations taskConfiguration = task.getConfigurations();
            final LoadFlowParameters loadFlowParameters = taskConfiguration.getLoadFlowParameters();
            final NetPositionsResults netPositionsFile = new NetPositionsResults();

            final SavedFile cgm = task.getOutputs().getCgm();
            final Network network = Network.read(cgm.getPath());

            final ReportNode rootReportNode = ReportNode.newRootReportNode()
                    .withMessageTemplate(LOADFLOW_LOGS)
                    .withResourceBundles(REPORT_BASE_NAME, PowsyblOpenLoadFlowReportResourceBundle.BASE_NAME)
                    .build();

            final LoadFlowResult result = runLoadFlowWithLogs(network, runnerSupplier, loadFlowParameters, rootReportNode);

            final LoadFlowOutput loadflowOutput = LoadFlowOutput.from(cgm.getOriginalName(),
                                                                      getLoadFlowMode(loadFlowParameters),
                                                                      result);

            saveArtifactFile(LOAD_FLOW_ON_FINAL_CGM_LOGS, fromOlfReportToXmlLogs(rootReportNode), task, configuration);

            network.getCountries().forEach(country -> computeCountryNetPositions(country, network, taskConfiguration));

            final FinalCgmResult cgmResult = new FinalCgmResult(loadflowOutput, netPositionsFile);

            saveArtifactFile(CGM_NET_POSITIONS_FILE, cgmResult, task, configuration);
            updateXnodesInformations(network, task);
        } catch (final Exception e) {
            final String errorMessage = String.format("CGM result calculation failed for task %d with target date %s",
                                                      task.getId(), task.getTargetDate());
            LOGGER.error(errorMessage);
            throw new CeMergingException(errorMessage, e);
        }
    }

    private void updateXnodesInformations(final Network network,
                                          final MergingTask task) {
        try {
            Optional.ofNullable(task.getArtifact(XNODES_INFORMATION_FILE, XnodesCheck.class))
                    .map(XnodesCheck::getXnodeInformationMap)
                    .filter(not(Map::isEmpty))
                    .map(infos -> xnodesCalculation.completeXnodeMergedInformation(network, infos))
                    .map(XnodesCheck::new)
                    .ifPresent(updated -> saveArtifactFile(XNODES_INFORMATION_FILE, updated, task, configuration));

        } catch (final Exception e) {
            LOGGER.warn("Cannot add merged information from final CGM to the xnodesInformation.json file : {}", e.getMessage());
        }
    }
}
