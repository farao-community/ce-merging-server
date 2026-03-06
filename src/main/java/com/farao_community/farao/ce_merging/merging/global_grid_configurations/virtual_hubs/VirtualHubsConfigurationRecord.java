/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 */
package com.farao_community.farao.ce_merging.merging.global_grid_configurations.virtual_hubs;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class VirtualHubsConfigurationRecord {
    @Id
    private UUID id;

    private LocalDateTime validFrom;

    private LocalDateTime validTo;

    private LocalDateTime publishedOn;

    @Lob
    private String configurationJson;
}
