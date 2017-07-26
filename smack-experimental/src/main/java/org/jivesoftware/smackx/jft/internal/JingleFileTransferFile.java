package org.jivesoftware.smackx.jft.internal;

import java.util.Date;

import org.jivesoftware.smackx.jft.element.JingleFileTransferChildElement;

/**
 * Created by vanitas on 26.07.17.
 */
public abstract class JingleFileTransferFile {

    public JingleFileTransferFile() {

    }

    public JingleFileTransferChildElement getElement() {
        JingleFileTransferChildElement.Builder builder = JingleFileTransferChildElement.getBuilder();
        builder.setDate(getDate());
        builder.setSize(getSize());
        builder.setName(getName());

        return builder.build();
    }

    public abstract Date getDate();

    public abstract long getSize();

    public abstract String getName();
}
