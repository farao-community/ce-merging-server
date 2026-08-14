/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.final_result;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.model.netpositions.NetPositionsResults;
import com.farao_community.farao.ce_merging.merging.process.netpositions.CountryNetPositionHandler;
import com.farao_community.farao.ce_merging.merging.process.xnode.XnodesCalculation;
import com.farao_community.farao.ce_merging.merging.process.xnode.XnodesCheck;
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

import java.util.function.Supplier;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.REPORT_BASE_NAME;
import static com.farao_community.farao.ce_merging.common.util.FileStorageUtils.saveArtifactFile;
import static com.farao_community.farao.ce_merging.common.util.LoadFlowUtils.getLoadFlowMode;
import static com.farao_community.farao.ce_merging.common.util.LoadFlowUtils.runLoadFlowWithLogs;
import static com.farao_community.farao.ce_merging.merging.process.netpositions.CountryNetPositionHandler.initHandler;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.CGM_NET_POSITIONS_FILE;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.LOAD_FLOW_ON_FINAL_CGM_LOGS;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.XNODES_INFORMATION_FILE;

@Service
public class FinalResultService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FinalResultService.class);
    private final CeMergingConfiguration configuration;
    private final Supplier<LoadFlow.Runner> loadFlowRunnerSupplier;
    private final XnodesCalculation xnodesCalculation;

    public FinalResultService(CeMergingConfiguration configuration,
                              Supplier<LoadFlow.Runner> loadFlowRunnerSupplier,
                              XnodesCalculation xnodesCalculation) {
        this.configuration = configuration;
        this.loadFlowRunnerSupplier = loadFlowRunnerSupplier;
        this.xnodesCalculation = xnodesCalculation;
    }

    public void computeCgmResults(MergingTask task) {
        try {
            final LoadFlowParameters loadFlowParameters = task.getConfigurations().getLoadFlowParameters();
            final NetPositionsResults netPositionsFile = new NetPositionsResults();

            final SavedFile cgm = task.getOutputs().getCgm();
            LOGGER.info("IIDM import of CGM network : {}", cgm.getOriginalName());
            final Network network = Network.read(cgm.getPath());

            final ReportNode rootReportNode = ReportNode.newRootReportNode()
                    .withMessageTemplate("loadflow.logs")
                    .withResourceBundles(REPORT_BASE_NAME, PowsyblOpenLoadFlowReportResourceBundle.BASE_NAME)
                    .build();

            final LoadFlowResult result = runLoadFlowWithLogs(network, loadFlowRunnerSupplier, loadFlowParameters, rootReportNode);

            final LoadFlowOutput loadflowOutput = LoadFlowOutput.from(cgm.getOriginalName(),
                                                                      getLoadFlowMode(loadFlowParameters),
                                                                      result);

            saveArtifactFile(LOAD_FLOW_ON_FINAL_CGM_LOGS,
                             OpenLoadFlowReportToXmlConverter.convert(rootReportNode),
                             task,
                             configuration);

            network.getCountries()
                    .stream()
                    .map(country -> initHandler(country, network, task.getConfigurations()))
                    .forEach(CountryNetPositionHandler::computeNetPositions);

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
            final XnodesCheck xnodesCheck = task.getArtifact(XNODES_INFORMATION_FILE, XnodesCheck.class);
            if (xnodesCheck != null && !xnodesCheck.getXnodeInformationMap().isEmpty()) {
                XnodesCheck xnodesCheckUpdated = new XnodesCheck(
                        xnodesCalculation.completeXnodeMergedInformation(network, xnodesCheck.getXnodeInformationMap())
                );
                saveArtifactFile(XNODES_INFORMATION_FILE, xnodesCheckUpdated, task, configuration);
            }
        } catch (final Exception e) {
            LOGGER.warn("Cannot add merged information from final CGM to the xnodesInformation.json file : {}", e.getMessage());
        }
    }
}
