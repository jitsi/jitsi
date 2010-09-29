/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.provdisc.dhcp;

import net.java.sip.communicator.service.provdisc.*;
import net.java.sip.communicator.service.provdisc.event.*;
import net.java.sip.communicator.util.*;

/**
 * Class that uses DHCP to retrieve provisioning URL. Basically it sends a
 * DHCPINFORM message with a custom option code in parameters list.
 *
 * Note that DHCP server have to understand this option and thus configured to
 * answer with a HTTP/HTTPS URL.
 *
 * @author Sebastien Vincent
 */
public class ProvisioningDiscoveryServiceDHCPImpl
    extends AbstractProvisioningDiscoveryService
    implements DiscoveryListener
{
    /**
     * Logger.
     */
    private final Logger logger
        = Logger.getLogger(ProvisioningDiscoveryServiceDHCPImpl.class);

    /**
     * DHCP provisioning discover object.
     */
    private DHCPProvisioningDiscover discover = null;

    /**
     * Name of the method used to retrieve provisioning URL.
     */
    private static final String METHOD_NAME = "DHCP";

    /**
     * Get the name of the method name used to retrieve provisioning URL.
     *
     * @return method name
     */
    public String getMethodName()
    {
        return METHOD_NAME;
    }

    /**
     * Constructor.
     */
    public ProvisioningDiscoveryServiceDHCPImpl()
    {
        try
        {
            discover = new DHCPProvisioningDiscover(6768, (byte)224);
            discover.addDiscoveryListener(this);
        }
        catch(Exception e)
        {
            logger.warn("Cannot create DHCP client socket", e);
        }
    }

    /**
     * Launch a discovery for a provisioning URL. This method is synchronous and
     * may block for some time. Note that you don't have to call
     * <tt>startDiscovery</tt> method prior to this one to retrieve URL.
     *
     * @return provisioning URL
     */
    public String discoverURL()
    {
        if(discover != null)
        {
            return discover.discoverProvisioningURL();
        }

        return null;
    }

    /**
     * Launch a discovery for a provisioning URL by sending a DHCP Inform
     * with parameter list option containing a custom option code.
     *
     * This method is asynchronous, the response will be notified to any
     * <tt>ProvisioningListener</tt> registered.
     */
    public void startDiscovery()
    {
        if(discover != null)
        {
            new Thread(discover).start();
        }
    }

    /**
     * Notify the provisioning URL.
     *
     * @param event provisioning event
     */
    public void notifyProvisioningURL(DiscoveryEvent event)
    {
        fireDiscoveryEvent(event);
    }
}