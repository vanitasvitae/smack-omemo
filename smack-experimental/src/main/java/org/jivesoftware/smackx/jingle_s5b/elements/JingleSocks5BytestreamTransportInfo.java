package org.jivesoftware.smackx.jingle_s5b.elements;

import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.jingle.element.JingleContentTransportInfo;

/**
 * Class representing possible SOCKS5 TransportInfo elements.
 */
public abstract class JingleSocks5BytestreamTransportInfo extends JingleContentTransportInfo {

    private static CandidateError CEI;
    private static ProxyError PEI;

    public static CandidateUsed CandidateUsed(String candidateId) {
        return new CandidateUsed(candidateId);
    }

    public static CandidateActivated CandidateActivated(String candidateId) {
        return new CandidateActivated(candidateId);
    }

    public static CandidateError CandidateError() {
        if (CEI == null) {
            CEI = new CandidateError();
        }
        return CEI;
    }

    public static ProxyError ProxyError() {
        if (PEI == null) {
            PEI = new ProxyError();
        }
        return PEI;
    }

    public static class CandidateActivated extends JingleSocks5BytestreamTransportInfo {
        public static final String ELEMENT = "candidate-activated";
        public static final String ATTR_CID = "cid";

        private final String candidateId;

        public CandidateActivated(String candidateId) {
            this.candidateId = candidateId;
        }

        public String getCandidateId() {
            return candidateId;
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        @Override
        public CharSequence toXML() {
            XmlStringBuilder xml = new XmlStringBuilder();
            xml.halfOpenElement(this);
            xml.attribute(ATTR_CID, candidateId);
            xml.closeEmptyElement();
            return xml;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof CandidateActivated &&
                    ((CandidateActivated) other).getCandidateId().equals(candidateId);
        }

        @Override
        public int hashCode() {
            return toXML().toString().hashCode();
        }
    }

    public static class CandidateUsed extends JingleSocks5BytestreamTransportInfo {
        public static final String ELEMENT = "candidate-used";
        public static final String ATTR_CID = "cid";

        private final String candidateId;

        public CandidateUsed(String candidateId) {
            this.candidateId = candidateId;
        }

        public String getCandidateId() {
            return candidateId;
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        @Override
        public CharSequence toXML() {
            XmlStringBuilder xml = new XmlStringBuilder();
            xml.halfOpenElement(this);
            xml.attribute(ATTR_CID, candidateId);
            xml.closeEmptyElement();
            return xml;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof CandidateUsed &&
                    ((CandidateUsed) other).getCandidateId().equals(candidateId);
        }

        @Override
        public int hashCode() {
            return toXML().toString().hashCode();
        }
    }

    public static class CandidateError extends JingleSocks5BytestreamTransportInfo {
        public static final String ELEMENT = "candidate-error";

        private CandidateError() {

        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        @Override
        public CharSequence toXML() {
            XmlStringBuilder xml = new XmlStringBuilder();
            xml.halfOpenElement(this);
            xml.closeEmptyElement();
            return xml;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof CandidateError;
        }

        @Override
        public int hashCode() {
            return toXML().toString().hashCode();
        }
    }

    public static class ProxyError extends JingleSocks5BytestreamTransportInfo {
        public static final String ELEMENT = "proxy-error";

        private ProxyError() {

        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        @Override
        public CharSequence toXML() {
            XmlStringBuilder xml = new XmlStringBuilder();
            xml.halfOpenElement(this);
            xml.closeEmptyElement();
            return xml;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof ProxyError;
        }

        @Override
        public int hashCode() {
            return toXML().toString().hashCode();
        }
    }
}
