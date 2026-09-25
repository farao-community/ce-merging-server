/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.daily_merging.outputs.merging_report.sheets;

import java.util.ArrayList;
import java.util.List;

public record MergeSheet(String bd, String timestamp, String type, int lfIterations, double slackImbalance,
                         String justification, String correctionsApplied) implements ColumnsHeader {

    @Override
    public List<String> getFieldNames() {
        List<String> fieldNames = new ArrayList<>();
        fieldNames.add("BD");
        fieldNames.add("TS");
        fieldNames.add("Type");
        fieldNames.add("LF iterations");
        fieldNames.add("Slack imbalance");
        fieldNames.add("Justification (if AC merge or failure)");
        fieldNames.add("Corrections applied");

        return fieldNames;
    }
}

