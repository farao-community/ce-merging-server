/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.model.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;

import static jakarta.persistence.GenerationType.AUTO;

@Entity
@Table(name = "tsoinfos")
public class TsoInfos implements Serializable {

    @Id
    @GeneratedValue(strategy = AUTO)
    private long ref;

    @Column(name = "name")
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

    public String getEic() {
        return eic;
    }

    public void setEic(final String eic) {
        this.eic = eic;
    }

    @Override
    public boolean equals(final Object o) {
        return EqualsBuilder.reflectionEquals(this, o, "ref");
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this, "ref");
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
