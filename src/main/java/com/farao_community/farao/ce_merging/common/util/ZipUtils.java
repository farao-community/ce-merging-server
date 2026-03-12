/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.util;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.exception.ServiceIOException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static com.farao_community.farao.ce_merging.common.util.FileUtils.getIfInside;

@Slf4j
@NoArgsConstructor(access = AccessLevel.NONE)
public class ZipUtils {

    private static final int BUFFER_SIZE = 4096;
    private static final String TMP_DIR = "CeMerging";

    /**
     * Extracts a zip file specified by the zipFilePath to a directory specified by
     * destDirectory (will be created if it does not exist)
     *
     * @param zipFilePath   path to the zip file to be extracted
     * @param destDirectory extraction destination diectory
     */
    public static void unzipFile(final Path zipFilePath,
                                 final Path destDirectory) {
        if (!destDirectory.toFile().exists() && !destDirectory.toFile().mkdir()) {
            log.error("Cannot create destination directory '{}'", destDirectory);
            throw new ServiceIOException(String.format("Cannot create destination directory '%s'", destDirectory));
        }
        try (final ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath.toFile()))) { // NOSONAR File location does not come from user input
            ZipEntry entry = zipIn.getNextEntry(); // NOSONAR File location does not come from user input
            // iterates over entries in the zip file
            while (entry != null) {
                final String filePath = getIfInside(entry.getName(),
                                                    destDirectory).toString();
                if (!entry.isDirectory()) {
                    // if the entry is a file, extracts it
                    Files.createDirectories(Paths.get(filePath).getParent());
                    extractFile(zipIn, filePath);
                } else {
                    // if the entry is a directory, make the directory
                    File dir = new File(filePath);
                    dir.mkdir(); // NOSONAR File location does not come from user input
                }
                zipIn.closeEntry();
                entry = zipIn.getNextEntry(); // NOSONAR it is safe to unzip here
            }
        } catch (final IOException e) {
            log.error("Error while extracting file '{}'", zipFilePath.getFileName(), e);
            throw new ServiceIOException(String.format("Error while extracting file '%s'", zipFilePath.getFileName()), e);
        }
    }

    /**
     * Extracts a zip entry (file entry)
     *
     * @param zipIn    zipped input stream
     * @param filePath file to extract
     */
    private static void extractFile(final ZipInputStream zipIn,
                                    final String filePath) throws IOException {
        try (final BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath))) { // NOSONAR File location does not come from user input
            final byte[] bytesIn = new byte[BUFFER_SIZE];
            int read;
            while ((read = zipIn.read(bytesIn)) != -1) {
                bos.write(bytesIn, 0, read);
            }
        }
    }

    public static Path unzipInputFileInTmp(final MultipartFile archives) throws IOException {
        final Path archiveTmpPath = Files.createTempDirectory(TMP_DIR); // NOSONAR directories are used safely here
        final Path extractionPath = Files.createDirectories(archiveTmpPath.resolve("content"));
        final Path inputsArchivePath = storeInputFileInPath(archives, archiveTmpPath);
        try {
            unzipFile(inputsArchivePath, extractionPath);
            FileSystemUtils.deleteRecursively(inputsArchivePath);
            return extractionPath;
        }
        catch (final Exception e) {
            FileSystemUtils.deleteRecursively(inputsArchivePath);
            throw e;
        }

    }

    private static Path storeInputFileInPath(final MultipartFile multipartFile,
                                             final Path path) throws IOException {
        // Use only the filename part, stripping any path components
        final String safeFilename = Paths.get(Optional.ofNullable(multipartFile.getOriginalFilename())
                                                  .orElseThrow(() -> new ServiceIOException("empty filename")))
            .getFileName()
            .toString();
        final Path inputPath = getIfInside(safeFilename, path);
        multipartFile.transferTo(inputPath);
        return inputPath;
    }

    public static byte[] zipDirectory(final String directory) {
        final ByteArrayOutputStream zipBytesStream = new ByteArrayOutputStream();
        try (final ZipOutputStream zipOutputStream = new ZipOutputStream(zipBytesStream);
             final Stream<Path> allFiles = Files.walk(Paths.get(directory))) {

            allFiles.filter(Files::isRegularFile)
                .map(Path::toString)
                .forEach(filePath -> addFileToZip(filePath, directory, zipOutputStream));

        } catch (final IOException e) {
            log.error("Error while compressing directory '{}'", directory, e);
            throw new ServiceIOException(String.format("Error while compressing directory '%s'", directory), e);
        }
        return zipBytesStream.toByteArray();
    }

    private static void addFileToZip(final String filePath,
                                     final String rootDir,
                                     final ZipOutputStream os) {

        final byte[] readBuffer = new byte[2156];
        int bytesIn;
        final String fileRelativePath = Paths.get(rootDir).relativize(Paths.get(filePath)).toString();
        try (final FileInputStream fileStream = new FileInputStream(filePath)) {
            os.putNextEntry(new ZipEntry(fileRelativePath));
            while ((bytesIn = fileStream.read(readBuffer)) != -1) {
                os.write(readBuffer, 0, bytesIn);
            }

        } catch (final IOException e) {
            throw new CeMergingException("Error during output ZIP creation", e);
        }
    }

}
