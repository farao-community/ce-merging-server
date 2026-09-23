/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations;

import com.farao_community.farao.ce_merging.global_grid_configurations.model.records.AbstractGridConfigurationRecord;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface GridConfigurationRepository<T extends AbstractGridConfigurationRecord> extends CrudRepository<T, String> {
    T findFirstByValidFromLessThanEqualAndValidToGreaterThanOrderByPublishedOnDesc(LocalDateTime date1, LocalDateTime date2);

    @Query("select r from AbstractGridConfigurationRecord r where type(r) = :type"
           + " and r.validFrom <= :date and r.validTo > :date order by r.publishedOn desc")
   List<T> findLatestValidOfType(@Param("type") Class<? extends AbstractGridConfigurationRecord> type, @Param("date") LocalDateTime date, Limit limit);
}
