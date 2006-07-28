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
 *
 * @author Emil Ivov
 */
public class SipAccountID
    extends AccountID
{
    protected SipAccountID(String userID, Map accountProperties,
                           String protocolName, String serviceName)
    {
        super(userID, accountProperties, protocolName, serviceName);
    }
}
