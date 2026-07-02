package com.farao_community.farao.ce_merging;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.TAG_VERSION;

@Configuration
public class CeMergingSwaggerConfig {

    @Bean
    public OpenAPI springShopOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("CE merging tasks management API")
                        .description("This REST API manages CE merging tasks.")
                        .version(TAG_VERSION));
    }

}
