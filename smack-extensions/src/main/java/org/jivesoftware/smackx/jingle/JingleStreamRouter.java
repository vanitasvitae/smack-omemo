package org.jivesoftware.smackx.jingle;

/**
 * Created by vanitas on 06.07.17.
 */
public class JingleStreamRouter {
    private static JingleStreamRouter INSTANCE;

    private JingleStreamRouter() {

    }

    public static JingleStreamRouter getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new JingleStreamRouter();
        }
        return INSTANCE;
    }
}
