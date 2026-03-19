/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.task.entities;

import jakarta.persistence.Embeddable;

import java.io.Serializable;

@Embeddable
public class BorderDirectionRecord implements Serializable {
    private String borderFrom;
    private String borderTo;

    public String getBorderFrom() {
        return borderFrom;
    }

    public void setBorderFrom(final String borderFrom) {
        this.borderFrom = borderFrom;
    }

    public String getBorderTo() {
        return borderTo;
    }

    public void setBorderTo(final String borderTo) {
        this.borderTo = borderTo;
    }
}
