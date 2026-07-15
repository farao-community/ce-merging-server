/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Set;

public final class CeMergingConstants {

    private CeMergingConstants() {
        // constants class
    }

    // Strings
    public static final String CE_MERGING_URL = "/ce-merging/";
    public static final String API_VERSION = "v1";
    public static final String JSON_API_MIME_TYPE = "application/vnd.api+json";
    public static final String ORIGIN_ANY = "*";
    public static final String MERGING_TASK_ID = "Merging task ID";
    public static final String TASK_NOT_RUN = "Merging task with given ID has not been run";
    public static final String OK = "200";
    public static final String CREATED = "201";
    public static final String BAD_REQUEST = "400";
    public static final String NOT_FOUND = "404";
    public static final String INTERNAL_ERROR = "500";
    public static final String INPUTS_DIR = "inputs";
    public static final String OUTPUTS_DIR = "outputs";
    public static final String DAILY_OUTPUTS_DIR = "daily-outputs";
    public static final String DAILY_INPUTS_DIR = "daily-inputs";
    public static final String ARTIFACTS_DIR = "artifacts";
    public static final String ARTIFACTS_TAG = "Artifacts";
    public static final String OUTPUTS_TAG = "Outputs";
    public static final String MERGING_SUPERVISOR_TAG  = "Merging supervisor";
    public static final String TASK_MANAGEMENT_TAG = "Tasks management";
    public static final String UCTE_FORMAT = "UCTE";
    public static final String DK_COUNTRY_CODE = "D1";
    public static final String DATE_TIME_FORMAT = "yyyyMMdd_HHmm";
    public static final String DK_HVDC_XNODES_PROPERTY = "dk.hvdc.xnodes";
    public static final String DK_NAMING_STRATEGY = "DKNamingStrategy";
    public static final String INPUTS_TAG = "Inputs";
    public static final String GLOBAL_CONFIGURATIONS_TAG = "Global Configurations";
    public static final String TASK_CONFIGURATIONS_TAG = "Task Configurations";
    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String CSV_SEPARATOR = ";";
    public static final String ARROW = "->";
    public static final String TAG_VERSION = "1.0.0";
    public static final Set<String> GERMAN_AND_DANISH_TSO = Set.of("D1", "D2", "D4", "D6", "D7", "D8");
    public static final Set<String> GERMAN_TSO = Set.of("D2", "D4", "D6", "D7", "D8");
    public static final String VIRTUAL_HUB_ALEGRO_BE_NODE_NAME = "XLI_OB1B";
    public static final String VIRTUAL_HUB_ALEGRO_DE_NODE_NAME = "XLI_OB1A";
    public static final String GERMAN_COUNTRY_CODE = "DE";
    public static final String DANISH_TSO = "D1";
    public static final String DENMARK_COUNTRY_CODE = "DK";
    public static final String STRING_FORMAT = "%s";
    public static final String NUMBER_FORMAT = "%d";

    // Numbers
    public static final int DEFAULT_ALEGRO_THRESHOLD = 2000;

    // Date management
    public static final ZoneOffset PARIS_WINTER_OFFSET = ZoneOffset.of("+01:00");
    public static final ZoneId UTC_ZONE_ID = ZoneId.of("UTC");
    public static final ZoneId PARIS_ZONE_ID = ZoneId.of("Europe/Paris");
    public static final DateTimeFormatter FILENAME_DATETIME_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");

}
