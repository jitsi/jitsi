/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.colibri;

import org.jivesoftware.smack.packet.*;

/**
 * The IQ used to trigger the graceful shutdown mode of the videobridge which
 * receives the stanza(given that source JID is authorized to start it).
 *
 * @author Pawel Domas
 */
public class GracefulShutdownIQ
    extends IQ
{
    public static final String NAMESPACE = ColibriConferenceIQ.NAMESPACE;

    public static final String ELEMENT_NAME = "graceful-shutdown";

    @Override
    public String getChildElementXML()
    {
        return "<" + ELEMENT_NAME + " xmlns='" + NAMESPACE + "' />";
    }
}
