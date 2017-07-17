package org.jivesoftware.smackx.jingle3;

import org.jivesoftware.smackx.jingle.element.JingleContent;
import org.jivesoftware.smackx.jingle.element.JingleContentDescription;
import org.jivesoftware.smackx.jingle.element.JingleContentSecurity;
import org.jivesoftware.smackx.jingle.element.JingleContentTransport;

/**
 * Internal class that holds the state of a content in a modifiable form.
 */
public class JingleSessionContent {
    private JingleContent.Creator creator;
    private String name;
    private JingleContent.Senders senders;
    private JingleContentDescription description;
    private JingleContentTransport transport;
    private JingleContentSecurity security;

    public JingleContent.Creator getCreator() {
        return creator;
    }

    public String getName() {
        return name;
    }

    public JingleContent.Senders getSenders() {
        return senders;
    }

    public JingleContentDescription getDescription() {
        return description;
    }

    public JingleContentTransport getTransport() {
        return transport;
    }

    public JingleContentSecurity getSecurity() {
        return security;
    }
}
