/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.post_process.ref_prog;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.model.netpositions.NetPositions;
import com.farao_community.farao.ce_merging.common.util.CountryCodeUtils;
import com.farao_community.farao.ce_merging.common.util.JaxbUtils;
import com.farao_community.farao.ce_merging.common.util.JsonUtils;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.BecByBoundary;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.BecCoefficients;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.Border;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.RegionConfiguration;
import com.farao_community.farao.ce_merging.global_grid_configurations.services.VirtualHubsConfigurationService;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.inputs.ReferenceExchangeData;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.inputs.ReferenceProgram;
import com.farao_community.farao.ce_merging.merging.process.final_cgm_result.FinalCgmResult;
import com.farao_community.farao.ce_merging.merging.task.MergingTaskRepository;
import com.farao_community.farao.ce_merging.merging.task.entities.BorderDirectionRecord;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
//TODO import com.farao_community.farao.ce_merging.LogsCustomisationService;
import com.farao_community.farao.ce_merging.merging.task.entities.VirtualHubRecord;
import com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType;
//TODO import com.farao_community.farao.ce_merging.post_process.merging_supervisor_logs.MergingCoreStep;
import com.farao_community.farao.ce_merging.xsd.ref_prog.PublicationDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */
@Service
public class RefProgCalculationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RefProgCalculationService.class);

    private final FinalRefProgBuilder finalRefProgBuilder;
    private final CeMergingConfiguration configuration;
    private final MergingTaskRepository mergingTaskRepository;
    //TODO private final LogsCustomisationService logsCustomisationService;
    private final VirtualHubsConfigurationService virtualHubsConfigurationService;

    public RefProgCalculationService(final MergingTaskRepository mergingTaskRepository,
                                     final CeMergingConfiguration configuration,
                                     final FinalRefProgBuilder finalRefProgBuilder,
                                     // TODO final LogsCustomisationService logsCustomisationService,
                                     final VirtualHubsConfigurationService virtualHubsConfigurationService) {
        this.configuration = configuration;
        this.finalRefProgBuilder = finalRefProgBuilder;
        this.mergingTaskRepository = mergingTaskRepository;
        //TODO this.logsCustomisationService = logsCustomisationService;
        this.virtualHubsConfigurationService = virtualHubsConfigurationService;
    }

    public void computeRefProg(final MergingTask mergingTask) {
        try {
            //TODO logsCustomisationService.setExtraFieldsInLogsMdc(mergingTask.getTaskId(), MergingCoreStep.REF_PROG.toString());

            final Map<Border, Double> virtualHubsExchanges = new HashMap<>();
            final Map<Border, Double> acExchanges = new HashMap<>();
            final ReferenceProgram referenceProgram = JsonUtils.read(ReferenceProgram.class, mergingTask.getArtifacts().getFile(ArtifactType.REFERENCE_PROGRAM_FORECAST_FILE).getPath());
            final FinalCgmResult finalCgmResult = JsonUtils.read(FinalCgmResult.class, mergingTask.getArtifacts().getFile(ArtifactType.CGM_NET_POSITIONS_FILE).getPath());

            computeExchanges(mergingTask, virtualHubsExchanges, acExchanges, referenceProgram, finalCgmResult);

            final RefProgResult refProgResult = new RefProgResult(referenceProgram.getDailyTimeInterval(), acExchanges, virtualHubsExchanges);
            final PublicationDocument finalRefProgResult = finalRefProgBuilder.buildFinalRefProgResult(refProgResult, mergingTask);
            saveRefProgFileInOutputs(finalRefProgResult, mergingTask);
            mergingTaskRepository.save(mergingTask);
        } catch (Exception e) {
            final String errorMessage = String.format("RefProg computation failed for task %d with target date %s, cause: %s", mergingTask.getId(), mergingTask.getInputs().getTargetDate(), e.getMessage());
            LOGGER.error(errorMessage);
            throw new CeMergingException(errorMessage, e);
        }
    }

    private static void computeExchanges(final MergingTask taskEntity,
                                         final Map<Border, Double> virtualHubsExchanges,
                                         final Map<Border, Double> acExchanges,
                                         final ReferenceProgram referenceProgram,
                                         final FinalCgmResult finalCgmResult) {

        final List<BorderDirectionRecord> borderDirectionRecords = taskEntity.getConfigurations().getBorderDirectionRecords();
        final RegionConfiguration regionConfiguration = taskEntity.getConfigurations().getRegionConfiguration();
        final List<VirtualHubRecord> virtualHubRecords = taskEntity.getConfigurations().getVirtualHubList();
        final List<BecByBoundary> sharingKeysBEC = taskEntity.getConfigurations().getBecMatrixConfig();

        final Map<String, Double> virtualHubsExchangesFromCgm = getVirtualHubsFromCgm(finalCgmResult);
        final List<ReferenceExchangeData> referenceExchangeDataList = referenceProgram.getReferenceExchangeDataList();
        final Map<String, Double> acNetPositionOutCountry = getAcNetPositionOutCountry(regionConfiguration, referenceExchangeDataList);

        borderDirectionRecords.forEach(borderDirection -> {
            String borderFrom = CountryCodeUtils.mapDk1ToDk(borderDirection.getBorderFrom());
            String borderTo = CountryCodeUtils.mapDk1ToDk(borderDirection.getBorderTo());

            if (isVirtualHubsExchange(virtualHubRecords, borderFrom, borderTo)) {
                final VirtualHubRecord virtualHubRecord = findVirtualHub(borderFrom, borderTo, virtualHubRecords);
                addVirtualHubExchange(virtualHubsExchanges, virtualHubRecord, virtualHubsExchangesFromCgm);
            } else if (isNonCoreExchange(regionConfiguration, borderFrom, borderTo)) {
                final String countryFromEicCode = toEicCode(regionConfiguration, borderFrom);
                final String countryToEicCode = toEicCode(regionConfiguration, borderTo);
                addNonCoreExchange(acExchanges, countryFromEicCode, countryToEicCode, referenceExchangeDataList);
            } else if (isCoreExchange(regionConfiguration, borderFrom, borderTo)) {
                final String countryFromEicCode = toEicCode(regionConfiguration, borderFrom);
                final String countryToEicCode = toEicCode(regionConfiguration, borderTo);
                addCoreExchange(acExchanges, countryFromEicCode, countryToEicCode, regionConfiguration, finalCgmResult, sharingKeysBEC, acNetPositionOutCountry);
            } else {
                throw new CeMergingException("Border direction from " + borderFrom + " to " + borderTo + " defined in VH config is not supported");
            }
        });
    }

    private static String toEicCode(final RegionConfiguration regionConfiguration, final String country) {
        return regionConfiguration.getAreasAll().get(country);
    }

    private static boolean isVirtualHubsExchange(final List<VirtualHubRecord> virtualHubRecords, final String borderFrom, final String borderTo) {
        for (VirtualHubRecord virtualHubRecord : virtualHubRecords) {
            if (virtualHubRecord.getCode().equals(borderFrom) || virtualHubRecord.getCode().equals(borderTo)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isNonCoreExchange(final RegionConfiguration regionConfiguration, final String borderFrom, final String borderTo) {
        final Map<String, String> areasOut = regionConfiguration.getAreasOut();
        return areasOut.containsKey(borderFrom) || areasOut.containsKey(borderTo);
    }

    private static boolean isCoreExchange(final RegionConfiguration regionConfiguration, final String borderFrom, final String borderTo) {
        final Map<String, String> areasIn = regionConfiguration.getAreasIn();
        return areasIn.containsKey(borderFrom) && areasIn.containsKey(borderTo);
    }

    private static void addVirtualHubExchange(final Map<Border, Double> virtualHubsExchanges,
                                              final VirtualHubRecord virtualHubRecord,
                                              final Map<String, Double> virtualHubsExchangesFromCgm) {

        final Border border = new Border(virtualHubRecord.getRelatedMaEic(), virtualHubRecord.getEic());
        final double flow = virtualHubsExchangesFromCgm.getOrDefault(virtualHubRecord.getNodeName(), 0.0);
        virtualHubsExchanges.put(border, flow);
    }

    private static void addNonCoreExchange(final Map<Border, Double> acExchanges,
                                           final String countryFromEicCode,
                                           final String countryToEicCode,
                                           final List<ReferenceExchangeData> referenceExchangeDataList) {

        final Border border = new Border(countryFromEicCode, countryToEicCode);
        final double flow = getExchangeFromRefProg(referenceExchangeDataList, countryFromEicCode, countryToEicCode).getFlow();
        acExchanges.put(border, flow);
    }

    private static void addCoreExchange(final Map<Border, Double> acExchanges,
                                        final String countryFromEicCode,
                                        final String countryToEicCode,
                                        final RegionConfiguration regionConfiguration,
                                        final FinalCgmResult finalCgmResult,
                                        final List<BecByBoundary> sharingKeysBEC,
                                        final Map<String, Double> acNetPositionOutCountry) {

        final Border border = new Border(countryFromEicCode, countryToEicCode);
        double flow = 0.0;

        final Map<String, NetPositions> netPositionsByCountryMap = finalCgmResult.getNetPositionsResults().getNetPositionsByCountryMap();
        final BecByBoundary becByBoundary = getBorderFromSharingKeysBEC(border, sharingKeysBEC);

        for (BecCoefficients becCoefficients : becByBoundary.getCoefficientByCountry()) {
            final String country = becCoefficients.getCountryCode();
            final String countryEicCode = toEicCode(regionConfiguration, country);
            final double coefficient = becCoefficients.getCoefficient();

            final NetPositions netPositions = netPositionsByCountryMap.get(country);
            final double globalWithoutVirtualHubs = netPositions.getGlobalNetPosition().getWithoutVirtualHubs();
            final double acNetPositionOutsideCountry = acNetPositionOutCountry.getOrDefault(countryEicCode, 0.0);

            flow += coefficient * (globalWithoutVirtualHubs - acNetPositionOutsideCountry);
        }

        acExchanges.put(border, flow);
    }

    private static VirtualHubRecord findVirtualHub(final String borderFrom, final String borderTo, final List<VirtualHubRecord> virtualHubRecords) {
        for (VirtualHubRecord virtualHubRecord : virtualHubRecords) {
            if (virtualHubRecord.getCode().equals(borderFrom) || virtualHubRecord.getCode().equals(borderTo)) {
                return virtualHubRecord;
            }
        }
        throw new CeMergingException("Unable to find border from " + borderFrom + " to " + borderTo + " in virtualHubs");
    }

    private static Map<String, Double> getVirtualHubsFromCgm(final FinalCgmResult finalCgmResult) {
        final Map<String, Double> virtualHubsFromCgm = new HashMap<>();

        finalCgmResult.getNetPositionsResults()
                .getNetPositionsByCountryMap()
                .values()
                .forEach(netPositions -> virtualHubsFromCgm.putAll(netPositions.getVirtualHubsExchanges()));

        return virtualHubsFromCgm;
    }

    private static ReferenceExchangeData getExchangeFromRefProg(final List<ReferenceExchangeData> referenceExchangeDataList, final String countryFromEicCode, final String countryToEicCode) {
        return referenceExchangeDataList.stream()
                .filter(referenceExchangeData -> referenceExchangeData.flowsBetween(countryFromEicCode, countryToEicCode))
                .findFirst()
                .orElseThrow(() -> new CeMergingException("Unable to find the exchange from " + countryFromEicCode + " to " + countryToEicCode + " in the reference programm"));
    }

    private static BecByBoundary getBorderFromSharingKeysBEC(final Border border, final List<BecByBoundary> sharingKeysBEC) {
        return sharingKeysBEC.stream()
                .filter(becByBoundary -> border.equals(becByBoundary.getBorder()))
                .findFirst()
                .orElseThrow(() -> new CeMergingException("Unable to find border from " + border.getOutArea() + " to " + border.getInArea() + " in sharingKeysBEC"));
    }

    private static Map<String, Double> getAcNetPositionOutCountry(final RegionConfiguration regionConfiguration, final List<ReferenceExchangeData> referenceExchangeDataList) {
        return regionConfiguration.getAreasIn().values().stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        countryEicCode -> computeAcNetPositionOutCountry(countryEicCode, regionConfiguration, referenceExchangeDataList)
                ));
    }

    private static double computeAcNetPositionOutCountry(final String countryEicCode, final RegionConfiguration regionConfiguration, final List<ReferenceExchangeData> referenceExchangeDataList) {
        return referenceExchangeDataList.stream()
                .filter(referenceExchangeData -> isAcExchangeWithCountry(countryEicCode, regionConfiguration, referenceExchangeData))
                .mapToDouble(referenceExchangeData -> referenceExchangeData.getAreaOutId().equals(countryEicCode)
                        ? referenceExchangeData.getFlow()
                        : -referenceExchangeData.getFlow())
                .sum();
    }

    private static boolean isAcExchangeWithCountry(final String countryEicCode, final RegionConfiguration regionConfiguration, final ReferenceExchangeData referenceExchangeData) {
        final String areaOutId = referenceExchangeData.getAreaOutId();
        final String areaInId = referenceExchangeData.getAreaInId();
        final String regionId = regionConfiguration.getId();
        final Map<String, String> areasAll = regionConfiguration.getAreasAll();

        return areaOutId.equals(countryEicCode) && !regionId.equals(areaInId) && areasAll.containsValue(areaInId) ||
                areaInId.equals(countryEicCode) && !regionId.equals(areaOutId) && areasAll.containsValue(areaOutId);
    }

    private void saveRefProgFileInOutputs(final PublicationDocument finalRefProgResult, final MergingTask taskEntity) {
        final ZonedDateTime targetDateInEuropeZone = taskEntity.getInputs().getTargetDate().atZoneSameInstant(ZoneId.of("Europe/Paris"));
        final String dateAndTime = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm").withLocale(Locale.FRANCE).format(targetDateInEuropeZone);
        final Path filePath = Paths.get(configuration.getOutputsDirectoryPath(taskEntity), dateAndTime + "_CORESO_RefProg.xml");
        JaxbUtils.writeToPath(PublicationDocument.class, finalRefProgResult, filePath);
        final SavedFile refProgSavedFile = new SavedFile("refprog.xml", filePath.toString(), String.format("/tasks/%d/outputs/ref-prog", taskEntity.getId()));
        taskEntity.getOutputs().setRefProg(refProgSavedFile);
        LOGGER.info("Ref prog file '{}' is saved in task '{}' outputs", filePath.getFileName(), taskEntity.getId());
    }
}
