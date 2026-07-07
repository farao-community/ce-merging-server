/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.model.entity;

import com.farao_community.farao.ce_merging.global_grid_configurations.model.abstractions.AbstractTsoInfos;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;

import static jakarta.persistence.GenerationType.AUTO;

@Entity
@Table(name = "tsoinfos")
public class TsoInfos extends AbstractTsoInfos implements Serializable {

    @Id
    @GeneratedValue(strategy = AUTO)
    private Long ref;

    @Column(name = "name")
    @JsonProperty(value = "tsoName")
    private String name;

    @Column(name = "eic")
    private String eic;

    public Long getRef() {
        return ref;
    }

    public void setRef(final Long ref) {
        this.ref = ref;
    }
}
