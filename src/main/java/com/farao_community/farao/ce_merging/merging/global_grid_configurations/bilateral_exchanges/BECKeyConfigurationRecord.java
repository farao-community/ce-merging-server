/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 */
package com.farao_community.farao.ce_merging.merging.global_grid_configurations.bilateral_exchanges;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.CascadeType.ALL;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BECKeyConfigurationRecord {
    @Id
    private String id;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private LocalDateTime publishedOn;
    @OneToMany(cascade = ALL)
    private List<BecByBoundaryDto> becMatrix = new ArrayList<>();
}
