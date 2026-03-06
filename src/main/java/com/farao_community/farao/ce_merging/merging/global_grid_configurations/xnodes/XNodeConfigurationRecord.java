/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 */
package com.farao_community.farao.ce_merging.merging.global_grid_configurations.xnodes;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class XNodeConfigurationRecord {
    @Id
    private String id;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private LocalDateTime publishedOn;

    @ElementCollection(fetch = FetchType.LAZY)
    private List<XnodeDto> xNodeList = new ArrayList<>();

}
