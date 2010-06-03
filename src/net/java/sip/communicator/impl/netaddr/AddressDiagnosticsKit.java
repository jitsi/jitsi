/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.netaddr;

import java.net.*;

import net.java.sip.communicator.util.*;
import net.java.stun4j.*;
import net.java.stun4j.attribute.*;
import net.java.stun4j.message.*;

/**
 * Runs a separate thread of diagnostics for a given network address. The
 * diagnostics thread would discover NAT bindings through stun, update bindings
 * lifetime test connectivity and etc.
 *
 * @author Emil Ivov
 */
public class AddressDiagnosticsKit
        extends Thread
{
    private static final Logger logger =
        Logger.getLogger(AddressDiagnosticsKit.class);

    public static final int DIAGNOSTICS_STATUS_OFF = 1;
    public static final int DIAGNOSTICS_STATUS_DISOVERING_CONFIG = 2;
    public static final int DIAGNOSTICS_STATUS_RESOLVING = 3;
    public static final int DIAGNOSTICS_STATUS_COMPLETED = 4;
    public static final int DIAGNOSTICS_STATUS_DISOVERING_BIND_LIFETIME = 5;
    public static final int DIAGNOSTICS_STATUS_TERMINATED = 6;

    private int diagnosticsStatus = DIAGNOSTICS_STATUS_OFF;

    /**
     * These are used by (to my knowledge) mac and windows boxes when dhcp
     * fails and are only usable with other boxes using the same address
     * in the same net segment. That's why they get their low preference.
     */
    // TODO Remove after confirmation that this not used
//    private static final AddressPreference ADDR_PREF_LOCAL_IPV4_AUTOCONF
//        = new AddressPreference(40);

    /**
     * Local IPv6 addresses are assigned by default to any network iface running
     * an ipv6 stack. Theya are one of our last resorts since an internet
     * connected node would have generally configured sth else as well.
     */
    private static final AddressPreference ADDR_PREF_LOCAL_IPV6
        = new AddressPreference(40);

    /**
     * Local IPv4 addresses are either assigned by DHCP or manually configured
     * which means that even if they're unresolved to a globally routable
     * address they're still there for a reason (let the reason be ...) and this
     * reason might very well be purposeful so they should get a preference
     * higher than local IPv6 (even though I'm an IPv6 fan :) )
     */
    private static final AddressPreference ADDR_PREF_PRIVATE_IPV4
        = new AddressPreference(50);

    /**
     * Global IPv4 Addresses are a good think when they work. We are therefore
     * setting a high preference that will then be corrected by.
     */
    private static final AddressPreference ADDR_PREF_GLOBAL_IPV4
        = new AddressPreference(60);

    /**
     * There are many reasons why global IPv6 addresses should have the highest
     * preference. A global IPv6 address is most often delivered through
     * stateless address autoconfiguration which means an active router and
     * might also mean an active net connection.
     */
    private static final AddressPreference ADDR_PREF_GLOBAL_IPV6
        = new AddressPreference(70);

    /**
     * The address of the stun server to query
     */
    private StunAddress primaryStunServerAddress =
        new StunAddress("stun01.sipphone.com", 3478);

    /**
     * The address pool entry that this kit is diagnosing.
     */
    private AddressPoolEntry addressEntry = null;

    /**
     * Specifies whether stun should be used or not.
     * This field is updated during runtime to conform to the configuration.
     */
    private boolean useStun = true;

    private StunClient stunClient = null;

    /**
     * The port to be used locally for sending generic stun queries.
     */
    static final int LOCAL_STUN_PORT = 55126;
    private int bindRetries = 10;

    public AddressDiagnosticsKit(AddressPoolEntry      addressEntry)
    {
        this.addressEntry = addressEntry;
        setDiagnosticsStatus(DIAGNOSTICS_STATUS_OFF);
    }

    /**
     * Sets the current status of the address diagnostics process
     * @param status int
     */
    private void setDiagnosticsStatus(int status)
    {
        this.diagnosticsStatus = status;
    }

    /**
     * Returns the current status of this diagnosics process.
     * @return int
     */
    public int getDiagnosticsStatus()
    {
        return this.diagnosticsStatus;
    }

    /**
     * The diagnostics code itself.
     */
    public void run()
    {
        if (logger.isDebugEnabled())
            logger.debug("Started a diag kit for entry: " + addressEntry);

        //implements the algorithm from AssigningAddressPreferences.png

        setDiagnosticsStatus(DIAGNOSTICS_STATUS_DISOVERING_CONFIG);

        InetAddress address = addressEntry.getInetAddress();

        //is this an ipv6 address
        if (addressEntry.isIPv6())
        {
            if (addressEntry.isLinkLocal())
            {
                addressEntry.setAddressPreference(ADDR_PREF_LOCAL_IPV6);
                setDiagnosticsStatus(DIAGNOSTICS_STATUS_TERMINATED);
                return;
            }

            if (addressEntry.is6to4())
            {
                //right now we don't support these. we should though ... one day
                addressEntry.setAddressPreference(AddressPreference.MIN);
                setDiagnosticsStatus(DIAGNOSTICS_STATUS_TERMINATED);
                return;
            }

            //if we get here then we are a globally routable ipv6 addr
            addressEntry.setAddressPreference(ADDR_PREF_GLOBAL_IPV6);
            setDiagnosticsStatus(DIAGNOSTICS_STATUS_COMPLETED);
            //should do some connectivity testing here and proceed with firewall
            //discovery but since stun4j does not support ipv6 yet, this too
            //will happen another day.
            return;
        }

        //from now on we're only dealing with IPv4
        if (addressEntry.isIPv4LinkLocalAutoconf())
        {
            //not sure whether these are used for anything.
            addressEntry.setAddressPreference(AddressPreference.MIN);
            setDiagnosticsStatus(DIAGNOSTICS_STATUS_TERMINATED);
            return;
        }

        //first try and see what we can infer from just looking at the
        //address
        if (addressEntry.isLinkLocalIPv4Address())
        {
            addressEntry.setAddressPreference(ADDR_PREF_PRIVATE_IPV4);
        }
        else
        {
            //public address
            addressEntry.setAddressPreference(ADDR_PREF_GLOBAL_IPV4);
        }

        if (!useStun)
        {
            //if we're configured not to run stun - we're done.
            setDiagnosticsStatus(DIAGNOSTICS_STATUS_TERMINATED);
            return;
        }

        //start stunning
        for(int i = 0; i < bindRetries; i++){
            StunAddress localStunAddress = new StunAddress(
                    address, 1024 + (int) (Math.random() * 64512));
            try
            {

                stunClient = new StunClient(localStunAddress);
                stunClient.start();
                if (logger.isDebugEnabled())
                    logger.debug("Successfully started StunClient for  "
                                 + localStunAddress + ".");
                break;
            }
            catch (StunException ex)
            {
                if (ex.getCause() instanceof SocketException
                    && i < bindRetries)
                {
                    if (logger.isDebugEnabled())
                        logger.debug("Failed to bind to "
                                 + localStunAddress + ". Retrying ...");
                    if (logger.isDebugEnabled())
                        logger.debug("Exception was ", ex);
                    continue;
                }
                logger.error("Failed to start a stun client for address entry ["
                             + addressEntry.toString()+"]:"
                             +localStunAddress.getPort() + ". Ceasing attempts",
                             ex);
                setDiagnosticsStatus(DIAGNOSTICS_STATUS_TERMINATED);
                return;
            }
        }
        //De Stun Test I
        StunMessageEvent event = null;
        try
        {
            event = stunClient.doStunTestI(
                primaryStunServerAddress);
        }
        catch (StunException ex)
        {
            logger.error("Failed to perform STUN Test I for address entry"
                + addressEntry.toString(), ex);
            setDiagnosticsStatus(DIAGNOSTICS_STATUS_TERMINATED);
            stunClient.shutDown();
            return;
        }

        if(event == null)
        {
            //didn't get a response - we either don't have connectivity or the
            //server is down
            /** @todo if possible try another stun server here. we should
             * support multiple stun servers*/
            if (logger.isDebugEnabled())
                logger.debug("There seems to be no inet connectivity for "
                         + addressEntry);
            setDiagnosticsStatus(DIAGNOSTICS_STATUS_TERMINATED);
            stunClient.shutDown();
            if (logger.isDebugEnabled())
                logger.debug("stun test 1 failed");
            return;
        }

        //the moment of the truth - are we behind a NAT?
        boolean isPublic;
        Message stunResponse = event.getMessage();

        Attribute mappedAttr = stunResponse.getAttribute(Attribute.MAPPED_ADDRESS);

        StunAddress mappedAddrFromTestI = ((MappedAddressAttribute)mappedAttr).getAddress();
        Attribute changedAddressAttributeFromTestI
            = stunResponse.getAttribute(Attribute.CHANGED_ADDRESS);
        StunAddress secondaryStunServerAddress =
            ((ChangedAddressAttribute)changedAddressAttributeFromTestI).
                getAddress();

        /** @todo verify whether the stun server returned the same address for
         * the primary and secondary server and act accordingly
         * */

        if(mappedAddrFromTestI == null){
            logger.error(
                "Stun Server did not return a mapped address for entry "
                + addressEntry.toString());
            setDiagnosticsStatus(DIAGNOSTICS_STATUS_TERMINATED);
            return;
        }

        if(mappedAddrFromTestI.equals(event.getSourceAccessPoint().getAddress()))
        {
            isPublic = true;
        }
        else
        {
            isPublic = false;
        }

        //do STUN Test II
        try
        {
            event = stunClient.doStunTestII(primaryStunServerAddress);
        }
        catch (StunException ex)
        {
            logger.error("Failed to perform STUN Test II for address entry"
                + addressEntry.toString(), ex);
            setDiagnosticsStatus(DIAGNOSTICS_STATUS_TERMINATED);
            stunClient.shutDown();
            if (logger.isDebugEnabled())
                logger.debug("stun test 2 failed");
            return;
        }

        if(event != null){
            logger.error("Secondary STUN server is down"
                         + addressEntry.toString());
            setDiagnosticsStatus(DIAGNOSTICS_STATUS_TERMINATED);
            stunClient.shutDown();
            return;
        }

        //might mean that either the secondary stun server is down
        //or that we are behind a restrictive firewall. Let's find out
        //which.
        try
        {
            event = stunClient.doStunTestI(secondaryStunServerAddress);
            if (logger.isDebugEnabled())
                logger.debug("stun test 1 succeeded with s server 2");
        }
        catch (StunException ex)
        {
            logger.error("Failed to perform STUN Test I for address entry"
                         + addressEntry.toString(), ex);
            setDiagnosticsStatus(DIAGNOSTICS_STATUS_TERMINATED);
            stunClient.shutDown();
            return;
        }

        if (event == null)
        {
            //secondary stun server is down
            logger.error("Secondary STUN server is down"
                         + addressEntry.toString());
            setDiagnosticsStatus(DIAGNOSTICS_STATUS_TERMINATED);
            stunClient.shutDown();
            return;
        }

        //we are at least behind a port restricted nat

        stunResponse = event.getMessage();
        mappedAttr = stunResponse.getAttribute(Attribute.MAPPED_ADDRESS);
        StunAddress mappedAddrFromSecServer =
            ((MappedAddressAttribute)mappedAttr).getAddress();

        if(!mappedAddrFromTestI.equals(mappedAddrFromSecServer))
        {
            //secondary stun server is down
            if (logger.isDebugEnabled())
                logger.debug("We are behind a symmetric nat"
                         + addressEntry.toString());
            setDiagnosticsStatus(DIAGNOSTICS_STATUS_TERMINATED);
            stunClient.shutDown();
            return;
        }

        //now let's run test III so that we could guess whether or not we're
        //behind a port restricted nat/fw or simply a restricted one.
        try
        {
            event = stunClient.doStunTestIII(primaryStunServerAddress);
            if (logger.isDebugEnabled())
                logger.debug("stun test 3 succeeded with s server 1");
        }
        catch (StunException ex)
        {
            logger.error("Failed to perform STUN Test III for address entry"
                         + addressEntry.toString(), ex);
            setDiagnosticsStatus(DIAGNOSTICS_STATUS_TERMINATED);
            stunClient.shutDown();
            return;
        }

        if (event == null)
        {
            if (logger.isDebugEnabled())
                logger.debug("We are behind a port restricted NAT or fw"
                         + addressEntry.toString());
            setDiagnosticsStatus(DIAGNOSTICS_STATUS_TERMINATED);
            stunClient.shutDown();
            return;
        }

        if (logger.isDebugEnabled())
            logger.debug("We are behind a restricted NAT or fw"
                     + addressEntry.toString());
        setDiagnosticsStatus(DIAGNOSTICS_STATUS_TERMINATED);
        stunClient.shutDown();
    }
}

