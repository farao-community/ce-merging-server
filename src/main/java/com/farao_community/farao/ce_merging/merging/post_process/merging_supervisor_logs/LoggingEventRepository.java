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
