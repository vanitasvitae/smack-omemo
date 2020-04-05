package org.jivesoftware.smackx.mix.core.element.iq;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.AbstractIqBuilder;
import org.jivesoftware.smack.packet.IqBuilder;
import org.jivesoftware.smackx.mix.core.element.NickElement;
import org.jivesoftware.smackx.mix.core.element.SubscribeElement;

import org.jxmpp.jid.EntityBareJid;

public class ClientJoinIQBuilder extends IqBuilder<ClientJoinIQBuilder, ClientJoinIQ> {

    private EntityBareJid channelAddress;
    private NickElement nickElement;
    private final Set<SubscribeElement> subscriptions = new HashSet<>();

    protected ClientJoinIQBuilder(AbstractIqBuilder<?> other) {
        super(other);
    }

    public ClientJoinIQBuilder(XMPPConnection connection) {
        super(connection);
    }

    protected ClientJoinIQBuilder(String stanzaId) {
        super(stanzaId);
    }

    @Override
    public ClientJoinIQBuilder getThis() {
        return this;
    }

    @Override
    public ClientJoinIQ build() {
        return new ClientJoinIQ(this);
    }

    public ClientJoinIQBuilder setChannelAddress(EntityBareJid channelAddress) {
        this.channelAddress = channelAddress;
        return getThis();
    }

    public ClientJoinIQBuilder addMixNodeSubscription(String nodeName) {
        return addMixNodeSubscription(new SubscribeElement(nodeName));
    }

    public ClientJoinIQBuilder addMixNodeSubscription(SubscribeElement subscription) {
        return addMixNodeSubscriptions(Collections.singleton(subscription));
    }

    public ClientJoinIQBuilder addMixNodeSubscriptions(Collection<SubscribeElement> subscriptions) {
        this.subscriptions.addAll(subscriptions);
        return getThis();
    }

    public ClientJoinIQBuilder setNickname(String nick) {
        this.nickElement = nick != null ? new NickElement(nick) : null;
        return getThis();
    }

    public EntityBareJid getChannelAddress() {
        return channelAddress;
    }

    public NickElement getNickElement() {
        return nickElement;
    }

    public Set<SubscribeElement> getSubscriptions() {
        return subscriptions;
    }
}
