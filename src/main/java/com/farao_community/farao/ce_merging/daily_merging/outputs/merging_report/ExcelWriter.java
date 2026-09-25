/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.daily_merging.outputs.merging_report;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.daily_merging.outputs.merging_report.sheets.ColumnsHeader;
import com.farao_community.farao.ce_merging.daily_merging.outputs.merging_report.sheets.FilesSheet;
import com.farao_community.farao.ce_merging.daily_merging.outputs.merging_report.sheets.MergeSheet;
import com.farao_community.farao.ce_merging.daily_merging.outputs.merging_report.sheets.XNodeInconsistenciesSheet;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public final class ExcelWriter {

    private ExcelWriter() {
        // utility class
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ExcelWriter.class);

    public static void export(final String filePath,
                              final List<MergeSheet> mergeSheets,
                              final List<XNodeInconsistenciesSheet> xNodeInconsistenciesSheets,
                              final List<FilesSheet> filesSheets) {
        writeToSheet(filePath, "Merge", mergeSheets);
        writeToSheet(filePath, "XNode Inconsistencies", xNodeInconsistenciesSheets);
        writeToSheet(filePath, "Files", filesSheets);
    }

    private static <T extends ColumnsHeader> void writeToSheet(final String filePath,
                                                               final String sheetName,
                                                               final List<T> data) {

        final File file = new File(filePath);
        try (final XSSFWorkbook workbook = createWorkbook(file);
             final OutputStream outputStream = new FileOutputStream(file)) {

            final Sheet sheet = workbook.createSheet(sheetName);

            final T first = data.getFirst();
            final Class<?> clazz = first.getClass();

            final List<String> fieldNames = Arrays.stream(clazz.getDeclaredFields())
                    .map(Field::getName)
                    .toList();
            final List<String> columnNames = first.getFieldNames();

            int rowCount = setSheetColumnNames(sheet, columnNames);
            for (final T rowValues : data) {
                Row row = sheet.createRow(rowCount++);
                writeCellValues(fieldNames, row, clazz, rowValues);
            }

            workbook.write(outputStream);
            outputStream.flush();

        } catch (final Exception e) {
            LOGGER.error("Cannot write merging report file '{}' ", e.getMessage());
            throw new CeMergingException("Cannot write merging report file '" + filePath + "'", e);

        }
    }

    private static XSSFWorkbook createWorkbook(final File file) throws IOException {
        return file.exists() ? (XSSFWorkbook) WorkbookFactory.create(new FileInputStream(file))
                : new XSSFWorkbook();
    }

    private static int setSheetColumnNames(final Sheet sheet,
                                           final List<String> columnNames) {
        int count = 0;
        final Row row = sheet.createRow(count++);
        int columnCount = 0;
        for (final String columnName : columnNames) {
            final Cell cell = row.createCell(columnCount++);
            cell.setCellValue(columnName);
        }
        return count;
    }

    private static <T extends ColumnsHeader> void writeCellValues(final List<String> fieldNames,
                                                                  final Row row,
                                                                  final Class<?> clazz,
                                                                  final T rowValues) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        int columnCount = 0;
        for (final String fieldName : fieldNames) {
            final Cell cell = row.createCell(columnCount);
            // because these are records, no getXXX
            final Method getter = clazz.getMethod(fieldName);
            final Object cellValue = getter.invoke(rowValues, (Object[]) null);
            if (cellValue != null) {
                switch (cellValue) {
                    case String s -> cell.setCellValue(s);
                    case Long l -> cell.setCellValue(l);
                    case Integer i -> cell.setCellValue(i);
                    case Double v -> cell.setCellValue(v);
                    default -> {
                    }
                }
            }
            columnCount++;
        }
    }
}
