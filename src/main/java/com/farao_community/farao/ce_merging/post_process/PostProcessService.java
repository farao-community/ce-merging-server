/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.post_process;

import com.farao_community.farao.ce_merging.common.util.chain.Chain;
import org.springframework.stereotype.Service;

@Service
public class PostProcessService {

    private final Chain<PostProcessRequest> requestChain;

    public PostProcessService(final Chain<PostProcessRequest> requestChain) {
        this.requestChain = requestChain;
    }

    public PostProcessRequest process(final PostProcessRequest request) {
        this.requestChain.handle(request);
        return request;
    }

}
