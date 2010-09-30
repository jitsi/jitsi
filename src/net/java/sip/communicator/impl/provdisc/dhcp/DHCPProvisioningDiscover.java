/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.provdisc.dhcp;

import java.net.*;
import java.util.*;

import net.java.sip.communicator.service.provdisc.event.*;
import net.java.sip.communicator.service.netaddr.*;
import net.java.sip.communicator.util.*;

import org.dhcp4java.*;

/**
 * Class that will perform DHCP provisioning discovery.
 *
 * @author Sebastien Vincent
 */
public class DHCPProvisioningDiscover
    implements Runnable
{
    /**
     * Logger.
     */
    private final Logger logger
        = Logger.getLogger(DHCPProvisioningDiscover.class);

    /**
     * UDP socket.
     */
    private DatagramSocket socket = null;

    /**
     * Listening port of the client. Note that the socket will send packet to
     * DHCP server on port - 1.
     */
    private int port = 6768;

    /**
     * Option code of the specific provisioning option.
     */
    private byte option = (byte)120;

    /**
     * DHCP timeout (in milliseconds).
     */
    private static final int DHCP_TIMEOUT = 5000;

    /**
     * List of <tt>ProvisioningListener</tt> that will be notified when
     * a provisioning URL is retrieved.
     */
    private List<DiscoveryListener> listeners =
        new ArrayList<DiscoveryListener>();

    /**
     * Constructor.
     *
     * @param port port on which we will bound and listen for DHCP response
     * @param option code of the specific provisioning option
     * @throws Exception if anything goes wrong during initialization
     */
    public DHCPProvisioningDiscover(int port, byte option) throws Exception
    {
        this.port = port;
        this.option = option;
        socket = new DatagramSocket(port);

        /* set timeout so that we will not blocked forever if we
         * have no response from DHCP server
         */
        socket.setSoTimeout(DHCP_TIMEOUT);
    }

    /**
     * It sends a DHCPINFORM message from all interfaces and wait for a
     * response. Thread stops after first successful answer that contains
     * specific option and thus the provisioning URL.
     *
     * @return provisioning URL
     */
    public String discoverProvisioningURL()
    {
        DHCPPacket inform = new DHCPPacket();
        byte macAddress[] = null;
        byte zeroIPAddress[] = {0x00, 0x00, 0x00, 0x00};
        byte broadcastIPAddr[] = {(byte)255, (byte)255, (byte)255, (byte)255};
        DHCPOption dhcpOpts[] = new DHCPOption[1];
        int xid = new Random().nextInt();

        try
        {

            inform.setOp(DHCPConstants.BOOTREQUEST);
            inform.setHtype(DHCPConstants.HTYPE_ETHER);
            inform.setHlen((byte) 6);
            inform.setHops((byte) 0);
            inform.setXid(xid);
            inform.setSecs((short) 0);
            inform.setFlags((short) 0);
            inform.setYiaddr(InetAddress.getByAddress(zeroIPAddress));
            inform.setSiaddr(InetAddress.getByAddress(zeroIPAddress));
            inform.setGiaddr(InetAddress.getByAddress(zeroIPAddress));
            inform.setChaddr(macAddress);
            inform.setDhcp(true);
            inform.setDHCPMessageType(DHCPConstants.DHCPINFORM);

            dhcpOpts[0] = new DHCPOption(
                    DHCPConstants.DHO_DHCP_PARAMETER_REQUEST_LIST,
                    new byte[] {option});

            inform.setOptions(dhcpOpts);

            Enumeration<NetworkInterface> en =
                NetworkInterface.getNetworkInterfaces();

            while(en.hasMoreElements())
            {
                NetworkInterface iface = en.nextElement();

                Enumeration<InetAddress> enAddr = iface.getInetAddresses();
                while(enAddr.hasMoreElements())
                {
                    InetAddress addr = enAddr.nextElement();

                    /* just take IPv4 address */
                    if(addr instanceof Inet4Address)
                    {
                        NetworkAddressManagerService netaddr =
                            ProvisioningDiscoveryDHCPActivator.
                                getNetworkAddressManagerService();

                        if(!addr.isLoopbackAddress())
                        {
                            macAddress = netaddr.getHardwareAddress(iface);
                            DHCPPacket p = inform.clone();

                            p.setCiaddr(addr);
                            p.setChaddr(macAddress);

                            byte msg[] = inform.serialize();
                            DatagramPacket pkt = new DatagramPacket(msg,
                                    msg.length,
                                    InetAddress.getByAddress(broadcastIPAddr),
                                    port - 1);

                            socket.send(pkt);
                            msg = null;
                            pkt = null;
                            p = null;
                        }
                    }
                }
            }

            /* now see if we receive DHCP ACK response and if it contains
             * our custom option
             */
            boolean found = false;

            try
            {
                DatagramPacket pkt2 = new DatagramPacket(new byte[1500], 1500);

                while(!found)
                {
                    /* we timeout after 5 seconds if no DHCP response are
                     * received
                     */
                    socket.receive(pkt2);
                    DHCPPacket dhcp = DHCPPacket.getPacket(pkt2);

                    if(dhcp.getXid() != xid)
                    {
                        continue;
                    }

                    DHCPOption optProvisioning = dhcp.getOption(option);

                    /* notify */
                    if(optProvisioning != null)
                    {
                        found = true;
                        return new String(optProvisioning.getValue());
                    }
                }
            }
            catch(SocketTimeoutException est)
            {
                logger.warn("Timeout, no DHCP answer received", est);
            }
        }
        catch(Exception e)
        {
            logger.warn("Exception occurred during DHCP discover", e);
        }

        return null;
    }

    /**
     * Thread entry point. It runs <tt>discoverProvisioningURL</tt> in a
     * separate thread.
     */
    public void run()
    {
        String url = discoverProvisioningURL();

        if(url != null)
        {
            /* as we run in an asynchronous manner, notify the listener */
            DiscoveryEvent evt = new DiscoveryEvent(this, url);

            for(DiscoveryListener listener : listeners)
            {
                listener.notifyProvisioningURL(evt);
            }
        }
    }

    /**
     * Add a listener that will be notified when the
     * <tt>discoverProvisioningURL</tt> has finished.
     *
     * @param listener <tt>ProvisioningListener</tt> to add
     */
    public void addDiscoveryListener(DiscoveryListener listener)
    {
        if(!listeners.contains(listener))
        {
            listeners.add(listener);
        }
    }

    /**
     * Add a listener that will be notified when the
     * <tt>discoverProvisioningURL</tt> has finished.
     *
     * @param listener <tt>ProvisioningListener</tt> to add
     */
    public void removeDiscoveryListener(DiscoveryListener listener)
    {
        if(listeners.contains(listener))
        {
            listeners.remove(listener);
        }
    }
}
