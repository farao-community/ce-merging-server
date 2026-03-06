/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 */
package com.farao_community.farao.ce_merging.merging.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * WARNING: this class is used by the merging supervisor. Please contact them if any modification is needed to check compatibility
 */
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
