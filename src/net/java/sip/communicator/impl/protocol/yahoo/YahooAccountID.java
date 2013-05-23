/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.yahoo;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * The Yahoo implementation of a sip-communicator AccountID
 *
 * @author Damian Minkov
 */
public class YahooAccountID
    extends AccountID
{
    /**
     * Creates an account id from the specified id and account properties.
     * @param id the id identifying this account
     * @param accountProperties any other properties necessary for the account.
     */
    YahooAccountID(String id, Map<String, String> accountProperties )
    {
        super(YahooSession.getYahooUserID(id),
              accountProperties, ProtocolNames.YAHOO, "yahoo.com");
    }
}
