package org.jivesoftware.smackx.mix.core.element.iq;

import org.jivesoftware.smack.DummyConnection;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smackx.mix.core.MixNodes;

import org.junit.jupiter.api.Test;
import org.jxmpp.jid.impl.JidCreate;

public class ClientJoinIQTest extends SmackTestSuite {

    @Test
    public void serializationTest() {
        ClientJoinIQ iq = new ClientJoinIQBuilder(DummyConnection.newConnectedDummyConnection())
                .ofType(IQ.Type.set)
                .setNickname("alice")
                .setChannelAddress(JidCreate.entityBareFromOrThrowUnchecked("rabbithole@mix.wonderland.lit"))
                .addMixNodeSubscription(MixNodes.NODE_MESSAGES)
                .addMixNodeSubscription(MixNodes.NODE_PRESENCE)
                .build();

        System.out.println(iq.toXML());
    }
}
