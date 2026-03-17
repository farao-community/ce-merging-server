/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.util;

import com.farao_community.farao.ce_merging.common.exception.ServiceIOException;
import com.google.common.io.ByteSource;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import static jakarta.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT;
import static java.lang.Boolean.TRUE;
import static java.nio.file.Files.newInputStream;

public final class JaxbUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(JaxbUtils.class);

    private JaxbUtils() {
        // utility class
    }

    public static <T> T readFromPath(final Class<T> clazz,
                                     final String path) {
        try (final InputStream fileContent = newInputStream(Paths.get(path))) {
            return genericUnmarshaller(clazz)
                .unmarshal(new StreamSource(fileContent), clazz)
                .getValue();
        } catch (final JAXBException | IOException e) {
            final String errorMessage = String.format("Error occurred when converting xml file %s to object of type %s",
                                                      path, clazz.getName());
            LOGGER.error(errorMessage);
            throw new ServiceIOException(errorMessage, e);
        }
    }

    @SuppressWarnings("unchecked") // because giving Class<T> to genericUnmarshaller necessarily produces a T
    public static <T> T readFromBytes(final Class<T> clazz,
                                      final byte[] fileContent) {
        try {
            return (T) genericUnmarshaller(clazz)
                .unmarshal(ByteSource.wrap(fileContent).openStream());
        } catch (final JAXBException | IOException e) {
            final String errorMessage = String.format("Error occurred when converting bytes to object of type %s", clazz.getName());
            LOGGER.error(errorMessage);
            throw new ServiceIOException(errorMessage, e);
        }
    }

    public static <T> byte[] writeToBytes(final Class<T> objectsClass,
                                          final T object) {
        try {
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            marshallerFormatOutput(objectsClass).marshal(object, bos);
            return bos.toByteArray();
        } catch (final JAXBException e) {
            final String errorMessage = String.format("Error occurred when writing content of object of type %s to bytes", objectsClass.getName());
            LOGGER.error(errorMessage);
            throw new ServiceIOException(errorMessage, e);
        }
    }

    public static <T> void writeToPath(final Class<T> objectsClass,
                                       final T object,
                                       final Path filePath) {
        try {
            marshallerFormatOutput(objectsClass).marshal(object, filePath.toFile());
        } catch (final JAXBException e) {
            final String errorMessage = String.format("Error occurred when writing content of object of type %s to path %s", objectsClass.getName(), filePath.toString());
            LOGGER.error(errorMessage);
            throw new ServiceIOException(errorMessage, e);
        }
    }

    // Solution to marshal an object without @XmlRootElement annotation
    public static <T> byte[] writeToBytes(final Class<T> objectsClass,
                                          final T object,
                                          final String nameSpaceURI,
                                          final String rootElement) {
        try {
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            marshallerFormatOutput(objectsClass).marshal(getElementFromRoot(objectsClass,
                                                                            object,
                                                                            nameSpaceURI,
                                                                            rootElement),
                                                         outputStream);
            return outputStream.toByteArray();
        } catch (final JAXBException e) {
            final String errorMessage = String.format("Error occurred when writing content of object of type %s to bytes",
                                                      objectsClass.getName());
            LOGGER.error(errorMessage);
            throw new ServiceIOException(errorMessage, e);
        }
    }

    // Solution to marshal an object without @XmlRootElement annotation
    public static <T> void writeToPath(final Class<T> objectsClass,
                                       final T object,
                                       final String nameSpaceURI,
                                       final String rootElement,
                                       final Path filePath) {
        try {
            marshallerFormatOutput(objectsClass).marshal(getElementFromRoot(objectsClass,
                                                                            object,
                                                                            nameSpaceURI,
                                                                            rootElement),
                                                         filePath.toFile());
        } catch (final JAXBException e) {
            final String errorMessage = String.format("Error occurred when writing content of object of type %s to path %s",
                                                      objectsClass.getName(), filePath.toString());
            LOGGER.error(errorMessage);
            throw new ServiceIOException(errorMessage, e);
        }
    }

    private static <T> JAXBElement<T> getElementFromRoot(final Class<T> objectsClass,
                                                     final T object,
                                                     final String nameSpaceURI,
                                                     final String rootElement) {
        return new JAXBElement<>(new QName(nameSpaceURI, rootElement),
                                 objectsClass,
                                 object);
    }

    private static <T> Marshaller marshallerFormatOutput(final Class<T> clazz) throws JAXBException {
        final Marshaller jaxbMarshaller = JAXBContext.newInstance(clazz).createMarshaller();
        jaxbMarshaller.setProperty(JAXB_FORMATTED_OUTPUT, TRUE);
        return jaxbMarshaller;
    }

    private static <T> Unmarshaller genericUnmarshaller(final Class<T> clazz) throws JAXBException {
        return JAXBContext.newInstance(clazz).createUnmarshaller();
    }

}
