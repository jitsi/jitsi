/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
