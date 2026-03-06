/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 */
package com.farao_community.farao.ce_merging.merging.global_grid_configurations.region_eic;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.util.serialization.JsonUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;

@Service
@AllArgsConstructor
@Slf4j
public class RegionConfigurationService {

    public static final String REGION_DEFAULT_CONFIGURATION = "gridDefaultConfigurations/region_configuration.json";
    private final RegionConfigurationRepository repository;

    public void publishRegionConfiguration(final MultipartFile configurationFile,
                                           final OffsetDateTime validFrom,
                                           final OffsetDateTime validTo) {
        try {
            String cfgFileContent = new String(configurationFile.getBytes(), UTF_8);
            if (!cfgFileContent.isEmpty()) {
                final RegionConfigurationDto regionConfiguration = JsonUtils.read(RegionConfigurationDto.class,
                                                                                  configurationFile);

                repository.save(new RegionConfigurationRecord(UUID.randomUUID().toString(),
                                                              validFrom.toLocalDateTime(),
                                                              validTo.toLocalDateTime(),
                                                              LocalDateTime.now(),
                                                              regionConfiguration));
            }
        } catch (final Exception e) {
            log.error("EIC codes configuration cannot be published to server");
            throw new CeMergingException("EIC Codes configuration could not be published, File or dates could be invalid.");
        }
    }

    public byte[] retrieveRegionConfiguration(final OffsetDateTime targetDate) throws IOException {
        return JsonUtils.writeInBytes(RegionConfigurationDto.class,
                                      getRegionConfiguration(targetDate)
                                          .getRegionConfiguration());
    }

    public JsonRegionConfiguration getRegionConfiguration(final OffsetDateTime targetDate) throws
        IOException {
        try {
            final RegionConfigurationRecord cfg = repository.findLastPublishedValidBetween(targetDate.toLocalDateTime(),
                                                                                           targetDate.toLocalDateTime());
            return new JsonRegionConfiguration(cfg.getRegionConfiguration());
        } catch (final Exception e) {
            log.warn("No region eic configuration could be found on the config repository, Default configuration will be used");
            return JsonUtils.read(JsonRegionConfiguration.class,
                                  new ClassPathResource(REGION_DEFAULT_CONFIGURATION).getInputStream());
        }
    }

}
