package org.jivesoftware.smackx.jingle_filetransfer.element;

import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.hash.element.HashElement;

/**
 * RangeElement which specifies, which range of a file shall be transferred.
 */
public class Range implements NamedElement {

    public static final String ELEMENT = "range";
    public static final String ATTR_OFFSET = "offset";
    public static final String ATTR_LENGTH = "length";

    private final int offset, length;
    private final HashElement hash;

    /**
     * Create a Range element with default values.
     */
    public Range() {
        this(0, -1, null);
    }

    /**
     * Create a Range element with specified length.
     * @param length length of the transmitted data in bytes.
     */
    public Range(int length) {
        this(0, length, null);
    }

    /**
     * Create a Range element with specified offset and length.
     * @param offset offset in bytes from the beginning of the transmitted data.
     * @param length number of bytes that shall be transferred.
     */
    public Range(int offset, int length) {
        this(offset, length, null);
    }

    /**
     * Create a Range element with specified offset, length and hash.
     * @param offset offset in bytes from the beginning of the transmitted data.
     * @param length number of bytes that shall be transferred.
     * @param hash hash of the bytes in the specified range.
     */
    public Range(int offset, int length, HashElement hash) {
        this.offset = offset;
        this.length = length;
        this.hash = hash;
    }

    /**
     * Return the index of the offset.
     * This marks the begin of the specified range.
     * @return offset
     */
    public int getOffset() {
        return offset;
    }

    /**
     * Return the length of the range.
     * @return length
     */
    public int getLength() {
        return length;
    }

    /**
     * Return the hash element that contains a checksum of the bytes specified in the range.
     * @return hash element
     */
    public HashElement getHash() {
        return hash;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public CharSequence toXML() {
        XmlStringBuilder sb =  new XmlStringBuilder(this);

        if (offset > 0) {
            sb.attribute(ATTR_OFFSET, offset);
        }
        if (length > 0) {
            sb.attribute(ATTR_LENGTH, length);
        }

        if (hash != null) {
            sb.rightAngleBracket();
            sb.element(hash);
            sb.closeElement(this);
        } else {
            sb.closeEmptyElement();
        }
        return sb;
    }
}
