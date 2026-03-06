package com.farao_community.farao.ce_merging.merging.mapper;

import com.farao_community.farao.ce_merging.merging.dto.MergingTaskDto;
import com.farao_community.farao.ce_merging.merging.entities.MergingTask;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MergingTaskMapper {
    MergingTaskDto mergingTaskToMergingTaskDto(MergingTask mergingTask);
}
