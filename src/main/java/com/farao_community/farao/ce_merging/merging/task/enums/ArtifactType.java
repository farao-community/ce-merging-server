/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.task.enums;

public enum ArtifactType {
    GERMAN_PRE_MERGED_IGM("%s_2D%d_DE0.uct", "german-pre-merge-result"),
    DK_CONVERTED_FILE("%s_2D%d_DK0.uct", "dk-igm-conversion-result"),
    TOPOLOGICAL_MERGE_FILE("%s_2D%d_UX0_TOPOLOGICAL_MERGED.uct", "topological-merge-result"),
    BALANCED_CGM_FILE("%s_2D%d_UX0_BALANCED.uct", "cgm"),
    CGM_FILE_AFTER_PST("%s_2D%d_UX0_PST_APPLIED.uct", "cgm"),
    GLSK_QUALITY_REPORT("%s_GLSK_QUALITY_CHECK.xml", "glsk-quality-report"),
    GLSK_QUALITY_CORRECTED_FILE("glsk_corrected_%s_2D%d_UX0.xml", "actual-glsk-report-correction"),
    IGMS_NET_POSITIONS_FILE("igmsNetPositions.json", "igms-net-positions"),
    GERMAN_IGMS_NET_POSITIONS_FILE("germanIgmsNetPositions.json", "german-igms-net-positions"),
    BCI_OUTPUT_FILE("bciOutputs.json", "bci-output"),
    BALANCES_ADJUSTMENT_TARGET_FILE("balancesAdjustmentTarget.json", "balances-adjustment-target"),
    CGM_NET_POSITIONS_FILE("cgmNetPositions.json", "cgm-net-positions"),
    TGM_FILE_AFTER_RECESSIVITY("%s_2D%d_UX0_RECESSIVITY_APPLIED.uct", "tgm-recessivity"),
    PST_OUTPUT_FILE("pstOutput.json", "pst-result"),
    LOAD_FLOW_ON_FINAL_CGM_LOGS("Loadflow_final_cgm_logs.xml", "open-loadflow-logs"),
    ALEGRO_NET_POSITIONS("alegroNetPositions.json", "igms-net-positions"),
    XNODES_INFORMATION_FILE("xnodesInformation.json", "xnodes-information"),
    XNODES_INCONSISTENCIES("xnodesInconsistencies.json", "xnodes-inconsistencies"),
    REFERENCE_PROGRAM_FORECAST_FILE("forecastReferenceProgram.json", "netPositionForecast");

    private final String fileName;
    private final String location;

    ArtifactType(final String fileName, final String location) {
        this.fileName = fileName;
        this.location = location;
    }

    public String getLocation() {
        return location;
    }

    public String getFileName() {
        return fileName;
    }
}
