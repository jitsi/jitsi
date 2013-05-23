/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.gui;

import java.net.*;

/**
 * Event-callback for clicks on links.
 *
 * @author Daniel Perren
 */
public interface ChatLinkClickedListener
{
    /**
     * Callback that is executed when a link was clicked.
     *
     * @param url The URI of the link that was clicked.
     */
    public void chatLinkClicked(URI url);
}
