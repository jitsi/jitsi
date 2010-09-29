/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.provdisc.event;

import java.util.*;

/**
 * Event representing that a provisioning URL has been retrieved.
 *
 * @author Sebastien Vincent
 */
public class DiscoveryEvent extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Provisioning URL.
     */
    private String url = null;

    /**
     * Constructor.
     *
     * @param source object that have created this event
     * @param url provisioning URL
     */
    public DiscoveryEvent(Object source, String url)
    {
        super(source);
        this.url = url;
    }

    /**
     * Get the provisioning URL.
     *
     * @return provisioning URL
     */
    public String getProvisioningURL()
    {
        return url;
    }
}
