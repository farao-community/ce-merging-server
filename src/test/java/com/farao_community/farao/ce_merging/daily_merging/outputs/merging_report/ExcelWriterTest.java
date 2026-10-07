/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.daily_merging.outputs.merging_report;

import com.farao_community.farao.ce_merging.daily_merging.outputs.merging_report.sheets.FilesSheet;
import com.farao_community.farao.ce_merging.daily_merging.outputs.merging_report.sheets.MergeSheet;
import com.farao_community.farao.ce_merging.daily_merging.outputs.merging_report.sheets.XNodeInconsistenciesSheet;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExcelWriterTest {

    @TempDir
    Path tempDirectory;

    @Test
    void shouldExportInputsToSpreadsheet() throws IOException {
        final MergeSheet mergeSheet = new MergeSheet("20260101", "3", "AC", 9, 4.8, "none", "auto");
        final XNodeInconsistenciesSheet xNodeInconsistenciesSheet = new XNodeInconsistenciesSheet("20260101", "3", "XAB_1234", "BE", "CLOSED", "FR", "OPEN", "OPEN", "auto");
        final FilesSheet filesSheet = new FilesSheet("20260101", "3", "D2CF", "DACF", "D2CF");

        final Path output = tempDirectory.resolve("merging-report.xlsx");
        ExcelWriter.export(output.toString(), List.of(mergeSheet), List.of(xNodeInconsistenciesSheet), List.of(filesSheet));

        try (var workbook = WorkbookFactory.create(output.toFile())) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(3);

            assertThat(workbook.getSheetName(0)).isEqualTo("Merge");
            assertThat(workbook.getSheetName(1)).isEqualTo("XNode Inconsistencies");
            assertThat(workbook.getSheetName(2)).isEqualTo("Files");

            final Sheet merge = workbook.getSheet("Merge");
            final Sheet xnodesInc = workbook.getSheet("XNode Inconsistencies");
            final Sheet files = workbook.getSheet("Files");

            assertCellText(merge, "BD", 0, 0);
            assertCellText(merge, "20260101", 1, 0);
            assertCellNumber(merge, 9.0, 1, 3);
            assertCellNumber(merge, 4.8, 1, 4);

            assertCellText(xnodesInc, "BD", 0, 0);
            assertCellText(xnodesInc, "XAB_1234", 1, 2);
            assertCellText(xnodesInc, "auto", 1, 8);

            assertCellText(files, "BD", 0, 0);
            assertCellText(files, "D2CF", 1, 2);
            assertCellText(files, "D2CF", 1, 4);
        }
    }

    static void assertCellText(final Sheet sheet,
                               final String expected,
                               final int rowNum,
                               final int cellNum) {
        assertThat(sheet.getRow(rowNum).getCell(cellNum).getStringCellValue()).isEqualTo(expected);
    }

    static void assertCellNumber(final Sheet sheet,
                                 final Number expected,
                                 final int rowNum,
                                 final int cellNum) {
        assertThat(sheet.getRow(rowNum).getCell(cellNum).getNumericCellValue()).isEqualTo(expected);
    }
}
