
/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.msn;

import net.sf.jml.*;

/**
 * The Msn implementation for Volatile Contact
 * @author Damian Minkov
 */
public class VolatileContact
    implements MsnContact
{
    private final String contactId;
    private final String displayName;
    private Email email;

    VolatileContact(String id, Email email, String displayName)
    {
        this.contactId = id;
        this.email = email;
        this.displayName = displayName;
    }
    
    VolatileContact(String id)
    {
        this(id, null, id);
    }

    public MsnContactList getContactList(){return null;}

    public String getId()
    {
        return contactId;
    }

    public String getFriendlyName()
    {
        return displayName;
    }

    public boolean isInList(MsnList msnList){return false;}

    public MsnGroup[] getBelongGroups(){return null;}

    public boolean belongGroup(MsnGroup msnGroup){return false;}

    public Email getEmail()
    {
        if (email == null)
            email = Email.parseStr(contactId);
        return email;
    }

    public String getDisplayName(){return displayName;}

    public MsnUserStatus getStatus(){return null;}

    public MsnClientId getClientId(){return null;}

    public MsnUserProperties getProperties(){return null;}

    public String getOldDisplayName(){return "";}

    public MsnUserStatus getOldStatus(){return null;}

    public String getPersonalMessage(){return "";}
    
    public MsnObject getAvatar(){return null;}
}
