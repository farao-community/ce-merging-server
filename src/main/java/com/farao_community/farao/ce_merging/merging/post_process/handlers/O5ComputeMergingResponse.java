/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.ce_merging.merging.post_process.handlers;

import com.farao_community.farao.ce_merging.common.chain.Handler;
import com.farao_community.farao.ce_merging.merging.post_process.PostProcessRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(50)
public class O5ComputeMergingResponse implements Handler<PostProcessRequest> {
    @Override
    public boolean handle(final PostProcessRequest request) {
        return false;
    }
}
