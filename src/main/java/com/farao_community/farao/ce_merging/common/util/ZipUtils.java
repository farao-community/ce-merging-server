/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.util;

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
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static java.io.File.separator;

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
        try (final ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath.toFile()))) {// NOSONAR File location does not come from user input
            ZipEntry entry = zipIn.getNextEntry();// NOSONAR it is safe to unzip here
            // iterates over entries in the zip file
            while (entry != null) {
                final String filePath = destDirectory + separator + entry.getName();
                if (!entry.isDirectory()) {
                    // if the entry is a file, extracts it
                    extractFile(zipIn, filePath);
                } else {
                    // if the entry is a directory, make the directory
                    File dir = new File(filePath);
                    dir.mkdir();// NOSONAR File location does not come from user input
                }
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();// NOSONAR it is safe to unzip here
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
        try (final BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath))) {// NOSONAR File location does not come from user input
            final byte[] bytesIn = new byte[BUFFER_SIZE];
            int read;
            while ((read = zipIn.read(bytesIn)) != -1) {
                bos.write(bytesIn, 0, read);
            }
        }
    }

    public static Path unzipInputFileInTmp(final MultipartFile archives) throws IOException {
        final Path archiveTmpPath = Files.createTempDirectory(TMP_DIR);// NOSONAR publicly writable directories are used safely here
        final Path inputsArchivePath = storeInputFileInPath(archives, archiveTmpPath);
        unzipFile(inputsArchivePath, archiveTmpPath);
        FileSystemUtils.deleteRecursively(inputsArchivePath);// NOSONAR File location does not come from user input
        return archiveTmpPath;
    }

    private static Path storeInputFileInPath(final MultipartFile multipartFile,
                                             final Path path) throws IOException {
        final Path inputPath = Paths.get(path.toString(), multipartFile.getOriginalFilename());
        multipartFile.transferTo(inputPath);// NOSONAR File location does not come from user input
        return inputPath;
    }

    public static byte[] zipDirectory(final String directory) {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        try (final ZipOutputStream zos = new ZipOutputStream(os)) {
            recursiveZip(directory, zos, directory);
        } catch (final IOException e) {
            log.error("Error while compressing directory '{}'", directory, e);
            throw new ServiceIOException(String.format("Error while compressing directory '%s'", directory), e);
        }
        return os.toByteArray();
    }

    private static void recursiveZip(final String directory,
                                     final ZipOutputStream outputStream,
                                     final String referencePath) {

        //create a new File object based on the directory we have to zip
        final File dirAsZip = new File(directory);
        //get a listing of the directory content
        final String[] children = dirAsZip.list();
        final byte[] readBuffer = new byte[2156];
        int bytesIn;
        //loop through children, and zip the files
        for (final String fileOrDir : children) {
            final File child = new File(dirAsZip, fileOrDir);
            final String path = child.getPath();
            if (child.isDirectory()) {
                //if the File object is a directory, call this
                //function again to add its content recursively
                recursiveZip(path, outputStream, referencePath);
                //loop again (go to next file/dir)
                continue;
            }
            //if we reached here, the File object child was not a directory
            //create a FileInputStream on top of child
            try (final FileInputStream inputStream = new FileInputStream(child)) {
                final String fileRelativePath = Paths.get(referencePath).relativize(Paths.get(path)).toString();
                //create a new zip entry and place it in the ZipOutputStream object
                outputStream.putNextEntry(new ZipEntry(fileRelativePath));
                //now write the content of the file to the ZipOutputStream
                while ((bytesIn = inputStream.read(readBuffer)) != -1) {
                    outputStream.write(readBuffer, 0, bytesIn);
                }
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
