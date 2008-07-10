/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.geolocation;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.jabberconstants.*;

import org.jivesoftware.smack.packet.*;
import net.java.sip.communicator.impl.protocol.jabber.*;

/**
 * This class represents a Jabber presence message includin a Geolocation
 * Extension.
 *
 * @author Guillaume Schreiner
 */
public class GeolocationPresence
{

    /**
     * the presence message to send via a XMPPConnection
     */
    private Presence prez = null;

    /**
     *
     * @param persistentPresence OperationSetPresence
     */
    public GeolocationPresence(OperationSetPresence persistentPresence)
    {
        this.prez = new Presence(Presence.Type.available);

        // set the custom status message
        this.prez.setStatus(persistentPresence
                            .getCurrentStatusMessage());

        // set the presence mode (available, NA, free for chat)
        this.prez.setMode(
            OperationSetPersistentPresenceJabberImpl.presenceStatusToJabberMode(
                persistentPresence
                .getPresenceStatus()));
    }

    public void setGeolocationExtention(GeolocationPacketExtension ext)
    {
        this.prez.addExtension(ext);
    }

    public Presence getGeolocPresence()
    {
        return this.prez;
    }

}

