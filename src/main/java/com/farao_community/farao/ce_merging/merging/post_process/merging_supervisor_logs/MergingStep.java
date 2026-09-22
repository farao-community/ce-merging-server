/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.post_process.merging_supervisor_logs;

public enum MergingStep {
    CONFIGURATIONS(0),
    INITIAL_IMPORT(1),
    XNODES_IGM_CHECK(2),
    FORECAST_NET_POSITION_IMPORT(3),
    GERMAN_PREMERGE(4),
    DK_RENAMING(5),
    INITIAL_NET_POSION_CALCULATION(6),
    TOPOLOGICAL_MERGE(7),
    RECESSIVITY_APPLICATION(8),
    ALEGRO(9),
    TGM_NET_POSION_CALCULATION(10),
    GLSK_QUALITY_CHECK(11),
    BASE_CASE_IMPROVEMENT(12),
    TARGET_NET_POSION_CALCULATION(13),
    BALANCES_ADJUSTMENT(14),
    PST_SPECIAL_PROCEDURE(15),
    SLACK_COMPENSATION(16),
    FINAL_RESULTS_CALCULATION(17),
    OPEN_LOAD_FLOW_LOGS(18),
    REF_PROG(19),
    MERGING_REPORTS(20),
    RESULTS_EXPORT(21);

    private final int order;

    MergingStep(int order) {
        this.order = order;
    }

    public int getOrder() {
        return order;
    }
}

