package com.farao_community.farao.ce_merging.common.task;

public interface Task {
    Long getId();

    String getName();

    TaskStatus getStatus();

    void setId(Long id);

    void setName(String name);

    void setStatus(TaskStatus status);
}
