/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging;

import com.farao_community.farao.ce_merging.common.chain.Chain;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import org.springframework.stereotype.Service;

@Service
public class MergingService {

    private final Chain<MergingTask> mergingTaskChain;

    public MergingService(final Chain<MergingTask> mergingTaskChain) {
        this.mergingTaskChain = mergingTaskChain;
    }

    public void run(final MergingTask task) {
        this.mergingTaskChain.handle(task);
    }
}
