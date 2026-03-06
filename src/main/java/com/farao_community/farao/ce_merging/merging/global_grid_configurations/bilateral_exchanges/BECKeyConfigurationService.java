/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 */
package com.farao_community.farao.ce_merging.merging.global_grid_configurations.bilateral_exchanges;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.exception.ServiceIOException;
import com.farao_community.farao.ce_merging.common.util.serialization.JsonUtils;
import com.farao_community.farao.ce_merging.merging.global_grid_configurations.region_eic.RegionConfigurationDto;
import com.farao_community.farao.ce_merging.merging.global_grid_configurations.region_eic.RegionConfigurationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.CSV_SEPARATOR;
import static java.nio.charset.StandardCharsets.UTF_8;

@Service
@AllArgsConstructor
@Slf4j
public class BECKeyConfigurationService {

    public static final String BEC_KEYS_DEFAULT_CONFIGURATION = "gridDefaultConfigurations/sharingKeysBEC.csv";
    private static final String ARROW = "->";
    private final BECKeyConfigurationRepository repository;
    private final RegionConfigurationService regionConfigurationService;

    public void publishBECKeyConfiguration(final MultipartFile configurationFile,
                                           final OffsetDateTime validFrom,
                                           final OffsetDateTime validTo) {
        try {
            final String configFileCsvContent = new String(configurationFile.getBytes(), UTF_8);
            if (!configFileCsvContent.isEmpty()) {
                final List<BecByBoundaryDto> becMatrix = parseSharingKeysBEC(validFrom, configFileCsvContent);
                final BECKeyConfigurationRecord cfgRrecord = new BECKeyConfigurationRecord(UUID.randomUUID().toString(),
                                                                                           validFrom.toLocalDateTime(),
                                                                                           validTo.toLocalDateTime(),
                                                                                           LocalDateTime.now(),
                                                                                           becMatrix);
                repository.save(cfgRrecord);
            }
        } catch (final Exception e) {
            log.error("Bec Keys configuration cannot be published to server");
            throw new CeMergingException("Bec Keys configuration could not be published, file or dates could be invalid.");
        }
    }

    public List<BecByBoundaryDto> parseSharingKeysBEC(final OffsetDateTime validFrom,
                                                      final String csvContent) throws IOException {
        final List<List<String>> exchanges = new ArrayList<>();
        final RegionConfigurationDto regionConfiguration = regionConfigurationService
            .getRegionConfiguration(validFrom)
            .getRegionConfiguration();

        // extract CSV elements as strings
        try (final BufferedReader br = new BufferedReader(new StringReader(csvContent))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(CSV_SEPARATOR);
                exchanges.add(Arrays.asList(values));
            }
        } catch (final IOException e) {
            log.error("Could not parse sharing keys BEC file from class resources");
            throw new ServiceIOException("Could not parse sharing keys BEC file from class resources", e);
        }

        final List<BecByBoundaryDto> becMatrix = new ArrayList<>();
        final List<BorderDto> borderList = new ArrayList<>();

        // transform string matrix to becMatrix
        for (int line = 1; line < exchanges.size(); line++) {
            final List<String> currentExchange = exchanges.get(line);

            final String countryOut = currentExchange.getFirst().split(ARROW)[0];
            final String countryIn = currentExchange.getFirst().split(ARROW)[1];

            borderList.add(new BorderDto(regionConfiguration.getAreasAll().get(countryOut),
                                         regionConfiguration.getAreasIn().get(countryIn)));

            final List<BecCoefficientsDto> bilateralExchanges = new ArrayList<>();

            for (int column = 1; column < exchanges.get(1).size(); column++) {
                final String country = exchanges.getFirst().get(column);
                final double coefficient = Double.parseDouble(exchanges.get(line).get(column)
                                                      .replace(",", "."));
                bilateralExchanges.add(new BecCoefficientsDto(country, coefficient));
            }

            becMatrix.add(new BecByBoundaryDto(borderList.get(line - 1),
                                               bilateralExchanges));
        }
        return becMatrix;
    }

    public byte[] retrieveBECKeyConfiguration(final OffsetDateTime targetDate) throws Exception {
        return JsonUtils.writeInBytes(JsonBecConfiguration.class,
                                      getBECKeyConfiguration(targetDate));
    }

    public JsonBecConfiguration getBECKeyConfiguration(final OffsetDateTime targetDate) throws Exception {
        try {
            final BECKeyConfigurationRecord cfg = repository.findLastPublishedValidBetween(targetDate.toLocalDateTime(),
                                                                                           targetDate.toLocalDateTime());
            log.info("BEC keys configuration is retrieved from configuration server");
            return new JsonBecConfiguration(cfg.getBecMatrix());
        } catch (final Exception e) {
            final String defaultCsvBecContent = new String(new ClassPathResource(BEC_KEYS_DEFAULT_CONFIGURATION)
                                                               .getInputStream()
                                                               .readAllBytes());

            return new JsonBecConfiguration(parseSharingKeysBEC(targetDate, defaultCsvBecContent));
        }
    }

}
