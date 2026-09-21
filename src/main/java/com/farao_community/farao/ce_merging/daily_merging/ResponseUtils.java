/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.ce_merging.daily_merging;

import com.farao_community.farao.ce_merging.common.exception.ServiceIOException;
import com.farao_community.farao.ce_merging.common.util.DateTimeUtils;
import com.farao_community.farao.ce_merging.daily_merging.merging_request.RequestInformation;
import com.farao_community.farao.ce_merging.merging.post_process.common.SchemaLocationNamespace;
import com.farao_community.farao.ce_merging.xsd.daily.response.payload.ResponseItem;
import com.farao_community.farao.ce_merging.xsd.daily.response.payload.ResponseItems;
import com.farao_community.farao.ce_merging.xsd.merging_request.EventMessageType;
import com.farao_community.farao.ce_merging.xsd.merging_request.HeaderType;
import com.farao_community.farao.ce_merging.xsd.merging_request.PayloadType;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.StringWriter;
import java.util.UUID;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.XML_HEADER;
import static com.farao_community.farao.ce_merging.merging.post_process.common.SchemaLocationNamespace.RESPONSE_XSD;
import static jakarta.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT;
import static jakarta.xml.bind.Marshaller.JAXB_FRAGMENT;
import static jakarta.xml.bind.Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION;
import static java.lang.Boolean.TRUE;

public final class ResponseUtils {

    private ResponseUtils() {
        throw new AssertionError("Utility class should not be constructed");
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseUtils.class);

    public static HeaderType fillResponseHeader(final RequestInformation requestInformation, final int version) {
        final HeaderType responseHeader = new HeaderType();
        responseHeader.setVerb("created");
        responseHeader.setNoun(requestInformation.noun());
        responseHeader.setRevision(Integer.toString(version));
        responseHeader.setContext(requestInformation.context());
        responseHeader.setTimestamp(DateTimeUtils.getNowDate());
        responseHeader.setSource(requestInformation.replyAddress());
        responseHeader.setMessageID(String.valueOf(UUID.randomUUID()));
        responseHeader.setCorrelationID(requestInformation.correlationID());
        return responseHeader;
    }

    public static byte[] writeResponseInBytes(EventMessageType responseMessageType) {
        try {
            final Marshaller marshaller = JAXBContext.newInstance(EventMessageType.class).createMarshaller();
            marshaller.setProperty(JAXB_FORMATTED_OUTPUT, TRUE);
            marshaller.setProperty(JAXB_NO_NAMESPACE_SCHEMA_LOCATION, RESPONSE_XSD.getName());
            // remove original header containing "standalone=yes"
            marshaller.setProperty(JAXB_FRAGMENT, true);
            // set a new header without "standalone=yes"
            final StringWriter writer = new StringWriter();
            writer.write(XML_HEADER);
            marshaller.marshal(responseMessageType, writer);
            return writer.toString().getBytes();
        } catch (final JAXBException e) {
            final String errorMessage = String.format("Error occurred when writing content of object of type %s to bytes", EventMessageType.class.getName());
            LOGGER.error(errorMessage);
            throw new ServiceIOException(errorMessage, e);
        }
    }

    public static PayloadType getResponseElement(final ResponseItems responseItems) throws JAXBException, ParserConfigurationException {
        final Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        final Marshaller marshaller = JAXBContext.newInstance(ResponseItems.class, ResponseItem.class).createMarshaller();
        marshaller.setProperty(JAXB_FORMATTED_OUTPUT, TRUE);
        marshaller.setProperty(JAXB_NO_NAMESPACE_SCHEMA_LOCATION, SchemaLocationNamespace.RESPONSE_PAYLOAD_XSD.getName());
        marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, "http://www.w3.org/2001/XMLSchema-instance");
        marshaller.marshal(responseItems, doc);
        PayloadType responsePayload = new PayloadType();
        responsePayload.getAny().add(doc.getDocumentElement());
        return responsePayload;
    }

}
