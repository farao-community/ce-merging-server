/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.bilateral_exchanges;

import com.farao_community.farao.ce_merging.common.exception.ServiceIOException;
import com.farao_community.farao.ce_merging.global_grid_configurations.abstraction.AbstractGridConfigurationService;
import com.farao_community.farao.ce_merging.global_grid_configurations.region_eic.RegionConfigurationDto;
import com.farao_community.farao.ce_merging.global_grid_configurations.region_eic.RegionConfigurationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.CSV_SEPARATOR;

@Service
@AllArgsConstructor
@Slf4j
public class BECKeyConfigurationService extends AbstractGridConfigurationService<BECKeyConfigurationRecord, JsonBecConfiguration> {

    private static final String ARROW = "->";
    private final RegionConfigurationService regionConfigurationService;

    @Override
    protected JsonBecConfiguration getDefaultConfiguration(final OffsetDateTime targetDate) throws IOException {
        final String defaultCsvBecContent = new String(getDefaultFileBytes());
        return new JsonBecConfiguration(parseBecSharingKeys(targetDate, defaultCsvBecContent));
    }

    @Override
    protected JsonBecConfiguration getConfigurationfromRecord(final BECKeyConfigurationRecord cfgRecord) {
        return new JsonBecConfiguration(cfgRecord.getBecMatrix());
    }

    @Override
    protected BECKeyConfigurationRecord buildFromFile(final MultipartFile configurationFile,
                                                      final OffsetDateTime validFrom,
                                                      final OffsetDateTime validTo) throws IOException {
        final String configFileCsvContent = getTextContent(configurationFile);
        final List<BecByBoundaryDto> becMatrix = parseBecSharingKeys(validFrom, configFileCsvContent);
        return new BECKeyConfigurationRecord(generateId(),
                                             validFrom.toLocalDateTime(),
                                             validTo.toLocalDateTime(),
                                             LocalDateTime.now(),
                                             becMatrix);
    }

    public List<BecByBoundaryDto> parseBecSharingKeys(final OffsetDateTime validFrom,
                                                      final String csvContent) throws IOException {
        final List<List<String>> exchanges = new ArrayList<>();
        final RegionConfigurationDto regionConfiguration = regionConfigurationService
            .getConfiguration(validFrom)
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
}
