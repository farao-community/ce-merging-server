/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.request_metadata.model;

import com.farao_community.farao.ce_merging.merging.task.entities.Configurations;

public class Data {
    private String type;
    private AttributesMetadata attributes;
    private Configurations configurations;

    public AttributesMetadata getAttributes() {
        return attributes;
    }

    public void setAttributes(final AttributesMetadata attributes) {
        this.attributes = attributes;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public Configurations getConfigurations() {
        return configurations;
    }

    public void setConfigurations(final Configurations configurations) {
        this.configurations = configurations;
    }
}
