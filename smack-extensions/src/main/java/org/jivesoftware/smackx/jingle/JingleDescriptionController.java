package org.jivesoftware.smackx.jingle;

/**
 * Created by vanitas on 27.07.17.
 */
public interface JingleDescriptionController {
    enum State {
        pending,            //Not yet accepted by us/peer
        negotiating,        //Accepted, but still negotiating transports etc.
        active,             //Bytestream initialized and active
        cancelled,          //We/Peer cancelled the transmission
        ended               //Successfully ended
    }

    State getState();
}
