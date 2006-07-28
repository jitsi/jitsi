package net.java.sip.communicator.impl.protocol.icq;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * The ICQ implementation of a sip-communicator AccountID
 *
 * @author Emil Ivov
 */
public class IcqAccountID
    extends AccountID
{
    /**
     * Creates an icq account id from the specified uin and account properties.
     * @param uin the uin identifying this account
     * @param accountProperties any other properties necessary for the account.
     */
    IcqAccountID(String uin, Map accountProperties )
    {
        super(uin, accountProperties, ProtocolNames.ICQ, "icq.com");
    }
}
