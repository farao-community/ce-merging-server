package com.farao_community.farao.ce_merging.merging.post_process.ref_prog;

import com.farao_community.farao.ce_merging.xsd.ref_prog.PublicationDocument;

import java.math.BigInteger;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.CODING_SCHEME;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.RECEIVER_ID;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.SENDER_ID;

public final class FinalRefProgHelper {

    private FinalRefProgHelper() {

    }

    static PublicationDocument.ReceiverIdentification getReceiverIdentification() {
        final PublicationDocument.ReceiverIdentification receiverIdentification = new PublicationDocument.ReceiverIdentification();
        receiverIdentification.setCodingScheme(CODING_SCHEME);
        receiverIdentification.setV(RECEIVER_ID);
        return receiverIdentification;
    }

    static PublicationDocument.SenderIdentification getSenderIdentification() {
        final PublicationDocument.SenderIdentification senderIdentification = new PublicationDocument.SenderIdentification();
        senderIdentification.setCodingScheme(CODING_SCHEME);
        senderIdentification.setV(SENDER_ID);
        return senderIdentification;
    }

    static PublicationDocument.DocumentIdentification getDocumentIdentification(final String documentIdentification) {
        final PublicationDocument.DocumentIdentification documentIdent = new PublicationDocument.DocumentIdentification();
        documentIdent.setV(documentIdentification);
        return documentIdent;
    }

    static PublicationDocument.DocumentVersion getDocumentVersion() {
        final PublicationDocument.DocumentVersion documentVersion = new PublicationDocument.DocumentVersion();
        documentVersion.setV(BigInteger.valueOf(5));
        return documentVersion;
    }

    static PublicationDocument.PublicationTimeInterval getPublicationTimeInterval(final String dailyTimeInterval) {
        final PublicationDocument.PublicationTimeInterval publicationTimeInterval = new PublicationDocument.PublicationTimeInterval();
        publicationTimeInterval.setV(dailyTimeInterval);
        return publicationTimeInterval;
    }
}
