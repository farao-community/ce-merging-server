/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 */
package com.farao_community.farao.ce_merging.common.util.serialization;

import com.farao_community.farao.ce_merging.common.exception.ServiceIOException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;

@Slf4j
@NoArgsConstructor(access = AccessLevel.NONE)
public final class JsonUtils {

    public static <T> T read(final Class<T> clazz, final String path) {
        try (final InputStream inputStream = new FileInputStream(path)) {
            return new ObjectMapper().enable(INDENT_OUTPUT).readValue(inputStream, clazz);
        } catch (final IOException e) {
            final String errorMessage = String.format("Error occurred when converting Json file %s to object of type %s",
                                                path,
                                                clazz.getName());
            log.error(errorMessage);
            throw new ServiceIOException(errorMessage, e);
        }
    }

    public static <T> T read(final Class<T> clazz, final InputStream inputStream) {
        try (inputStream) {
            return new ObjectMapper().enable(INDENT_OUTPUT).readValue(inputStream, clazz);
        } catch (final IOException e) {
            final String errorMessage = String.format("Error occurred when converting Json file to object of type %s",
                                                      clazz.getName());
            log.error(errorMessage);
            throw new ServiceIOException(errorMessage, e);
        }
    }

    public static <T> T read(final Class<T> clazz, final MultipartFile file) {
        try (final ByteArrayInputStream inputStream = new ByteArrayInputStream(file.getBytes())) {
            return new ObjectMapper().enable(INDENT_OUTPUT).readValue(inputStream, clazz);
        } catch (final IOException e) {
            final String errorMessage = String.format("Error occurred when converting Json file to object of type %s",
                                                      clazz.getName());
            log.error(errorMessage);
            throw new ServiceIOException(errorMessage, e);
        }
    }

    public static <T> byte[] writeInBytes(final Class<T> clazz, final T object) {
        try (final ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            new ObjectMapper().enable(INDENT_OUTPUT).writeValue(outputStream, object);
            return outputStream.toByteArray();
        } catch (final IOException e) {
            final String errorMessage = String.format("Error occurred when writing content of object of type %s to bytes",
                                                      clazz.getName());
            log.error(errorMessage);
            throw new ServiceIOException(errorMessage, e);
        }
    }

    public static <T> void writeInPath(final Class<T> clazz, final T object, final Path filePath) {
        try (final OutputStream outputStream = Files.newOutputStream(filePath)) {
            new ObjectMapper()
                .enable(INDENT_OUTPUT)
                .writeValue(outputStream, object);
        } catch (final IOException e) {
            final String errorMessage = String.format("Error occurred when writing content of object of type %s to path %s",
                                                      clazz.getName(),
                                                      filePath);
            log.error(errorMessage);
            throw new ServiceIOException(errorMessage, e);
        }
    }

}

