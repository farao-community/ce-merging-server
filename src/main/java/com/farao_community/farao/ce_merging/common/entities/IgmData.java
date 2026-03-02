/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.entities;

import com.farao_community.farao.ce_merging.common.entities.enums.IgmType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.OneToOne;
import lombok.Data;

import java.io.Serializable;
import java.nio.file.Paths;
import java.util.InputMismatchException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static jakarta.persistence.CascadeType.ALL;

@Embeddable
@Data
public class IgmData implements Serializable {

    // UCTE filename convention <yyyymmdd>_<HHMM>_<TY><w>_<cc><v>.uct
    // UCTE filename convention <yyyymmdd>_<B2MM>_<TY><w>_<cc><v>.uct for the second hour in case of long clock change
    private static final String UCTE_FILENAME_REGEX = "^(?<year>\\d{4})(?<month>\\d{2})(?<day>\\d{2})_(?<hour>\\d{2}|B2)(?<minutes>\\d{2})_(?<type>FO|2D|SN|RE|LR)(?<dayOfWeek>\\d)_(?<countryCode>[A-Z0-9]{2})(?<version>\\d)\\.(uct|UCT)$";
    private static final Pattern UCTE_PATTERN = Pattern.compile(UCTE_FILENAME_REGEX);
    private static final String NAMING_ERROR = "File '%s' does not match UCTE file naming convention";
    /**
     * The country of the IGM
     */
    private String country;
    /**
     * The type of the IGM
     */
    private IgmType type;
    /**
     * The saved IGM
     */
    @OneToOne(cascade = ALL)
    private SavedFile igmFile = new SavedFile();
    /**
     * The saved IGM quality report
     */
    @OneToOne(cascade = ALL)
    private SavedFile igmQualityReportFile = new SavedFile();

    public void setIgmFilePath(final String igmFilePath) {
        final String fileName = Paths.get(igmFilePath).getFileName().toString();
        checkUcteNameConvention(fileName);
        igmFile.setOriginalName(fileName);
        igmFile.setPath(igmFilePath);
    }

    private void checkUcteNameConvention(final String igmFilePath) {
        final Matcher ucteNaming = UCTE_PATTERN.matcher(igmFilePath);
        if (!ucteNaming.matches()) {
            throw new InputMismatchException(String.format(NAMING_ERROR, igmFilePath));
        }
        country = ucteNaming.group("countryCode");
        type = IgmType.fromTypeCode(ucteNaming.group("type"));
    }

    public String getIgmQualityReportFileLocation() {
        return igmQualityReportFile.getLocation();
    }

    public void setIgmQualityReportFilePath(final String igmQualityReportFilePath) {
        igmQualityReportFile.feedPathAndName(igmQualityReportFilePath);
    }
}
