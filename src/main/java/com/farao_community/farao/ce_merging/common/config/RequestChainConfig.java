/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.config;

import com.farao_community.farao.ce_merging.common.util.chain.Chain;
import com.farao_community.farao.ce_merging.common.util.chain.ChainBuilder;
import com.farao_community.farao.ce_merging.common.util.chain.Handler;
import com.farao_community.farao.ce_merging.post_process.PostProcessRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.stream.IntStream;

@Configuration
public class RequestChainConfig {

    @Bean
    public Chain<PostProcessRequest> postProcessRequestChain(final List<Handler<PostProcessRequest>> handlers) {
        final ChainBuilder<PostProcessRequest> builder = ChainBuilder.chainBuilder();
        final ChainBuilder<PostProcessRequest>.SuccessorBuilder successorBuilder = builder.first(handlers.getFirst());
        IntStream.range(1, handlers.size())
            .forEach(i -> successorBuilder.next(handlers.get(i)));
        return successorBuilder.build();
    }

}
