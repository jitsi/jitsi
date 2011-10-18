/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

/**
 * <tt>ContactPresenceStatusListener</tt>s listener for events caused by
 * changes in the status of contacts that we have active subscriptions for.
 * <p>
 * Events handled by this listener a most often the direct result of server/
 * remotely generated notifications.
 * @author Emil Ivov
 */
public interface ContactPresenceStatusListener
    extends EventListener
{
    /**
     * Called whenever a change occurs in the PresenceStatus of one of the
     * contacts that we have subscribed for.
     * @param evt the ContactPresenceStatusChangeEvent describing the status
     * change.
     */
    public void contactPresenceStatusChanged(
                                    ContactPresenceStatusChangeEvent evt);

}
