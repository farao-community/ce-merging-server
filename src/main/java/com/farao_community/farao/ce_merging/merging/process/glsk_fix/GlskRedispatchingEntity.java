/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.glsk_fix;

public class GlskRedispatchingEntity {
    private String id;
    private double share;

    public GlskRedispatchingEntity(String id, double share) {
        this.id = id;
        this.share = share;
    }

    public String getId() {
        return id;
    }

    public double getShare() {
        return share;
    }
}
