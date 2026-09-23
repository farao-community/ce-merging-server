/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.daily_merging.merging_report.sheets;

import com.farao_community.farao.ce_merging.merging.process.xnode.inconsistencies.XnodeIncorrect;

import java.util.ArrayList;
import java.util.List;

public record XNodeInconsistenciesSheet(String bd, String timestamp, String xNode, String tso1, String status1,
                                        String tso2, String status2, String finalStatus,
                                        String correctionsApplied) implements ColumnsHeader {

    @Override
    public List<String> getFieldNames() {
        List<String> fieldNames = new ArrayList<>();
        fieldNames.add("BD");
        fieldNames.add("TS");
        fieldNames.add("Xnode");
        fieldNames.add("TSO 1");
        fieldNames.add("Status 1");
        fieldNames.add("TSO 2");
        fieldNames.add("Status 2");
        fieldNames.add("Final Status");
        fieldNames.add("Correction applied?");

        return fieldNames;
    }

    public static XNodeInconsistenciesSheet fromXnodeIncorrect(final String businessDay,
                                                               final String instant,
                                                               final XnodeIncorrect xnodeIncorrect) {
        return new XNodeInconsistenciesSheet(businessDay,
                                             instant,
                                             xnodeIncorrect.getName(),
                                             xnodeIncorrect.getCountry1(),
                                             xnodeIncorrect.getStatus1().toString(),
                                             xnodeIncorrect.getCountry2(),
                                             xnodeIncorrect.getStatus2().toString(),
                                             xnodeIncorrect.getFinalStatus().toString(),
                                             "auto");
    }
}
