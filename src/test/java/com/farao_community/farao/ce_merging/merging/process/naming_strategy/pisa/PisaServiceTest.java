/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.ce_merging.merging.process.naming_strategy.pisa;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.merging.task.entities.Inputs;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import test_utils.assertions.NetworkAssert;

import java.io.File;
import java.io.FileNotFoundException;

import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.CGM_FILE_AFTER_COMPENSATION;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static test_utils.CeTestUtils.createMockTask;
import static test_utils.CeTestUtils.stringPathOf;
import static test_utils.CeTestUtils.withArtifact;
import static test_utils.assertions.CeThrowableAssert.assertThatThrownBy;

class PisaServiceTest {

    private final CeMergingConfiguration configuration = mock(CeMergingConfiguration.class);

    @TempDir
    private File tempDirectory;

    @Test
    void shouldDisplaceNodes() throws FileNotFoundException {
        final MergingTask task = getPisaTask("network_ok.uct");
        when(configuration.getOutputsDirectoryPath(any(MergingTask.class))).thenReturn(tempDirectory.getPath());
        final PisaService service = new PisaService(configuration);
        service.displacePisaNodes(task);

        final Network cgm = Network.read(task.getOutputs().getCgm().getPath());

        NetworkAssert.assertThat(cgm)
                .hasBoundaryLine("XFG.IL11 F.TEST11 1")
                .hasLine("IPI_GI11 I.TEST11 1")
                .hasBoundaryLine("IPI_GI11 XFG.IL13 1")
                .hasBoundaryLine("XFG.IL12 IPI_GI12 1")
                .hasLine("I.TEST11 IPI_GI12 1");
    }

    @Test
    void shouldThrowExceptionIfInvalidLineConnected() {
        final MergingTask task = getPisaTask("network_ko.uct");
        final PisaService service = new PisaService(configuration);
        assertThatThrownBy(() -> service.displacePisaNodes(task))
                .isValidServiceException()
                .hasMessage("PiSa post-treatment: cannot remove line FFG.IL11 FFG.IL12 1 because it is in operation.");
    }

    private MergingTask getPisaTask(final String networkName) {
        return createMockTask(1L, new Inputs(), withArtifact(CGM_FILE_AFTER_COMPENSATION, networkName, stringPathOf("pisa") + "/"));
    }
}
