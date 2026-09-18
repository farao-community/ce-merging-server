/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.ce_merging.merging.task.mapper;

import com.farao_community.farao.ce_merging.merging.task.dto.ArtifactsDto;
import com.farao_community.farao.ce_merging.merging.task.entities.Artifacts;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType;
import org.mapstruct.Mapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.ALEGRO_NET_POSITIONS;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.BALANCED_CGM_FILE;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.BALANCES_ADJUSTMENT_TARGET_FILE;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.BCI_OUTPUT_FILE;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.CGM_FILE_AFTER_PST;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.CGM_NET_POSITIONS_FILE;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.DK_CONVERTED_FILE;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.GERMAN_PRE_MERGED_IGM;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.GLSK_QUALITY_REPORT;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.IGMS_NET_POSITIONS_FILE;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.LOAD_FLOW_ON_FINAL_CGM_LOGS;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.PST_OUTPUT_FILE;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.TOPOLOGICAL_MERGE_FILE;

@Mapper(componentModel = "spring")
public interface ArtifactsMapper {
    default ArtifactsDto artifactsToArtifactsDto(final Artifacts artifacts) {
        final ArtifactsDto mapped = new ArtifactsDto();

        final Map<Consumer<String>, ArtifactType> settersByType = new HashMap<>(Map.of(
                mapped::setAlegroNetPositionsLocation, ALEGRO_NET_POSITIONS,
                mapped::setBalancedCgmFileLocation, BALANCED_CGM_FILE,
                mapped::setBalancesAdjustmentTargetFileLocation, BALANCES_ADJUSTMENT_TARGET_FILE,
                mapped::setBciOutputFileLocation, BCI_OUTPUT_FILE,
                mapped::setCgmNetPositionsFileLocation, CGM_NET_POSITIONS_FILE,
                mapped::setDkConvertedFileLocation, DK_CONVERTED_FILE,
                mapped::setGermanPreMergedIgmFileLocation, GERMAN_PRE_MERGED_IGM,
                mapped::setGlskQualityReportLocation, GLSK_QUALITY_REPORT,
                mapped::setIgmsNetPositionsFileLocation, IGMS_NET_POSITIONS_FILE,
                mapped::setLoadflowOnFinalCgmLogsLocation, LOAD_FLOW_ON_FINAL_CGM_LOGS
        ));

        settersByType.putAll(Map.of(mapped::setPstAppliedCgmFileLocation, CGM_FILE_AFTER_PST,
                                    mapped::setPstOutputFileLocation, PST_OUTPUT_FILE,
                                    mapped::setTopologicalMergeFileLocation, TOPOLOGICAL_MERGE_FILE));

        settersByType.forEach((setter, type) -> setter.accept(Optional.ofNullable(artifacts.getFile(type))
                                                                      .map(SavedFile::getLocation)
                                                                      .orElse(null)));
        return mapped;
    }
}
