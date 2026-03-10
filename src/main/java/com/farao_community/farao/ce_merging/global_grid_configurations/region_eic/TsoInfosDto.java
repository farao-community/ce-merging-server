/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.region_eic;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.NAME;

@Entity
@Data
@Table(name = "tsoinfosdto")
public class TsoInfosDto {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long ref;

    @Column(name = NAME)
    @JsonProperty(value = "tsoName")
    @Getter
    @Setter
    private String name;

    @Column(name = "eic")
    @Getter
    @Setter
    private String eic;
}
