/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.post_process.merging_supervisor;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.util.JaxbUtils;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.xsd.merging_logs.MergingLog;
import com.farao_community.farao.ce_merging.xsd.merging_supervisor.bci_nf.ExportBCI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MergingLogsConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger(MergingLogsConverter.class);

    private MergingLogsConverter() {
        //utility
    }

    public static byte[] convert(final MergingTask task) {
        try {
            final MergingLog mergingLogs = JaxbUtils.readFromPath(MergingLog.class, task.getOutputs().getMergingLogs().getPath());
            final ExportBCI exportBci = new BciExportBuilder(task, mergingLogs).buildBciExport();
            return JaxbUtils.writeToBytes(ExportBCI.class, exportBci);
        } catch (final Exception e) {
            LOGGER.error("Cannot convert merging logs file for task '{}'", task.getId(), e);
            throw new CeMergingException(String.format("Cannot convert merging logs file for task %s ", task.getId()), e);
        }
    }
}
