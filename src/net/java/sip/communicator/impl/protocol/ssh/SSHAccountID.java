/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 *
 * SSHAccountID.java
 *
 * SSH Suport in SIP Communicator - GSoC' 07 Project
 *
 */

package net.java.sip.communicator.impl.protocol.ssh;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * The SSH implementation of a sip-communicator account id.
 * @author Shobhit Jindal
 */
public class SSHAccountID
    extends AccountID
{
    /**
     * Creates an account id from the specified id and account properties.
     *
     * @param userID the user identifier correspnding to thi account
     * @param accountProperties any other properties necessary for the account.
     */
    SSHAccountID(String userID, Map<String, String> accountProperties)
    {
        super(userID
              , accountProperties
              , "SSH"
              , "sip-communicator.org");
    }
}
