/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.dict;

import net.java.sip.communicator.service.protocol.*;

import java.util.Map;

/**
 * The Dict implementation of a sip-communicator account id.
 * @author LITZELMANN Cedric
 * @author ROTH Damien
 */
public class DictAccountID
    extends AccountID
{
    /**
     * Creates an account id from the specified id and account properties.
     *
     * @param userID the user identifier correspnding to the account
     * @param accountProperties any other properties necessary for the account.
     */
    DictAccountID(String userID, Map<String, String> accountProperties)
    {
        super(userID, accountProperties, ProtocolNames.DICT, "dict.org");
    }

    /**
     * Returns the dict server adress
     * @return the dict server adress
     */
    public String getHost()
    {
        return getAccountPropertyString(ProtocolProviderFactory.SERVER_ADDRESS);
    }

    /**
     * Returns the dict server port
     * @return the dict server port
     */
    public int getPort()
    {
        return Integer
            .parseInt(getAccountPropertyString(ProtocolProviderFactory.SERVER_PORT));
    }

    /**
     * Returns the selected strategy
     * @return the selected strategy
     */
    public String getStrategy()
    {
        return getAccountPropertyString(ProtocolProviderFactory.STRATEGY);
    }
}
