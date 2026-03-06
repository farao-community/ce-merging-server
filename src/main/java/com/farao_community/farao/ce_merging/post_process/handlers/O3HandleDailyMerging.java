package com.farao_community.farao.ce_merging.post_process.handlers;

import com.farao_community.farao.ce_merging.merging.entities.MergingTask;
import com.farao_community.farao.ce_merging.common.util.chain.Handler;
import com.farao_community.farao.ce_merging.post_process.PostProcessRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.farao_community.farao.ce_merging.merging.MergingTaskPredicates.isSuccessful;

@Component
@Order(20)
public class O3HandleDailyMerging implements Handler<PostProcessRequest> {

    @Override
    public boolean handle(final PostProcessRequest request) {
        final List<MergingTask> successfulTasks = request.getMergingTasks()
            .stream()
            .filter(isSuccessful())
            .toList();
        if (!successfulTasks.isEmpty()) {
            //oneDayRefProgBuilder.computeOneDayRefProg(dailyCoreMergingEntity, successfulTasks, requestInformation);
            //oneDayMergingLogsBuilder.computeOneDayMergingLogs(dailyCoreMergingEntity, successfulTasks);
            //oneDayQualityCheckReportBuilder.computeOneDayGlskQualityReport(dailyCoreMergingEntity, successfulTasks, requestTimeInterval);
        }
        return false;
    }
}
