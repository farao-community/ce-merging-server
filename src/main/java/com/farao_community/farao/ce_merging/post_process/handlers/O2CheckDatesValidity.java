package com.farao_community.farao.ce_merging.post_process.handlers;

import com.farao_community.farao.ce_merging.merging.entities.MergingTask;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.util.chain.Handler;
import com.farao_community.farao.ce_merging.post_process.PostProcessRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.farao_community.farao.ce_merging.common.util.DateTimeUtils.convertToZFormat;
import static com.farao_community.farao.ce_merging.merging.MergingTaskPredicates.isBetween;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.Comparator.comparing;
import static java.util.function.Predicate.not;

@Component
@Order(10)
@Slf4j
public class O2CheckDatesValidity implements Handler<PostProcessRequest> {
    @Override
    public boolean handle(final PostProcessRequest request) {
        checkIfTargetDatesInRequestInterval(request);
        checkIfDistinctTargetDates(request.getMergingTasks());
        final Comparator<MergingTask> byTargetDateTime = comparing(task -> task.getInputs().getTargetDate());
        request.getMergingTasks().sort(byTargetDateTime);
        return false;
    }

    private void checkIfTargetDatesInRequestInterval(final PostProcessRequest request) {
        final OffsetDateTime requestStart = request.getStartDateTime();
        final OffsetDateTime requestEnd = request.getEndDateTime().minusMinutes(1);

        request.getMergingTasks()
            .stream()
            .filter(not(isBetween(requestStart, requestEnd)))
            .findAny()
            .ifPresent(task -> {
                String errorMessage = String.format("Task's %s target date %s outside merging request time interval %s",
                                                    task.getTaskId(),
                                                    task.getInputs().getTargetDate(),
                                                    request.getRequestTimeInterval());
                log.error(errorMessage);
                throw new CeMergingException(errorMessage);
            });

    }

    private void checkIfDistinctTargetDates(final List<MergingTask> tasksList) {
        final Set<String> tasksIntervals = new HashSet<>();

        tasksList.stream()
            .map(task -> task.getInputs().getTargetDate().truncatedTo(HOURS))
            .map(dt -> convertToZFormat(dt) + "/" + convertToZFormat(dt.plusHours(1)))
            .forEach(interval -> {
                if (!tasksIntervals.add(interval)) {
                    final String errorMessage = String.format("More than one task with same target date inside interval %s",
                                                              interval);
                    log.error(errorMessage);
                    throw new CeMergingException(errorMessage);
                }
            });
    }
}
