/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.gibberish;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * The Gibberish implementation of a sip-communicator account id.
 * @author Emil Ivov
 */
public class GibberishAccountID
    extends AccountID
{
    /**
     * Creates an account id from the specified id and account properties.
     *
     * @param userID the user identifier correspnding to thi account
     * @param accountProperties any other properties necessary for the account.
     */
    GibberishAccountID(String userID, Map<String, String> accountProperties)
    {
        super(userID
              , accountProperties
              , "Gibberish"
              , "gibberish.org");
    }
}
