/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.post_process.merging_logs;

public record AlegroReportInformation(
        String countryName,
        Double globalNpTargetInitial,
        Double globalNpTargetFinal,
        double globalBalanceIgm,
        double globalBalanceCgm,
        double generationIgm,
        double loadIgm,
        double generationCgm,
        double loadCgm) {

    public static AlegroReportInformation unavailable(
            final String countryName,
            final Double globalNpTargetInitial) {

        return new AlegroReportInformation(
                countryName,
                globalNpTargetInitial,
                null,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0
        );
    }
}


