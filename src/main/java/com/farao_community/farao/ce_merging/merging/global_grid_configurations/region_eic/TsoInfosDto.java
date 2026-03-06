/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
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
import lombok.Getter;
import lombok.Setter;

@Entity
@Data
@Table(name = "tsoinfosdto")
public class TsoInfosDto {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long ref;

    @Column(name = "name")
    @JsonProperty(value = "tsoName")
    @Getter
    @Setter
    private String name;

    @Column(name = "eic")
    @Getter
    @Setter
    private String eic;
}
