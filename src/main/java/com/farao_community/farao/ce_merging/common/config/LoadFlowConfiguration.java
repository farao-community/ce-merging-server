/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.config;

import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Supplier;

@Configuration
public class LoadFlowConfiguration {
    @Bean
    public Supplier<LoadFlow.Runner> loadflowSupplier() {
        return LoadFlow::find;
    }

    @Bean
    public Supplier<LoadFlowParameters> loadFlowParametersSupplier() {
        return LoadFlowParameters::load;
    }
}
