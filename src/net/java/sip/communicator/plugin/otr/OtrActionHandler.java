/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.otr;

import java.util.*;

/**
 * Interface for providing OTR related actions handlers.
 *
 * @author Pawel Domas
 */
public interface OtrActionHandler
{
    /**
     * Method fired when authenticate chat link is clicked.
     *
     * @param uuid session's identifier extracted from clicked URL.
     */
    public void onAuthenticateLinkClicked(UUID uuid);
}
