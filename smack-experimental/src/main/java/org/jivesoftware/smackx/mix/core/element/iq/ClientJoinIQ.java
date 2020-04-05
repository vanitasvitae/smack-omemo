package org.jivesoftware.smackx.mix.core.element.iq;

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.mix.core.element.JoinElement;
import org.jivesoftware.smackx.mix.core.element.NickElement;
import org.jivesoftware.smackx.mix.core.element.SubscribeElement;
import org.jivesoftware.smackx.mix.pam.element.ClientJoinElement;

import org.jxmpp.jid.EntityBareJid;

public class ClientJoinIQ extends IQ {

    private final EntityBareJid channelJid;
    private final NickElement nickElement;
    private final List<SubscribeElement> subscriptions;

    public ClientJoinIQ(ClientJoinIQBuilder builder) {
        super(builder, ClientJoinElement.V2.ELEMENT, null);
        this.channelJid = builder.getChannelAddress();
        this.nickElement = builder.getNickElement();
        this.subscriptions = new ArrayList<>(builder.getSubscriptions());
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        JoinElement.V1 joinElement = new JoinElement.V1(subscriptions, nickElement);
        xml.append(new ClientJoinElement.V2(channelJid, joinElement));
        return xml;
    }
}
