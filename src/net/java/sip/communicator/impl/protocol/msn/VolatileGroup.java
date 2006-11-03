/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.msn;

import net.sf.jml.*;

/**
 * The Msn implementation of the Volatile ContactGroup interface.
 *
 * @author Damian Minkov
 */
public class VolatileGroup
    implements MsnGroup
{
    private String groupName = new String("NotInContactList");

    public MsnContactList getContactList(){return null;}

    public String getGroupId()
    {
        return groupName;
    }

    public String getGroupName()
    {
        return getGroupId();
    }

    public MsnContact[] getContacts(){return null;}

    public boolean containContact(MsnContact contact){return false;}

    public boolean isDefaultGroup(){return false;}
}
