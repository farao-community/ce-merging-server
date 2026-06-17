package com.farao_community.farao.ce_merging.merging.task.mapper;

import com.farao_community.farao.ce_merging.merging.task.dto.ArtifactsDto;
import com.farao_community.farao.ce_merging.merging.task.entities.Artifacts;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import org.junit.jupiter.api.Test;

import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.CGM_NET_POSITIONS_FILE;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.XNODES_INFORMATION_FILE;
import static org.assertj.core.api.Assertions.assertThat;

class MergingTaskMapperTest {

    @Test
    void shouldMapTaskWithArtifacts() {
        final MergingTask task = new MergingTask();
        final Artifacts artifacts = new Artifacts();
        final SavedFile file = new SavedFile();
        file.setLocation(".");
        artifacts.putFile(CGM_NET_POSITIONS_FILE, file);
        task.setArtifacts(artifacts);

        final ArtifactsDto dto = new MergingTaskMapperImpl().mergingTaskToMergingTaskDto(task).getArtifacts();

        assertThat(dto.getLocation(CGM_NET_POSITIONS_FILE)).isNotNull();
        assertThat(dto.getLocation(XNODES_INFORMATION_FILE)).isNull();
    }

}