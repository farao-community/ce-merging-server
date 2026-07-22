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
