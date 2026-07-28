/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.monita;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Switch;
import com.powsybl.ucte.converter.NamingStrategy;
import com.powsybl.ucte.converter.UcteException;
import com.powsybl.ucte.network.UcteElementId;
import com.powsybl.ucte.network.UcteNodeCode;

import java.util.HashMap;
import java.util.Map;

import static com.powsybl.ucte.network.UcteCountryCode.XX;
import static com.powsybl.ucte.network.UcteElementId.parseUcteElementId;
import static com.powsybl.ucte.network.UcteNodeCode.parseUcteNodeCode;

public class MonitaNamingStrategy implements NamingStrategy {
    private final Map<String, UcteNodeCode> ucteNodeIds = new HashMap<>();
    private final Map<String, UcteElementId> ucteElementIds = new HashMap<>();
    private static final String MONITA_NODE_NAME_REGEX = "I(KOT|CEP)R[12]2[0-9A-Z]";

    @Override
    public void initializeNetwork(Network network) {
        //Empty implementation by default
    }

    @Override
    public String getName() {
        return "MonitaNamingStrategy";
    }

    @Override
    public UcteNodeCode getUcteNodeCode(final String id) {
        return ucteNodeIds.computeIfAbsent(id, code -> parseUcteNodeCode(code).map(this::convertMonitaNode)
            .orElseThrow(() -> new UcteException("Invalid UCTE node identifier: " + code)));
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
        return ucteElementIds.computeIfAbsent(id, code -> parseUcteElementId(code).map(this::convertMonitaBranch)
            .orElseThrow(() -> new UcteException("Invalid UCTE element identifier: " + code)));
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

    private UcteNodeCode convertMonitaNode(final UcteNodeCode ucteNodeCode) {
        if (ucteNodeCode.toString().matches(MONITA_NODE_NAME_REGEX)) {
            ucteNodeCode.setUcteCountryCode(XX);
            ucteNodeCode.setBusbar('0');
        }
        return ucteNodeCode;
    }

    private UcteElementId convertMonitaBranch(final UcteElementId ucteElementId) {
        convertMonitaNode(ucteElementId.getNodeCode1());
        convertMonitaNode(ucteElementId.getNodeCode2());
        return ucteElementId;
    }
}
