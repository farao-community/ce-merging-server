package com.farao_community.farao.ce_merging.merging;

import com.farao_community.farao.ce_merging.merging.entities.MergingTask;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MergingTaskRepository extends CrudRepository<MergingTask, Long> {
}
