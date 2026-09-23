/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.ce_merging.daily_merging.outputs.common;

import com.farao_community.farao.ce_merging.common.util.DateTimeUtils;
import com.farao_community.farao.ce_merging.daily_merging.merging_request.RequestInformation;
import com.farao_community.farao.ce_merging.xsd.merging_response.ResponseItem;
import com.farao_community.farao.ce_merging.xsd.merging_response.ResponseItems;
import com.farao_community.farao.ce_merging.xsd.merging_request.HeaderType;
import com.farao_community.farao.ce_merging.xsd.merging_request.PayloadType;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.UUID;

import static jakarta.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT;
import static jakarta.xml.bind.Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION;
import static java.lang.Boolean.TRUE;

public final class ResponseUtils {

    private ResponseUtils() {
        throw new AssertionError("Utility class should not be constructed");
    }

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
