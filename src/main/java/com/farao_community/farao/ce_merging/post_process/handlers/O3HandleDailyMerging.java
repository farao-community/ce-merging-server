package com.farao_community.farao.ce_merging.post_process.handlers;

import com.farao_community.farao.ce_merging.common.entities.CeMergingTaskEntity;
import com.farao_community.farao.ce_merging.common.util.chain.Handler;
import com.farao_community.farao.ce_merging.post_process.PostProcessRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.farao_community.farao.ce_merging.common.entities.enums.TaskStatus.SUCCESS;

@Component
@Order(20)
@Slf4j
public class O3HandleDailyMerging implements Handler<PostProcessRequest> {
    @Override
    public boolean handle(final PostProcessRequest request) {
        final List<CeMergingTaskEntity> successfulTasks = request.getCeMergingTaskEntities()
            .stream()
            .filter(task -> SUCCESS.equals(task.getTaskStatus()))
            .toList();
        if (!successfulTasks.isEmpty()) {
            //oneDayRefProgBuilder.computeOneDayRefProg(dailyCoreMergingEntity, successfulTasks, requestInformation);
            //oneDayMergingLogsBuilder.computeOneDayMergingLogs(dailyCoreMergingEntity, successfulTasks);
            //oneDayQualityCheckReportBuilder.computeOneDayGlskQualityReport(dailyCoreMergingEntity, successfulTasks, requestTimeInterval);
        }
        return false;
    }
}
