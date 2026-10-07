/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.dk_renaming;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Switch;
import com.powsybl.ucte.converter.NamingStrategy;
import com.powsybl.ucte.converter.UcteException;
import com.powsybl.ucte.network.UcteElementId;
import com.powsybl.ucte.network.UcteNodeCode;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.COMMA;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.DK_HVDC_XNODES_PROPERTY;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.DK_NAMING_STRATEGY;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.ONE;
import static com.powsybl.ucte.network.UcteCountryCode.DE;
import static com.powsybl.ucte.network.UcteCountryCode.DK;
import static com.powsybl.ucte.network.UcteCountryCode.XX;

public class DKNamingStrategy implements NamingStrategy {
    private final Map<String, UcteNodeCode> ucteNodeIds = new HashMap<>();
    private final Map<String, UcteElementId> ucteElementIds = new HashMap<>();
    private List<String> dkHvdcXnodes = Collections.emptyList();

    @Override
    public void initializeNetwork(final Network network) {
        dkHvdcXnodes = Collections.emptyList();
        String raw = network.getProperty(DK_HVDC_XNODES_PROPERTY);
        if (StringUtils.isNotBlank(raw)) {
            dkHvdcXnodes = Arrays.asList(raw.split(COMMA));
        }
    }

    @Override
    public String getName() {
        return DK_NAMING_STRATEGY;
    }

    @Override
    public UcteNodeCode getUcteNodeCode(final String id) {
        return ucteNodeIds.computeIfAbsent(id, k -> UcteNodeCode.parseUcteNodeCode(k).map(this::convertIfDk).orElseThrow(() -> new UcteException("Invalid UCTE node identifier: " + k)));
    }

    @Override
    public UcteNodeCode getUcteNodeCode(final Bus bus) {
        return getUcteNodeCode(bus.getId());
    }

    @Override
    public UcteNodeCode getUcteNodeCode(final DanglingLine danglingLine) {
        return getUcteNodeCode(danglingLine.getPairingKey());
    }

    @Override
    public UcteElementId getUcteElementId(final String id) {
        return ucteElementIds.computeIfAbsent(id, k -> UcteElementId.parseUcteElementId(k).map(this::convertIfDk).orElseThrow(() -> new UcteException("Invalid UCTE element identifier: " + k)));
    }

    @Override
    public UcteElementId getUcteElementId(final Switch sw) {
        return getUcteElementId(sw.getId());
    }

    @Override
    public UcteElementId getUcteElementId(final Branch branch) {
        return getUcteElementId(branch.getId());
    }

    @Override
    public UcteElementId getUcteElementId(final DanglingLine danglingLine) {
        return getUcteElementId(danglingLine.getId());
    }

    private UcteNodeCode convertIfDk(final UcteNodeCode ucteNodeCode) {
        if (ucteNodeCode.getUcteCountryCode() == DE) {
            final String spot = ucteNodeCode.getGeographicalSpot();
            if (spot.startsWith(ONE)) {
                ucteNodeCode.setUcteCountryCode(DK);
            } else if (dkHvdcXnodes.contains(spot)) {
                ucteNodeCode.setUcteCountryCode(XX);
            }
        }

        return ucteNodeCode;
    }

    private UcteElementId convertIfDk(final UcteElementId ucteElementId) {
        convertIfDk(ucteElementId.getNodeCode1());
        convertIfDk(ucteElementId.getNodeCode2());
        return ucteElementId;
    }
}
