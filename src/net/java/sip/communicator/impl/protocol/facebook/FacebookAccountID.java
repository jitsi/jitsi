/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.facebook;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * The Facebook implementation of a sip-communicator account id.
 * 
 * @author Dai Zhiwei
 */
public class FacebookAccountID
    extends AccountID
{

    /**
     * Creates an account id from the specified id and account properties.
     * 
     * @param userID
     *            the user identifier corresponding to the account
     * @param accountProperties
     *            any other properties necessary for the account.
     */
    public FacebookAccountID(
            String userID,
            Map<String, String> accountProperties)
    {
        super(
            userID,
            accountProperties,
            ProtocolNames.FACEBOOK,
            "facebook.com");
    }
}
