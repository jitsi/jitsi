/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.zeroconf;

import java.util.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The Zeroconf implementation of a sip-communicator AccountID
 *
 * @author Christian Vincenot
 */
public class ZeroconfAccountID
    extends AccountID
{
    /* Firstname, lastname, mail address */
    String first = null;
    String last = null;
    String mail = null;
    
    private boolean rememberContacts = false;
    
    /**
     * Creates a zeroconf account id from the specified id and account 
     * properties.
     * @param userID id identifying this account
     * @param accountProperties any other properties necessary for the account.
     */
    ZeroconfAccountID(String userID, Map accountProperties)
    {
        super(userID
              , accountProperties
              , "Zeroconf"
              , "zeroconf.org");
        
        first = (String)accountProperties.get("first");
        last = (String)accountProperties.get("last");
        mail = (String)accountProperties.get("mail");
        
        rememberContacts = 
            new Boolean((String)accountProperties.get("rememberContacts"))
                .booleanValue();
    }

    /**
     * Returns a String representing the firstname of this user.
     * @return String representing the firstname of this user.
     */
    public String getFirst()
    {
        return first;
    }

    /**
     * Returns a String representing the lastname of this user.
     * @return String representing the lastname of this user.
     */
    public String getLast()
    {
        return last;
    }

    /**
     * Returns a String representing the mail address of this user.
     * @return String representing the mail address of this user.
     */
    public String getMail()
    {
        return mail;
    }

    /**
     * Returns a boolean indicating if we store the contacts we meet or not.
     * @return boolean indicating if we store the contacts we meet or not.
     */
    public boolean isRememberContacts()
    {
        return rememberContacts;
    }
}
