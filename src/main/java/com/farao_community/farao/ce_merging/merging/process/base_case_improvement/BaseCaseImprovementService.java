/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.base_case_improvement;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import org.springframework.stereotype.Service;

@Service
public class BaseCaseImprovementService {

    private final CeMergingConfiguration configuration;

    public BaseCaseImprovementService(CeMergingConfiguration configuration) {
        this.configuration = configuration;
    }

    public void computeTargetNetPositions(MergingTask task) {
        BciProcessor processor = new BciProcessor(task, configuration);
        processor.run();
    }
}
