package net.java.sip.communicator.impl.protocol.jabber;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * The Jabber implementation of a sip-communicator AccountID
 *
 * @author Damian Minkov
 */
public class JabberAccountID
    extends AccountID
{
    /**
     * Creates an account id from the specified id and account properties.
     * @param id the id identifying this account
     * @param accountProperties any other properties necessary for the account.
     */
    JabberAccountID(String id, Map accountProperties )
    {
        super(id, accountProperties, ProtocolNames.JABBER, "jabber.org");
    }
}
