/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.model.entity;

import com.farao_community.farao.ce_merging.global_grid_configurations.model.abstractions.AbstractXNodeConfig;
import com.farao_community.farao.ce_merging.xsd.Xnode;
import jakarta.persistence.Embeddable;

import java.io.Serializable;

@Embeddable
public class XnodeConfig extends AbstractXNodeConfig implements Serializable {

    public XnodeConfig() {
    }

    public XnodeConfig(final String name,
                       final String area1,
                       final String subarea1,
                       final String area2,
                       final String subarea2) {
        this.name = name;
        this.area1 = area1;
        this.area2 = area2;
        this.subarea1 = subarea1;
        this.subarea2 = subarea2;
    }

    public static XnodeConfig fromXNodeEntity(final Xnode xnode) {
        final XnodeConfig cfg = new XnodeConfig();
        cfg.setName(xnode.getName());
        cfg.setArea1(xnode.getArea1());
        cfg.setArea2(xnode.getArea2());
        cfg.setSubarea1(xnode.getSubarea1());
        cfg.setSubarea2(xnode.getSubarea2());
        return cfg;
    }
}
