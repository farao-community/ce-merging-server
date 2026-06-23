/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.naming_strategy;

import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Switch;
import com.powsybl.iidm.network.Network;
import com.powsybl.ucte.converter.NamingStrategy;
import com.powsybl.ucte.converter.UcteException;
import com.powsybl.ucte.network.UcteCountryCode;
import com.powsybl.ucte.network.UcteElementId;
import com.powsybl.ucte.network.UcteNodeCode;

import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.List;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.DK_HVDC_XNODES_PROPERTY;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.DK_NAMING_STRATEGY;

public class DKNamingStrategy implements NamingStrategy {
    private final Map<String, UcteNodeCode> ucteNodeIds = new HashMap<>();
    private final Map<String, UcteElementId> ucteElementIds = new HashMap<>();
    private List<String> dkHvdcXnodes = Collections.emptyList();

    @Override
    public void initializeNetwork(Network network) {
        String raw = network.getProperty(DK_HVDC_XNODES_PROPERTY);
        if (raw != null && !raw.isEmpty()) {
            dkHvdcXnodes = Arrays.asList(raw.split(","));
        }
    }

    @Override
    public String getName() {
        return DK_NAMING_STRATEGY;
    }

    @Override
    public UcteNodeCode getUcteNodeCode(String id) {
        return ucteNodeIds.computeIfAbsent(id, k -> UcteNodeCode.parseUcteNodeCode(k).map(this::convertForDk).orElseThrow(() -> new UcteException("Invalid UCTE node identifier: " + k)));
    }

    @Override
    public UcteNodeCode getUcteNodeCode(Bus bus) {
        return getUcteNodeCode(bus.getId());
    }

    @Override
    public UcteNodeCode getUcteNodeCode(DanglingLine danglingLine) {
        return getUcteNodeCode(danglingLine.getPairingKey());
    }

    @Override
    public UcteElementId getUcteElementId(String id) {
        return ucteElementIds.computeIfAbsent(id, k -> UcteElementId.parseUcteElementId(k).map(this::convertForDk).orElseThrow(() -> new UcteException("Invalid UCTE element identifier: " + k)));
    }

    @Override
    public UcteElementId getUcteElementId(Switch sw) {
        return getUcteElementId(sw.getId());
    }

    @Override
    public UcteElementId getUcteElementId(Branch branch) {
        return getUcteElementId(branch.getId());
    }

    @Override
    public UcteElementId getUcteElementId(DanglingLine danglingLine) {
        return getUcteElementId(danglingLine.getId());
    }

    private UcteNodeCode convertForDk(UcteNodeCode ucteNodeCode) {
        if (ucteNodeCode.getUcteCountryCode() == UcteCountryCode.DE && ucteNodeCode.getGeographicalSpot().startsWith("1")) {
            ucteNodeCode.setUcteCountryCode(UcteCountryCode.DK);
        } else if (ucteNodeCode.getUcteCountryCode() == UcteCountryCode.DE && dkHvdcXnodes.contains(ucteNodeCode.getGeographicalSpot())) {
            ucteNodeCode.setUcteCountryCode(UcteCountryCode.XX);
        }
        return ucteNodeCode;
    }

    private UcteElementId convertForDk(UcteElementId ucteElementId) {
        convertForDk(ucteElementId.getNodeCode1());
        convertForDk(ucteElementId.getNodeCode2());
        return ucteElementId;
    }
}
