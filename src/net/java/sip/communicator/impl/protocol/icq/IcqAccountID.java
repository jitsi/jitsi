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
    IcqAccountID(String accountID, Map accountProperties)
    {
        super(accountID, accountProperties);
    }
}
