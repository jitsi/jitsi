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
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * Events of this class indicate a change in one of the properties of a
 * ServerStoredGroup.
 *
 * @author Emil Ivov
 */
public class ServerStoredGroupEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Indicates that a contact group has been successfully created.
     */
    public static final int GROUP_CREATED_EVENT = 1;

    /**
     * Indicates that a contact group has been successfully deleted.
     */
    public static final int GROUP_REMOVED_EVENT = 2;

    /**
     * Indicates that a group has been successfully renamed.
     */
    public static final int GROUP_RENAMED_EVENT = 3;

    /**
     * Indicates that a group has just been resolved against the server.
     */
    public static final int GROUP_RESOLVED_EVENT = 4;

    /**
     * Indicates that we have failed to create a group.
     */
    public static final int GROUP_CREATION_FAILED_EVENT = 5;


    private int eventID = -1;
    private ProtocolProviderService sourceProvider = null;
    private OperationSetPersistentPresence parentOperationSet = null;
    private ContactGroup parentGroup = null;


    /**
     * Creates a ServerStoredGroupChangeEvent instance.
     * @param sourceGroup the group that this event is pertaining to.
     * @param eventID an int describing the cause of the event
     * @param parentGroup the group that the source group is a child of.
     * @param sourceProvider a reference to the protocol provider where this is
     * happening
     * @param opSet a reference to the operation set responsible for the event
     */
    public ServerStoredGroupEvent(ContactGroup sourceGroup,
                                  int eventID,
                                  ContactGroup parentGroup,
                                  ProtocolProviderService sourceProvider,
                                  OperationSetPersistentPresence opSet)
    {
        super(sourceGroup);

        this.eventID = eventID;
        this.sourceProvider = sourceProvider;
        this.parentOperationSet = opSet;
        this.parentGroup = parentGroup;
    }

    /**
     * Returns a reference to the <tt>ContactGroup</tt> that this event is
     * pertaining to.
     * @return a reference to the ContactGroup that caused the event.
     */
    public ContactGroup getSourceGroup()
    {
        return (ContactGroup)getSource();
    }

    /**
     * Returns an int describing the cause of this event.
     * @return an int describing the cause of this event.
     */
    public int getEventID()
    {
        return eventID;
    }

    /**
     * Returns a reference to the provider under which the event is being
     * generated
     * @return a ProtocolProviderService instance indicating the provider
     * responsible for the event.
     */
    public ProtocolProviderService getSourceProvider()
    {
        return this.sourceProvider;
    }

    /**
     * Returns a reference to the operation set that generated the event
     * @return a reference to an OperationSetPersistentPresence instance,
     * responsible for generating the event.
     */
    public OperationSetPersistentPresence getSourceOperationSet()
    {
        return this.parentOperationSet;
    }

    /**
     * Returns the group containing the event source group
     * @return a reference to the <tt>ContactGroup</tt> instance that is parent
     * of the <tt>ContactGroup</tt> which is the source of this event.
     */
    public ContactGroup getParentGroup()
    {
        return parentGroup;
    }

    /**
     * Returns a String representation of this event.
     * @return a String containing details describin this event.
     */
    @Override
    public String toString()
    {
        StringBuffer buff
            = new StringBuffer("ServerStoredGroupEvent:[EventID= ");
        buff.append(getEventID());
        buff.append(" SourceGroup=");
        buff.append(getSource());
        buff.append("]");

        return buff.toString();
    }
}
