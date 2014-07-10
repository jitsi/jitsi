package net.java.sip.communicator.impl.protocol.msn;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * The Msn implementation of a sip-communicator AccountID
 *
 * @author Damian Minkov
 */
public class MsnAccountID
    extends AccountID
{
    /**
     * Creates an account id from the specified id and account properties.
     * @param id the id identifying this account
     * @param accountProperties any other properties necessary for the account.
     */
    MsnAccountID(String id, Map<String, String> accountProperties )
    {
        super(id, accountProperties, ProtocolNames.MSN, "msn.com");
    }
}
