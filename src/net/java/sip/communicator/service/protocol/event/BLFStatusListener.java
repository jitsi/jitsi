/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

/**
 * Listens for changes in the BLFStatus of the monitored lines we have
 * subscribed for.
 * @author Damian Minkov
 */
public interface BLFStatusListener
    extends EventListener
{
    /**
     * Called whenever a change occurs in the BLFStatus of one of the
     * monitored lines that we have subscribed for.
     * @param event the BLFStatusEvent describing the status change.
     */
    public void blfStatusChanged(BLFStatusEvent event);
}
