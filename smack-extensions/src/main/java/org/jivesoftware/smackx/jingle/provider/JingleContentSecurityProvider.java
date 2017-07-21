package org.jivesoftware.smackx.jingle.provider;

import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smackx.jingle.element.JingleContentSecurityElement;

import org.xmlpull.v1.XmlPullParser;

/**
 * Created by vanitas on 18.07.17.
 */
public abstract class JingleContentSecurityProvider<D extends JingleContentSecurityElement> extends ExtensionElementProvider<D> {

@Override
    public abstract D parse(XmlPullParser parser, int initialDepth) throws Exception;

    public abstract String getNamespace();

}

