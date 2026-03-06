/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 */
package com.farao_community.farao.ce_merging.merging.global_grid_configurations.region_eic;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static jakarta.persistence.CascadeType.ALL;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegionConfigurationRecord {
    @Id
    private String id;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private LocalDateTime publishedOn;
    @OneToOne(cascade = ALL)
    private RegionConfigurationDto regionConfiguration;
}
