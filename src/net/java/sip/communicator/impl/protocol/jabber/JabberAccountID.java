/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * The Jabber implementation of a sip-communicator AccountID
 *
 * @author Damian Minkov
 * @author Sebastien Vincent
 */
public class JabberAccountID
    extends AccountID
{
    /**
     * Creates an account id from the specified id and account properties.
     * @param id the id identifying this account
     * @param accountProperties any other properties necessary for the account.
     */
    JabberAccountID(String id, Map<String, String> accountProperties )
    {
        super(  id, accountProperties,
                ProtocolNames.JABBER,
                getServiceName(accountProperties));
    }

    /**
     * Returns the service name - the server we are logging to
     * if it is null which is not supposed to be - we return for compatibility
     * the string we used in the first release for creating AccountID
     * (Using this string is wrong, but used for compatibility for now)
     * @param accountProperties Map
     * @return String
     */
    private static String getServiceName(Map<String, String> accountProperties)
    {
        String serviceName
            = accountProperties.get(ProtocolProviderFactory.SERVER_ADDRESS);

        return (serviceName != null) ? serviceName : "jabber.org";
    }

    /**
     * Returns the list of STUN servers that this account is currently
     * configured to use.
     *
     * @return the list of STUN servers that this account is currently
     * configured to use.
     */
    public List<StunServerDescriptor> getStunServers()
    {
        Map<String, String> accountProperties = getAccountProperties();
        List<StunServerDescriptor> serList
            = new ArrayList<StunServerDescriptor>();

        for (int i = 0; i < StunServerDescriptor.MAX_STUN_SERVER_COUNT; i ++)
        {
            StunServerDescriptor stunServer
                = StunServerDescriptor.loadDescriptor(
                        accountProperties,
                        ProtocolProviderFactory.STUN_PREFIX + i);

            // If we don't find a stun server with the given index, it means
            // that there're no more servers left in the table so we've nothing
            // more to do here.
            if (stunServer == null)
                break;

            serList.add(stunServer);
        }

        return serList;
    }

    /**
     * Determines whether this account's provider is supposed to auto discover
     * STUN and TURN servers.
     *
     * @return <tt>true</tt> if this provider would need to discover STUN/TURN
     * servers and false otherwise.
     */
    public boolean isStunServerDiscoveryEnabled()
    {
        return
            getAccountPropertyBoolean(
                    ProtocolProviderFactory.AUTO_DISCOVER_STUN,
                    false);
    }

    /**
     * Determines whether this account's provider uses the default STUN server
     * provided by SIP Communicator if there is no other STUN/TURN server
     * discovered/configured.
     *
     * @return <tt>true</tt> if this provider would use the default STUN server,
     * <tt>false</tt> otherwise
     */
    public boolean isUseDefaultStunServer()
    {
        return
            getAccountPropertyBoolean(
                    ProtocolProviderFactory.USE_DEFAULT_STUN_SERVER,
                    true);
    }

    /**
     * Returns the list of JingleNodes trackers/relays that this account is
     * currently configured to use.
     *
     * @return the list of JingleNodes trackers/relays that this account is
     * currently configured to use.
     */
    public List<JingleNodeDescriptor> getJingleNodes()
    {
        Map<String, String> accountProperties = getAccountProperties();
        List<JingleNodeDescriptor> serList
            = new ArrayList<JingleNodeDescriptor>();

        for (int i = 0; i < JingleNodeDescriptor.MAX_JN_RELAY_COUNT; i ++)
        {
            JingleNodeDescriptor node
                = JingleNodeDescriptor.loadDescriptor(
                        accountProperties,
                        JingleNodeDescriptor.JN_PREFIX + i);

            // If we don't find a relay server with the given index, it means
            // that there're no more servers left in the table so we've nothing
            // more to do here.
            if (node == null)
                break;

            serList.add(node);
        }

        return serList;
    }

    /**
     * Determines whether this account's provider is supposed to auto discover
     * JingleNodes relay.
     *
     * @return <tt>true</tt> if this provider would need to discover JingleNodes
     * relay, <tt>false</tt> otherwise
     */
    public boolean isJingleNodesAutoDiscoveryEnabled()
    {
        return getAccountPropertyBoolean(
                ProtocolProviderFactory.AUTO_DISCOVER_JINGLE_NODES,
                true);
    }

    /**
     * Determines whether this account's provider uses JingleNodes relay (if
     * available).
     *
     * @return <tt>true</tt> if this provider would use JingleNodes relay (if
     * available), <tt>false</tt> otherwise
     */
    public boolean isJingleNodesRelayEnabled()
    {
        return getAccountPropertyBoolean(
                ProtocolProviderFactory.IS_USE_JINGLE_NODES,
                true);
    }
}
