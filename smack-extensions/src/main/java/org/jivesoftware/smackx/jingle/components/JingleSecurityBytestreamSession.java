package org.jivesoftware.smackx.jingle.components;

import java.io.IOException;

import org.jivesoftware.smackx.bytestreams.BytestreamSession;

/**
 * Created by vanitas on 27.07.17.
 */
public abstract class JingleSecurityBytestreamSession implements BytestreamSession {

    protected BytestreamSession wrapped;

    @Override
    public int getReadTimeout() throws IOException {
        return wrapped.getReadTimeout();
    }

    @Override
    public void setReadTimeout(int timeout) throws IOException {
        wrapped.setReadTimeout(timeout);
    }

    public JingleSecurityBytestreamSession(BytestreamSession session) {
        this.wrapped = session;
    }
}
