package org.jivesoftware.smackx.mix.misc.element;

import static org.jivesoftware.smackx.mix.misc.MixMiscConstants.NAMESPACE_MISC_0;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.mix.core.element.NickElement;

/**
 * Element for registering a nick name with the MIX channel.
 *
 * see <a href="https://xmpp.org/extensions/xep-0407.html#usecase-user-register">
 *     XEP-0407: MIX Miscellaneous Capabilities - §3. Registering a Nick</a>
 */
public abstract class RegisterElement implements ExtensionElement {

    public static final String ELEMENT = "register";

    private final NickElement nick;

    /**
     * Create an empty register element.
     * A MIX service must assign a nick name if the request does not contain a nick child element.
     */
    public RegisterElement() {
        this(null);
    }

    /**
     * Create a register element with a desired nick name.
     *
     * @param nickElement nick element containing the nick name.
     */
    public RegisterElement(NickElement nickElement) {
        this.nick = nickElement;
    }

    /**
     * Return the nick child element.
     * For a request, this is the requested nick name and for a response, this is the assigned nick name (might differ).
     *
     * @return nick element
     */
    public NickElement getNick() {
        return nick;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
        return new XmlStringBuilder(this)
                .rightAngleBracket()
                .optAppend(nick)
                .closeElement(this);
    }

    public static class V0 extends RegisterElement {

        public V0() {
            super();
        }

        public V0(NickElement nickElement) {
            super(nickElement);
        }

        @Override
        public String getNamespace() {
            return NAMESPACE_MISC_0;
        }
    }
}
