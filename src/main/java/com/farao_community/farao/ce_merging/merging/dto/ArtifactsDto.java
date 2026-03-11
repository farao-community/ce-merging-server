/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
@Data
public class ArtifactsDto implements Serializable {
    private String germanPreMergedIgmFileLocation;
    private String dkConvertedFileLocation;
    private String topologicalMergeFileLocation;
    private String glskQualityReportLocation;
    private String igmsNetPositionsFileLocation;
    private String tgmNetPositionsFileLocation;
    private String bciOutputFileLocation;
    private String balancesAdjustmentTargetFileLocation;
    private String cgmNetPositionsFileLocation;
    private String pstOutputFileLocation;
    private String balancedCgmFileLocation;
    private String pstAppliedCgmFileLocation;
    private String executionLogsForMergingSupervisorLocation;
    private String loadflowOnFinalCgmLogsLocation;
    private String alegroNetPositionsLocation;
}
