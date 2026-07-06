package com.farao_community.farao.ce_merging.common.task;

import com.farao_community.farao.ce_merging.common.exception.task.TaskNotRunException;

import static com.farao_community.farao.ce_merging.common.task.TaskStatus.CREATED;
import static com.farao_community.farao.ce_merging.common.task.TaskStatus.RUNNING;

public interface Task {
    Long getId();

    String getName();

    TaskStatus getStatus();

    void setId(Long id);

    void setName(String name);

    void setStatus(TaskStatus status);
    
    default void assertFinished() {
        if (this.getStatus() == CREATED) {
            throw new TaskNotRunException(String.format("Task %d has not been run", this.getId()));
        } else if (this.getStatus() == RUNNING) {
            throw new TaskNotRunException(String.format("Task %d is currently running", this.getId()));
        }
    }
}
