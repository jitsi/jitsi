/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
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
     * Then name of a property which represenstots is this account icq or aim.
     */
    public static final String IS_AIM = "IS_AIM";
    
    /**
     * Creates an icq account id from the specified uin and account properties.
     * If property IS_AIM is set to true then this is an AIM account, else 
     * an Icq one.
     * @param uin the uin identifying this account
     * @param accountProperties any other properties necessary for the account.
     */
    IcqAccountID(String uin, Map<String, String> accountProperties )
    {
        super(
            uin,
            accountProperties,
            isAIM(accountProperties) ? ProtocolNames.AIM : ProtocolNames.ICQ,
            isAIM(accountProperties) ? "aim.com" : "icq.com");
    }
    
    /**
     * Checks whether the specified set of account properties describes an AIM
     * account.
     *
     * @param accountProperties the set of account properties to be checked
     * whether they describe an AIM account
     * @return <tt>true</tt> if <tt>accountProperties</tt> describes an AIM
     * account; otherwise, <tt>false</tt>
     */
    static boolean isAIM(Map<String, String> accountProperties)
    {
        String isAim = accountProperties.get(IS_AIM);

        return "true".equalsIgnoreCase(isAim);
    }
}
