package org.jivesoftware.smackx.omemo.element;

import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smack.util.stringencoder.Base64;

/**
 * Small class to collect key (byte[]), its id and whether its a preKey or not.
 */
public class OmemoKeyElement implements NamedElement {

    public static final String NAME_KEY = "key";
    public static final String ATTR_RID = "rid";
    public static final String ATTR_PREKEY = "prekey";

    private final byte[] data;
    private final int id;
    private final boolean preKey;

    public OmemoKeyElement(byte[] data, int id) {
        this.data = data;
        this.id = id;
        this.preKey = false;
    }

    public OmemoKeyElement(byte[] data, int id, boolean preKey) {
        this.data = data;
        this.id = id;
        this.preKey = preKey;
    }

    public int getId() {
        return this.id;
    }

    public byte[] getData() {
        return this.data;
    }

    public boolean isPreKey() {
        return this.preKey;
    }

    @Override
    public String toString() {
        return Integer.toString(id);
    }

    @Override
    public String getElementName() {
        return NAME_KEY;
    }

    @Override
    public CharSequence toXML() {
        XmlStringBuilder sb = new XmlStringBuilder(this);

        if (isPreKey()) {
            sb.attribute(ATTR_PREKEY, true);
        }

        sb.attribute(ATTR_RID, getId());
        sb.rightAngleBracket();
        sb.append(Base64.encodeToString(getData()));
        sb.closeElement(this);
        return sb;
    }
}