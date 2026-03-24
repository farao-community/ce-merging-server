/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.util;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.exception.ServiceIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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

import static com.farao_community.farao.ce_merging.common.util.ExceptionUtils.logAndThrow;
import static com.farao_community.farao.ce_merging.common.util.FileUtils.getIfInside;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.createTempDirectory;
import static org.springframework.util.FileSystemUtils.deleteRecursively;

public final class ZipUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZipUtils.class);
    private static final int WRITE_BUFFER_SIZE = 4096;
    private static final int READ_BUFFER_SIZE = 2156;
    private static final String TMP_DIR = "CeMerging";

    private ZipUtils() {
        // utility class
    }

    /**
     * Extracts a zip file specified by the zipFilePath to a directory specified by
     * destDirectory (will be created if it does not exist)
     *
     * @param zipFilePath   path to the zip file to be extracted
     * @param destDirectory extraction destination directory
     */
    public static void unzipFile(final Path zipFilePath,
                                 final Path destDirectory) {
        final File destination = destDirectory.toFile();
        if (!destination.exists() && !destination.mkdir()) {
            logAndThrow(null, "Cannot create destination directory '%s'", destDirectory);
        }

        try (final ZipInputStream zipIn = getZipStream(zipFilePath)) { //NOSONAR expanding archive is safe
            ZipEntry zipEntry;
            // iterates over entries in the zip file
            while ((zipEntry = zipIn.getNextEntry()) != null) { // NOSONAR expanding archive is safe
                final Path filePath = getIfInside(zipEntry.getName(), destDirectory);
                final String fileDir = filePath.toString();
                if (zipEntry.isDirectory()) {
                    // if the entry is a directory, create it if it doesn't already exist
                    final File dir = new File(fileDir);
                    if (!dir.exists() && !dir.mkdir()) {
                        throw new IOException("Could not create folder %s".formatted(fileDir));
                    }
                } else {
                    // if the entry is a file, extracts it
                    createDirectories(filePath.getParent());
                    extractFile(zipIn, fileDir);
                }
                zipIn.closeEntry();
            }
        } catch (final IOException e) {
            logAndThrow(e, "extracting file '%s'", zipFilePath.getFileName());
        }
    }

    private static ZipInputStream getZipStream(final Path zipFilePath) throws FileNotFoundException {
        return new ZipInputStream(new FileInputStream(zipFilePath.normalize().toFile()));
    }

    /**
     * Extracts a zip entry (file entry)
     *
     * @param zipIn    zipped input stream
     * @param filePath file to extract
     */
    private static void extractFile(final ZipInputStream zipIn,
                                    final String filePath) throws IOException {
        try (final BufferedOutputStream outFile = new BufferedOutputStream(new FileOutputStream(filePath))) { // NOSONAR File location does not come from user input
            final byte[] bytesIn = new byte[WRITE_BUFFER_SIZE];
            int read;
            while ((read = zipIn.read(bytesIn)) != -1) {
                outFile.write(bytesIn, 0, read);
            }
        }
    }

    public static Path unzipInputFileInTmp(final MultipartFile zipFile) throws IOException {
        final Path archiveTmpPath = createTempDirectory(TMP_DIR); // NOSONAR directories are used safely here
        final Path inputsArchivePath = storeInputFileInPath(zipFile, archiveTmpPath);
        unzipFile(inputsArchivePath, archiveTmpPath);
        deleteRecursively(inputsArchivePath);
        return archiveTmpPath;
    }

    private static Path storeInputFileInPath(final MultipartFile multipartFile,
                                             final Path inputsPath) throws IOException {
        // Use only the filename part, stripping any path components
        final String safeFilename = Paths.get(Optional.ofNullable(multipartFile.getOriginalFilename())
                                                  .orElseThrow(() -> new ServiceIOException("empty filename")))
            .getFileName()
            .toString();
        final Path inputPath = getIfInside(safeFilename, inputsPath);
        multipartFile.transferTo(inputPath);
        return inputPath;
    }

    public static byte[] zipDirectory(final String directory) {
        final ByteArrayOutputStream zipBytesStream = new ByteArrayOutputStream();
        try (final ZipOutputStream zipOutputStream = new ZipOutputStream(zipBytesStream);
             final Stream<Path> allFiles = Files.walk(Paths.get(directory))) {

            allFiles.filter(Files::isRegularFile)
                .forEach(filePath -> addFileToZip(filePath, directory, zipOutputStream));

        } catch (final IOException e) {
            logAndThrow(e, "compressing directory %s", directory);
        }
        return zipBytesStream.toByteArray();
    }

    static void addFileToZip(final Path filePath,
                             final String rootDir,
                             final ZipOutputStream outFile) {

        final byte[] readBuffer = new byte[READ_BUFFER_SIZE];
        int bytesIn;
        final String fileRelativePath = Paths.get(rootDir).relativize(filePath).toString();

        try (final FileInputStream inStream = new FileInputStream(filePath.toFile())) {
            outFile.putNextEntry(new ZipEntry(fileRelativePath));
            // write while there are still bytes left
            while ((bytesIn = inStream.read(readBuffer)) != -1) {
                outFile.write(readBuffer, 0, bytesIn);
            }
            outFile.closeEntry();
        } catch (final IOException e) {
            throw new CeMergingException("Error during output ZIP creation", e);
        }
    }

}
