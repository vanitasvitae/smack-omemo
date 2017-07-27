package org.jivesoftware.smackx.jft.internal.file;

import java.util.Date;

import org.jivesoftware.smackx.hashes.element.HashElement;
import org.jivesoftware.smackx.jft.element.JingleFileTransferChildElement;

/**
 * Created by vanitas on 26.07.17.
 */
public abstract class AbstractJingleFileTransferFile {

    public AbstractJingleFileTransferFile() {

    }

    public JingleFileTransferChildElement getElement() {
        JingleFileTransferChildElement.Builder builder = JingleFileTransferChildElement.getBuilder();
        builder.setDate(getDate());
        builder.setSize(getSize());
        builder.setName(getName());
        builder.setDescription(getDescription());
        builder.setMediaType(getMediaType());
        builder.setHash(getHashElement());

        return builder.build();
    }

    public abstract Date getDate();

    public abstract long getSize();

    public abstract String getName();

    public abstract String getDescription();

    public abstract String getMediaType();

    public abstract HashElement getHashElement();
}
