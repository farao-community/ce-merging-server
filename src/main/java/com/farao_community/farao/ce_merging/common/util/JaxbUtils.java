/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.util;

import com.google.common.io.ByteSource;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.farao_community.farao.ce_merging.common.exception.ServiceIOException.errorWhile;
import static jakarta.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT;
import static java.lang.Boolean.TRUE;
import static java.nio.file.Files.newInputStream;

public final class JaxbUtils {

    private JaxbUtils() {
        // utility class
    }

    /**
     *
     * @param clazz the Class object representing T
     * @param <T>   the class of the object
     * @param path  the path of the file to read from
     * @return an object from an XML file path
     */
    public static <T> T readFromPath(final Class<T> clazz,
                                     final String path) {
        try (final InputStream fileContent = newInputStream(Paths.get(path))) {
            return unmarshaller(clazz)
                .unmarshal(new StreamSource(fileContent), clazz)
                .getValue();
        } catch (final Exception e) {
            throw errorWhile(e, "converting XML file %s to a %s object", path, clazz.getSimpleName());
        }
    }

    /**
     *
     * @param clazz       the Class object representing T
     * @param <T>         the class of the object
     * @param fileContent the byte array to read from
     * @return an object from XML byte content
     */
    public static <T> T readFromBytes(final Class<T> clazz,
                                      final byte[] fileContent) {
        try (InputStream inputStream = bytesToStream(fileContent)) {
            return unmarshaller(clazz)
                .unmarshal(new StreamSource(inputStream), clazz)
                .getValue();

        } catch (final Exception e) {
            throw errorWhile(e, "converting bytes to a %s object", clazz.getSimpleName());
        }
    }

    /**
     *
     * @param clazz  the Class object representing T
     * @param <T>    the class of the object
     * @param object object to read
     * @return a byte array of the file content
     */
    public static <T> byte[] writeToBytes(final Class<T> clazz,
                                          final T object) {
        try {
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            marshaller(clazz).marshal(object, bos);
            return bos.toByteArray();
        } catch (final Exception e) {
            throw errorWhile(e, "writing a %s object to bytes", clazz.getSimpleName());
        }
    }

    /**
     *
     * @param clazz    the Class object representing T
     * @param object   object to write
     * @param filePath path to write to
     * @param <T>      the class of the object
     */
    public static <T> void writeToPath(final Class<T> clazz,
                                       final T object,
                                       final Path filePath) {
        try {
            marshaller(clazz).marshal(object, filePath.toFile());
        } catch (final Exception e) {
            throw errorWhile(e, "writing a %s object to %s",
                             clazz.getSimpleName(),
                             filePath);
        }
    }

    /**
     * Solution to marshal an object without @XmlRootElement annotation
     *
     * @param clazz        the Class object representing T
     * @param <T>          the class of the object
     * @param object       object to read
     * @param nameSpaceURI given so that JAXB is able to marshal
     * @param rootElement  given so that JAXB is able to marshal
     * @return a byte array of the file content
     */
    public static <T> byte[] writeToBytes(final Class<T> clazz,
                                          final T object,
                                          final String nameSpaceURI,
                                          final String rootElement) {
        try {
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            marshaller(clazz).marshal(withSpecifiedRoot(clazz,
                                                        object,
                                                        nameSpaceURI,
                                                        rootElement),
                                      outputStream);
            return outputStream.toByteArray();
        } catch (final Exception e) {
            throw errorWhile(e, "writing a %s object to bytes", clazz.getSimpleName());
        }
    }

    /**
     * Solution to marshal an object without @XmlRootElement annotation
     *
     * @param filePath     the path to write to
     * @param clazz        the Class object representing T
     * @param <T>          the class of the object
     * @param object       object to write
     * @param nameSpaceURI given so that JAXB is able to marshal
     * @param rootElement  given so that JAXB is able to marshal
     */
    public static <T> void writeToPath(final Class<T> clazz,
                                       final T object,
                                       final String nameSpaceURI,
                                       final String rootElement,
                                       final Path filePath) {
        try {
            marshaller(clazz).marshal(withSpecifiedRoot(clazz,
                                                        object,
                                                        nameSpaceURI,
                                                        rootElement),
                                      filePath.toFile());
        } catch (final Exception e) {
            throw errorWhile(e, "writing a %s object to %s",
                             clazz.getSimpleName(),
                             filePath);
        }
    }

    /**
     *
     * @param clazz        the Class object representing T
     * @param <T>          the class of the object to read
     * @param nameSpaceURI given so that JAXB is able to marshal
     * @param rootElement  given so that JAXB is able to marshal
     * @return the root element as a JAXB element
     */
    private static <T> JAXBElement<T> withSpecifiedRoot(final Class<T> clazz,
                                                        final T object,
                                                        final String nameSpaceURI,
                                                        final String rootElement) {
        return new JAXBElement<>(new QName(nameSpaceURI, rootElement),
                                 clazz,
                                 object);
    }

    /**
     * @param clazz the Class object representing T
     * @param <T>   the class of the object to write
     * @return a marshaller with JAXB_FORMATTED_OUTPUT enabled
     * @throws JAXBException if there's a problem with the JAXB context
     */
    private static <T> Marshaller marshaller(final Class<T> clazz) throws JAXBException {
        final Marshaller jaxbMarshaller = JAXBContext.newInstance(clazz).createMarshaller();
        jaxbMarshaller.setProperty(JAXB_FORMATTED_OUTPUT, TRUE);
        return jaxbMarshaller;
    }

    /**
     * @param clazz the Class object representing T
     * @param <T>   the class of the object to read
     * @return a standard unmarshaller
     * @throws JAXBException if there's a problem with the JAXB context
     */
    private static <T> Unmarshaller unmarshaller(final Class<T> clazz) throws JAXBException {
        return JAXBContext.newInstance(clazz).createUnmarshaller();
    }

    /**
     * @param bytes byte array to be converted
     * @return bytes as a stream
     * @throws IOException if stream won't open
     */
    private static InputStream bytesToStream(final byte[] bytes) throws IOException {
        return ByteSource.wrap(bytes).openStream();
    }

}
