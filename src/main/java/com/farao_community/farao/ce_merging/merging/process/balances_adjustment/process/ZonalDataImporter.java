/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.balances_adjustment.process;

import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VoltageLevel;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Map;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.MAX_FICTITIOUS_P;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.MAX_FICTITIOUS_Q;
import static com.powsybl.glsk.api.io.GlskDocumentImporters.importGlsk;
import static com.powsybl.iidm.network.LoadType.FICTITIOUS;

public final class ZonalDataImporter {
    private ZonalDataImporter() {
        /* This utility class should not be instantiated */
    }

    public static Map<String, Scalable> getZonalDataFromGlsk(final File glskFile,
                                                             final Network network,
                                                             final OffsetDateTime targetDateTime) throws IOException {
        try (final FileInputStream fis = new FileInputStream(glskFile)) {
            return getZonalDataFromGlsk(fis, network, targetDateTime);
        }
    }

    public static Map<String, Scalable> getZonalDataFromGlsk(final InputStream glskInputStream,
                                                             final Network network,
                                                             final OffsetDateTime targetDateTime) {
        createAllMissingElements(network);

        return importGlsk(glskInputStream)
            .getZonalScalable(network, targetDateTime.toInstant())
            .getDataPerZone();
    }

    private static void createAllMissingElements(final Network network) {
        network.getVoltageLevelStream()
            .map(VoltageLevel::getBusBreakerView)
            .flatMap(VoltageLevel.BusBreakerView::getBusStream)
            .forEach(bus -> createBusMissingElements(bus, network));
    }

    private static void createBusMissingElements(final Bus bus, final Network network) {
        final VoltageLevel voltageLevel = bus.getVoltageLevel();
        final String busId = bus.getId();
        createMissingGenerator(network, voltageLevel, busId);
        createMissingLoad(network, voltageLevel, busId);
    }

    private static void createMissingGenerator(final Network network, final VoltageLevel voltageLevel, final String busId) {
        final String generatorId = busId + "_generator";
        if (network.getGenerator(generatorId) == null) {
            voltageLevel.newGenerator()
                .setBus(busId)
                .setEnsureIdUnicity(true)
                .setId(generatorId)
                .setMaxP(MAX_FICTITIOUS_P)
                .setMinP(0)
                .setTargetP(0)
                .setTargetQ(0)
                .setTargetV(voltageLevel.getNominalV())
                .setVoltageRegulatorOn(false)
                .setFictitious(true)
                .add()
                .newMinMaxReactiveLimits()
                .setMaxQ(MAX_FICTITIOUS_Q)
                .setMinQ(MAX_FICTITIOUS_Q)
                .add();
        }
    }

    private static void createMissingLoad(final Network network, final VoltageLevel voltageLevel, final String busId) {
        String loadId = busId + "_load";
        if (network.getLoad(loadId) == null) {
            voltageLevel.newLoad()
                .setBus(busId)
                .setEnsureIdUnicity(true)
                .setId(loadId)
                .setP0(0)
                .setQ0(0)
                .setLoadType(FICTITIOUS)
                .setFictitious(true)
                .add();
        }
    }
}
