/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.post_process.merging_supervisor_logs;

import com.farao_community.farao.ce_merging.merging.post_process.merging_supervisor_logs.entity.LoggingEvent;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface LoggingEventRepository extends CrudRepository<LoggingEvent, Long> {

    @Query("select l from LoggingEvent l, LoggingEventProperty lp, LoggingEventProperty lp2 " +
           "where lp.mappedKey = 'taskId' and lp.mappedValue = ?1 and lp.loggingEvent = l " +
           "and lp2.loggingEvent = l and lp2.mappedKey = 'merging-step' ")
    Set<LoggingEvent> findLogsByTaskId(Long taskId);
}
