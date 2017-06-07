package org.jivesoftware.smackx.jingle_s5b;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;

import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.test.util.TestUtils;
import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream;
import org.jivesoftware.smackx.jingle_s5b.elements.JingleSocks5BytestreamTransport;
import org.jivesoftware.smackx.jingle_s5b.elements.JingleSocks5BytestreamTransportCandidate;
import org.jivesoftware.smackx.jingle_s5b.elements.JingleSocks5BytestreamTransportInfo;
import org.jivesoftware.smackx.jingle_s5b.provider.JingleSocks5BytestreamTransportProvider;
import org.junit.Test;
import org.jxmpp.jid.impl.JidCreate;

/**
 * Test Provider and serialization.
 */
public class JingleSocks5BytestreamTransportTest extends SmackTestSuite {

    @Test
    public void candidatesProviderTest() throws Exception {
        String xml =
                "<transport " +
                        "xmlns='urn:xmpp:jingle:transports:s5b:1' " +
                        "dstaddr='972b7bf47291ca609517f67f86b5081086052dad' " +
                        "mode='tcp' " +
                        "sid='vj3hs98y'>" +

                        "<candidate " +
                        "cid='hft54dqy' " +
                        "host='192.168.4.1' " +
                        "jid='romeo@montague.lit/orchard' " +
                        "port='5086' " +
                        "priority='8257636' " +
                        "type='direct'/>" +

                        "<candidate " +
                        "cid='hutr46fe' " +
                        "host='24.24.24.1' " +
                        "jid='romeo@montague.lit/orchard' " +
                        "port='5087' " +
                        "priority='8258636' " +
                        "type='direct'/>" +

                        "<candidate " +
                        "cid='xmdh4b7i' " +
                        "host='123.456.7.8' " +
                        "jid='streamer.shakespeare.lit' " +
                        "port='7625' " +
                        "priority='7878787' " +
                        "type='proxy'/>" +

                        "</transport>";
        JingleSocks5BytestreamTransport transport = new JingleSocks5BytestreamTransportProvider().parse(TestUtils.getParser(xml));
        assertEquals("972b7bf47291ca609517f67f86b5081086052dad", transport.getDestinationAddress());
        assertEquals("vj3hs98y", transport.getStreamId());
        assertEquals(Bytestream.Mode.tcp, transport.getMode());
        assertEquals(3, transport.getCandidates().size());

        JingleSocks5BytestreamTransportCandidate candidate1 =
                (JingleSocks5BytestreamTransportCandidate) transport.getCandidates().get(0);
        assertEquals("hft54dqy", candidate1.getCandidateId());
        assertEquals("192.168.4.1", candidate1.getHost());
        assertEquals(JidCreate.from("romeo@montague.lit/orchard"), candidate1.getJid());
        assertEquals(5086, candidate1.getPort());
        assertEquals(8257636, candidate1.getPriority());
        assertEquals(JingleSocks5BytestreamTransportCandidate.Type.direct, candidate1.getType());

        JingleSocks5BytestreamTransportCandidate candidate2 =
                (JingleSocks5BytestreamTransportCandidate) transport.getCandidates().get(1);
        assertEquals("hutr46fe", candidate2.getCandidateId());
        assertEquals("24.24.24.1", candidate2.getHost());
        assertEquals(JidCreate.from("romeo@montague.lit/orchard"), candidate2.getJid());
        assertEquals(5087, candidate2.getPort());
        assertEquals(8258636, candidate2.getPriority());
        assertEquals(JingleSocks5BytestreamTransportCandidate.Type.direct, candidate2.getType());

        JingleSocks5BytestreamTransportCandidate candidate3 =
                (JingleSocks5BytestreamTransportCandidate) transport.getCandidates().get(2);
        assertEquals("xmdh4b7i", candidate3.getCandidateId());
        assertEquals("123.456.7.8", candidate3.getHost());
        assertEquals(JidCreate.domainBareFrom("streamer.shakespeare.lit"), candidate3.getJid());
        assertEquals(7625, candidate3.getPort());
        assertEquals(7878787, candidate3.getPriority());
        assertEquals(JingleSocks5BytestreamTransportCandidate.Type.proxy, candidate3.getType());

        assertEquals(xml, transport.toXML().toString());
    }

