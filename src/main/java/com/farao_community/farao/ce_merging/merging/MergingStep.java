/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging;

public enum MergingStep {
    // listed in processing order
    CONFIGURATIONS,
    INITIAL_IMPORT,
    XNODES_IGM_CHECK,
    FORECAST_NET_POSITION_IMPORT,
    GERMAN_PREMERGE,
    DK_RENAMING,
    INITIAL_NET_POSION_CALCULATION,
    TOPOLOGICAL_MERGE,
    RECESSIVITY_APPLICATION,
    ALEGRO,
    TGM_NET_POSION_CALCULATION,
    GLSK_QUALITY_CHECK,
    BASE_CASE_IMPROVEMENT,
    TARGET_NET_POSION_CALCULATION,
    BALANCES_ADJUSTMENT,
    PST_SPECIAL_PROCEDURE,
    SLACK_COMPENSATION,
    INTERNAL_HVDC_ADJUSTMENT,
    FINAL_RESULTS_CALCULATION,
    OPEN_LOAD_FLOW_LOGS,
    REF_PROG,
    MERGING_REPORTS,
    RESULTS_EXPORT
}

