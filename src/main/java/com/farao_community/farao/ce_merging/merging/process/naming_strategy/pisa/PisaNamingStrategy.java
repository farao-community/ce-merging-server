/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.naming_strategy.pisa;

import com.powsybl.iidm.network.BoundaryLine;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Switch;
import com.powsybl.ucte.converter.NamingStrategy;
import com.powsybl.ucte.converter.UcteException;
import com.powsybl.ucte.network.UcteCountryCode;
import com.powsybl.ucte.network.UcteElementId;
import com.powsybl.ucte.network.UcteNodeCode;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.PISA_NAMING_STRATEGY;
import static com.powsybl.ucte.network.UcteCountryCode.XX;
import static com.powsybl.ucte.network.UcteElementId.parseUcteElementId;
import static com.powsybl.ucte.network.UcteNodeCode.parseUcteNodeCode;

public class PisaNamingStrategy implements NamingStrategy {
    private final Map<String, UcteNodeCode> ucteNodeIds = new HashMap<>();
    private final Map<String, UcteElementId> ucteElementIds = new HashMap<>();
    public static final Pattern FFG_NODE_NAME_PATTERN = Pattern.compile("FFG\\.IL1[1-4]");
    public static final Pattern XPI_NODE_NAME_PATTERN = Pattern.compile("XPI_GI1[1-2]");

    @Override
    public void initializeNetwork(Network network) {
        //Empty implementation by default
    }

    @Override
    public String getName() {
        return PISA_NAMING_STRATEGY;
    }

    @Override
    public UcteNodeCode getUcteNodeCode(final String id) {
        return ucteNodeIds.computeIfAbsent(id, code -> parseUcteNodeCode(code).map(this::convertPisaNode)
                .orElseThrow(() -> new UcteException("Invalid UCTE node identifier: " + code)));
    }

    @Override
    public UcteNodeCode getUcteNodeCode(final Bus bus) {
        return getUcteNodeCode(bus.getId());
    }

    @Override
    public UcteNodeCode getUcteNodeCode(final BoundaryLine boundaryLine) {
        return getUcteNodeCode(boundaryLine.getPairingKey());
    }

    @Override
    public UcteElementId getUcteElementId(final String id) {
        return ucteElementIds.computeIfAbsent(id, code -> parseUcteElementId(code).map(this::convertPisaBranch)
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
    public UcteElementId getUcteElementId(final BoundaryLine boundaryLine) {
        return getUcteElementId(boundaryLine.getId());
    }

    private UcteNodeCode convertPisaNode(final UcteNodeCode ucteNodeCode) {
        if (FFG_NODE_NAME_PATTERN.matcher(ucteNodeCode.toString()).matches()) {
            ucteNodeCode.setUcteCountryCode(XX);
        }
        if (XPI_NODE_NAME_PATTERN.matcher(ucteNodeCode.toString()).matches()) {
            ucteNodeCode.setUcteCountryCode(UcteCountryCode.IT);
        }
        return ucteNodeCode;
    }

    private UcteElementId convertPisaBranch(final UcteElementId ucteElementId) {
        convertPisaNode(ucteElementId.getNodeCode1());
        convertPisaNode(ucteElementId.getNodeCode2());
        return ucteElementId;
    }
}
