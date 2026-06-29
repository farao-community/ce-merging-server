/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.config;

import com.farao_community.farao.ce_merging.common.chain.Chain;
import com.farao_community.farao.ce_merging.common.chain.ChainBuilder;
import com.farao_community.farao.ce_merging.common.chain.Handler;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.stream.IntStream;

/**
 * class used to define handler chain beans
 * to create a new one, use the same implementation with a different class
 */
@Configuration
public class TaskChainConfig {

    @Bean
    public Chain<MergingTask> mergingProcessTaskChain(final List<Handler<MergingTask>> handlers) {
        final ChainBuilder<MergingTask> builder = ChainBuilder.chainBuilder();
        final ChainBuilder<MergingTask>.SuccessorBuilder successorBuilder = builder.first(handlers.getFirst());
        IntStream.range(1, handlers.size())
            .mapToObj(handlers::get)
            .forEach(successorBuilder::next);
        return successorBuilder.build();
    }

}