    @Test
    public void infoProviderTest() throws Exception {
        String candidateError =
                "<transport xmlns='urn:xmpp:jingle:transports:s5b:1' sid='vj3hs98y'>" +
                    "<candidate-error/>" +
                "</transport>";
        JingleSocks5BytestreamTransport candidateErrorTransport = new JingleSocks5BytestreamTransportProvider()
                .parse(TestUtils.getParser(candidateError));
        assertNull(candidateErrorTransport.getDestinationAddress());
        assertEquals(1, candidateErrorTransport.getInfos().size());
        assertEquals("vj3hs98y", candidateErrorTransport.getStreamId());
        assertEquals(JingleSocks5BytestreamTransportInfo.CandidateError(),
                candidateErrorTransport.getInfos().get(0));
        assertEquals(candidateError, candidateErrorTransport.toXML().toString());

        String proxyError =
                "<transport xmlns='urn:xmpp:jingle:transports:s5b:1' sid='vj3hs98y'>" +
                        "<proxy-error/>" +
                        "</transport>";
        JingleSocks5BytestreamTransport proxyErrorTransport = new JingleSocks5BytestreamTransportProvider()
                .parse(TestUtils.getParser(proxyError));
        assertNull(proxyErrorTransport.getDestinationAddress());
        assertEquals(1, proxyErrorTransport.getInfos().size());
        assertEquals("vj3hs98y", proxyErrorTransport.getStreamId());
        assertEquals(JingleSocks5BytestreamTransportInfo.ProxyError(),
                proxyErrorTransport.getInfos().get(0));
        assertEquals(proxyError, proxyErrorTransport.toXML().toString());

        String candidateUsed =
                "<transport xmlns='urn:xmpp:jingle:transports:s5b:1' sid='vj3hs98y'>" +
                        "<candidate-used cid='hr65dqyd'/>" +
                        "</transport>";
        JingleSocks5BytestreamTransport candidateUsedTransport = new JingleSocks5BytestreamTransportProvider()
                .parse(TestUtils.getParser(candidateUsed));
        assertEquals(1, candidateUsedTransport.getInfos().size());
        assertEquals(JingleSocks5BytestreamTransportInfo.CandidateUsed("hr65dqyd"),
                candidateUsedTransport.getInfos().get(0));
        assertEquals("hr65dqyd",
                ((JingleSocks5BytestreamTransportInfo.CandidateUsed)
                        candidateUsedTransport.getInfos().get(0)).getCandidateId());
        assertEquals(candidateUsed, candidateUsedTransport.toXML().toString());

        String candidateActivated =
                "<transport xmlns='urn:xmpp:jingle:transports:s5b:1' sid='vj3hs98y'>" +
                        "<candidate-activated cid='hr65dqyd'/>" +
                        "</transport>";
        JingleSocks5BytestreamTransport candidateActivatedTransport = new JingleSocks5BytestreamTransportProvider()
                .parse(TestUtils.getParser(candidateActivated));
        assertEquals(1, candidateActivatedTransport.getInfos().size());
        assertEquals(JingleSocks5BytestreamTransportInfo.CandidateActivated("hr65dqyd"),
                candidateActivatedTransport.getInfos().get(0));
        assertEquals("hr65dqyd",
                ((JingleSocks5BytestreamTransportInfo.CandidateActivated)
                        candidateActivatedTransport.getInfos().get(0)).getCandidateId());
        assertEquals(candidateActivated, candidateActivatedTransport.toXML().toString());
    }
}
