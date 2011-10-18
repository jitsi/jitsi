/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.icq;

import java.util.*;

import net.kano.joustsim.oscar.oscar.service.ssi.*;

/**
 * Used when initializing a volatile group.
 * @author Emil Ivov
 */
class VolatileGroup
    implements MutableGroup
{
    private final String groupName;

    VolatileGroup()
    {
        this(IcqActivator.getResources()
            .getI18NString("service.gui.NOT_IN_CONTACT_LIST_GROUP_NAME"));
    }

    VolatileGroup(String groupName)
    {
        this.groupName = groupName;
    }

    /**
     * Returns the name of this group.
     *
     * @return the name of this group.
     */
    public String getName()
    {
        return groupName;
    }


    public void addGroupListener(GroupListener listener){}
    public List<Buddy> getBuddiesCopy(){return null;}
    public void removeGroupListener(GroupListener listener){}
    public void deleteBuddies(List<Buddy> ingroup){}
    public void addBuddy(String screenname){}
    public void copyBuddies(Collection<? extends Buddy> buddies){}
    public void deleteBuddy(Buddy buddy){}
    public void rename(String newName){}
}
