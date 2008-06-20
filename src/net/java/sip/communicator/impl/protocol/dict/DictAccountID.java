/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.dict;

import net.java.sip.communicator.service.protocol.*;
import java.util.Map;

/**
 * The Dict implementation of a sip-communicator account id.
 * @author LITZELMANN Cedric
 * @author ROTH Damien
 */
public class DictAccountID
    extends AccountID
{
    /**
     * Creates an account id from the specified id and account properties.
     *
     * @param userID the user identifier correspnding to the account
     * @param accountProperties any other properties necessary for the account.
     */
    DictAccountID(String userID, Map accountProperties)
    {
        super(userID, accountProperties, ProtocolNames.DICT, "dict.org");
    }
}
