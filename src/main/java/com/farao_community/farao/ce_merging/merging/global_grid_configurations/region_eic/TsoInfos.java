/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 */
package com.farao_community.farao.ce_merging.merging.global_grid_configurations.region_eic;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "tsoinfos")
@Data
public class TsoInfos {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long ref;

    @Column(name = "name")
    @JsonProperty(value = "tsoName")
    private String name;

    @Column(name = "eic")
    private String eic;
}
