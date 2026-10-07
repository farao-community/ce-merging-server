/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.repository;

import com.farao_community.farao.ce_merging.global_grid_configurations.model.records.BECKeyConfigurationRecord;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface BECKeyConfigurationRepository extends CrudRepository<BECKeyConfigurationRecord, String> {

    @Query("select r from BECKeyConfigurationRecord r where r.validFrom <= :date and r.validTo > :date order by r.publishedOn desc limit 1")
    BECKeyConfigurationRecord findLatestValidOfType(@Param("date") LocalDateTime date);
}
