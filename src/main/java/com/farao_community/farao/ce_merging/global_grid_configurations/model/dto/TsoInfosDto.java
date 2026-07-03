/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.NAME;

@Entity
@Table(name = "tsoinfosdto")
public class TsoInfosDto implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long ref;

    @Column(name = NAME)
    @JsonProperty(value = "tsoName")
    private String name;

    @Column(name = "eic")
    private String eic;

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Long getRef() {
        return ref;
    }

    public void setRef(final Long ref) {
        this.ref = ref;
    }

    public String getEic() {
        return eic;
    }

    public void setEic(final String eic) {
        this.eic = eic;
    }
}
