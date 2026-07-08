/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.inputs;

import com.farao_community.farao.ce_merging.xsd.NetPositionConstraint;

public class ExternalConstraintsInputs {

    private String areaId;
    private String direction;
    private Double value;

    public ExternalConstraintsInputs(final String areaId, final String direction, final Double value) {
        this.areaId = areaId;
        this.direction = direction;
        this.value = value;
    }

    public static ExternalConstraintsInputs fromNetPositionConstraint(final NetPositionConstraint npc) {
        return new ExternalConstraintsInputs(npc.getHub().toUpperCase(),
                                             npc.getDirection(),
                                             npc.getValue().doubleValue());
    }

    public String getAreaId() {
        return areaId;
    }

    public void setAreaId(String areaId) {
        this.areaId = areaId;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }
}
