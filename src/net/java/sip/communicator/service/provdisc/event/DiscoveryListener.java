/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.provdisc.event;

import java.util.*;

/**
 * Listener that will be notified when a provisioning URL is retrieved by the
 * <tt>ProvisioningDiscoveryService</tt>.
 *
 * @author Sebastien Vincent
 */
public interface DiscoveryListener extends EventListener
{
    /**
     * Notify the provisioning URL.
     *
     * @param event provisioning event
     */
    public void notifyProvisioningURL(DiscoveryEvent event);
}
