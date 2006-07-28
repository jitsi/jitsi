/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * A SIP extension of the account ID property.
 * @author Emil Ivov
 */
public class SipAccountID
    extends AccountID
{

    /**
     * Creates a SIP account id from the specified ide and account properties.
     *
     * @param userID the user id part of the SIP uri identifying this contact.
     * @param accountProperties any other properties necessary for the account.
     */
    protected SipAccountID(String userID,
                           Map    accountProperties,
                           String serviceName)
    {
        super(userID, accountProperties, ProtocolNames.SIP, serviceName);
    }
}
