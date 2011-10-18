/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.gibberish;

import net.java.sip.communicator.service.protocol.*;

/**
 * A default, Gibberish implementation of the account id.
 * @author Yana Stamcheva
 */
public class AccountIDGibberishImpl
    extends AccountID
{
    public static final String GIBBERISH_SERVICE_NAME = "Gibberish";
    protected AccountIDGibberishImpl(String userName)
    {
        super(  userName,
                new java.util.Hashtable<String, String>(),
                ProtocolNames.GIBBERISH,
                GIBBERISH_SERVICE_NAME);
    }
}
