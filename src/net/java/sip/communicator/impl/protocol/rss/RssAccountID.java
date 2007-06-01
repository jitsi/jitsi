/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.rss;

import net.java.sip.communicator.service.protocol.*;
import java.util.Map;

/**
 * The Rss implementation of a sip-communicator account id.
 * @author Emil Ivov
 */
public class RssAccountID
    extends AccountID
{
    /**
     * Creates an account id from the specified id and account properties.
     *
     * @param userID the user identifier correspnding to the account
     * @param accountProperties any other properties necessary for the account.
     */
    RssAccountID(String userID, Map accountProperties)
    {
        super(  userID, 
                accountProperties, 
                "Rss", 
                "rss.org");
    }
}
