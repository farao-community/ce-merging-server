/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.daily_merging;

import com.farao_community.farao.ce_merging.daily_merging.entities.DailyMergingTask;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DailyMergingRepository extends CrudRepository<DailyMergingTask, Long> {
    @Query("select t.id from DailyMergingTask t")
    List<Long> findAllIds();
}
