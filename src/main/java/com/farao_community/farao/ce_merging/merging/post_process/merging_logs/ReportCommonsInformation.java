/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.post_process.merging_logs;

public record ReportCommonsInformation(
        String countryName,
        double generationIgm,
        double loadIgm,
        double globalBalanceIgm,
        double generationCgm,
        double loadCgm,
        double globalBalanceCgm) {
}
