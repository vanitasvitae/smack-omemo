package org.jivesoftware.smackx.jingle3.transport.jingle_s5b;

import java.util.ArrayList;

import org.jivesoftware.smackx.jingle3.adapter.JingleTransportAdapter;
import org.jivesoftware.smackx.jingle3.element.JingleContentTransportCandidateElement;
import org.jivesoftware.smackx.jingle3.element.JingleContentTransportElement;
import org.jivesoftware.smackx.jingle3.transport.jingle_s5b.elements.JingleS5BTransportCandidateElement;
import org.jivesoftware.smackx.jingle3.transport.jingle_s5b.elements.JingleS5BTransportElement;

/**
 * Created by vanitas on 19.07.17.
 */
public class JingleS5BTransportAdapter implements JingleTransportAdapter<JingleS5BTransport> {

    @Override
    public JingleS5BTransport transportFromElement(JingleContentTransportElement element) {
        JingleS5BTransportElement s5b = (JingleS5BTransportElement) element;
        ArrayList<JingleS5BTransportCandidate> candidates = new ArrayList<>();

        for (JingleContentTransportCandidateElement e : element.getCandidates()) {
            candidates.add(JingleS5BTransportCandidate.fromElement((JingleS5BTransportCandidateElement) e));
        }

        return new JingleS5BTransport(s5b.getSid(), s5b.getDestinationAddress(), s5b.getMode(), candidates);
    }

    @Override
    public String getNamespace() {
        return JingleS5BTransport.NAMESPACE;
    }
}
