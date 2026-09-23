/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.daily_merging.merging_report.sheets;

import java.util.ArrayList;
import java.util.List;

public record FilesSheet(String bd, String timestamp, String dacfReplacementStrategy, String igmCH,
                         String igmIT) implements ColumnsHeader {

    @Override
    public List<String> getFieldNames() {
        List<String> fieldNames = new ArrayList<>();
        fieldNames.add("BD");
        fieldNames.add("TS");
        fieldNames.add("DACF Replacement strategy");
        fieldNames.add("IGM-CH");
        fieldNames.add("IGM-IT");

        return fieldNames;
    }
}
