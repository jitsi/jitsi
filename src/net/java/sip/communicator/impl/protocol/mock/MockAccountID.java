/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.mock;

import net.java.sip.communicator.service.protocol.*;

/**
 * A default, 1-to-1 mock implementation of the account id.
 * @author Emil Ivov
 */
public class MockAccountID
    extends AccountID
{
    public static final String MOCK_SERVICE_NAME = "MockService";
    protected MockAccountID(String userName)
    {
        super(  userName
              , new java.util.Hashtable<String, String>()
              , ProtocolNames.SIP_COMMUNICATOR_MOCK
              , MOCK_SERVICE_NAME);
    }
}
