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
        super(uin, accountProperties, 
            isAIM(accountProperties) ? ProtocolNames.AIM : ProtocolNames.ICQ, 
            isAIM(accountProperties) ? "aim.com" : "icq.com");
    }
    
    /**
     * Checks is the provided properties are for icq or aim account
     * @param accountProperties account properties for the checked account.
     */
    static boolean isAIM(Map accountProperties)
    {
        Object isAim = accountProperties.get(IS_AIM);

        if(isAim != null && 
            isAim instanceof String && 
            ((String)isAim).equalsIgnoreCase("true"))
                return true;
        else
            return false;
    }
}
