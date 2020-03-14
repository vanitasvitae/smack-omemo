package org.jivesoftware.smackx.mix.core;

import org.jxmpp.jid.EntityBareJid;

public class MixParticipant {

    private EntityBareJid jid;
    private String nick;

    public MixParticipant(EntityBareJid jid) {
        this(jid, null);
    }

    public MixParticipant(EntityBareJid jid, String nick) {
        this.jid = jid;
        this.nick = nick;
    }

    public EntityBareJid getJid() {
        return jid;
    }

    public String getNick() {
        return nick;
    }
}
