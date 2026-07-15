/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.pst_special_process;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.PhaseTapChanger;
import com.powsybl.iidm.network.TieLine;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.farao_community.farao.ce_merging.common.util.NetworkUtil.isIdentifiedBy;
import static java.lang.Double.isNaN;

public final class PstUtils {

    private PstUtils() {
        throw new AssertionError("Utility class should not be constructed");
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(PstUtils.class);

    public static void regulatePst(final TwoWindingsTransformer pst, final double targetFlow) {
        applyIfHasTap(pst, phaseTapChanger -> {
            phaseTapChanger.setRegulationValue(targetFlow);
            phaseTapChanger.setRegulationTerminal(pst.getTerminal1());
            phaseTapChanger.setRegulationMode(PhaseTapChanger.RegulationMode.ACTIVE_POWER_CONTROL);
            phaseTapChanger.setTargetDeadband(0.0);
            phaseTapChanger.setRegulating(true);
        });
    }

    public static void setPstRegulating(final TwoWindingsTransformer pst, final boolean regulating) {
        applyIfHasTap(pst, phaseTapChanger -> phaseTapChanger.setRegulating(regulating));
    }

    public static void applyIfHasTap(final TwoWindingsTransformer pst, final Consumer<PhaseTapChanger> action) {
        Optional.ofNullable(pst.getPhaseTapChanger()).ifPresentOrElse(
            action,
            () -> LOGGER.error("Error during PST special process: phase tap changer of {} not found in network", pst.getId())
        );
    }

    public static double getPstTarget(final TwoWindingsTransformer pst) {
        return pst.getPhaseTapChanger().getRegulationValue();
    }

    public static boolean inconsistentTargetFlows(final TwoWindingsTransformer pst1, final TwoWindingsTransformer pst2) {
        if (pst1.getPhaseTapChanger() == null || pst2.getPhaseTapChanger() == null) {
            return false;
        }
        return pst1.getPhaseTapChanger().getRegulationValue() != pst2.getPhaseTapChanger().getRegulationValue();
    }

    public static boolean hasTargetFlow(final TwoWindingsTransformer pst) {
        return pst != null && pst.getPhaseTapChanger() != null && !isNaN(pst.getPhaseTapChanger().getRegulationValue());
    }

    public static Branch getPstBranch(final SpecialPst pst, final Network network) {
        return getPstIdentifiable(pst.getIdRegex(), network.getBranchStream());
    }

    public static TieLine getPstTieLine(final String idRegex, final Network network) {
        return getPstIdentifiable(idRegex, network.getTieLineStream());
    }

    public static <T extends Identifiable<?>> T getPstIdentifiable(final String idRegex, final Stream<T> stream) {
        final T identifiable = stream.filter(isIdentifiedBy(idRegex)).findFirst().orElse(null);

        if (identifiable == null) {
            LOGGER.warn("Cannot find element matching the regex: '{}', default flow value 0 will be used",
                        idRegex);
        }

        return identifiable;
    }

    public static double getBoundaryP(final String idRegex, final Network igm) {
        final DanglingLine danglingLine = getPstIdentifiable(idRegex, igm.getDanglingLineStream());
        if (danglingLine == null) {
            return 0;
        } else if (danglingLine.getGeneration() == null) {
            return danglingLine.getP0();
        } else {
            return danglingLine.getP0() - danglingLine.getGeneration().getTargetP();
        }
    }



}
