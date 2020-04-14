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
package net.java.sip.communicator.service.contactlist.event;

import java.util.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;

/**
 *
 * @author Yana Stamcheva
 */
public class MetaContactGroupEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * ID of the event.
     */
    private int eventID = -1;

    /**
     * Indicates that the MetaContactGroupEvent instance was triggered by
     * adding a MetaContactGroup.
     */
    public static final int META_CONTACT_GROUP_ADDED = 1;

    /**
     * Indicates that the MetaContactGroupEvent instance was triggered by the
     * removal of an existing MetaContactGroup.
     */
    public static final int META_CONTACT_GROUP_REMOVED = 2;

    /**
     * Indicates that the MetaContactGroupEvent instance was triggered by the
     * removal of a protocol specific ContactGroup in the source
     * MetaContactGroup.
     */
    public static final int CONTACT_GROUP_REMOVED_FROM_META_GROUP = 3;

    /**
     * Indicates that the MetaContactGroupEvent instance was triggered by the
     * fact that child contacts were reordered in the source group.
     */
    public static final int CHILD_CONTACTS_REORDERED = 4;


    /**
     * Indicates that the MetaContactGroupEvent instance was triggered by the
     * renaming of a protocol specific ContactGroup in the source
     * MetaContactGroup. Note that this does not in any way mean that the
     * name of the MetaContactGroup itslef has changed. <tt>MetaContactGroup</tt>s
     * contain multiple protocol groups and their name cannot change each time
     * one of them is renamed.
     */
    public static final int CONTACT_GROUP_RENAMED_IN_META_GROUP = 5;

    /**
     * Indicates that the MetaContactGroupEvent instance was triggered by adding
     * a protocol specific ContactGroup to the source MetaContactGroup.
     */
    public static final int CONTACT_GROUP_ADDED_TO_META_GROUP = 6;

    /**
     * Indicates that the MetaContactGroupEvent instance was triggered by the
     * renaming of an existing MetaContactGroup.
     */
    public static final int META_CONTACT_GROUP_RENAMED = 7;

    /**
     * the ProtocolProviderService instance where this event
     * occurred.
     */
    private ProtocolProviderService sourceProvider = null;

    /**
     * The proto group associated with this event.
     */
    private ContactGroup sourceProtoGroup = null;

    /**
     * Creates a new MetaContactGroup event according to the specified parameters.
     * @param source the MetaContactGroup instance that is added to the MetaContactList
     * @param provider the ProtocolProviderService instance where this event
     * occurred
     * @param sourceProtoGroup the proto group associated with this event or
     * null if the event does not concern a particular source group.
     * @param eventID one of the METACONTACT_XXX static fields indicating the
     * nature of the event.
     */
    public MetaContactGroupEvent( MetaContactGroup source,
                       ProtocolProviderService provider,
                       ContactGroup sourceProtoGroup,
                       int eventID)
    {
        super(source);
        this.sourceProvider = provider;
        this.sourceProtoGroup = sourceProtoGroup;
        this.eventID = eventID;
    }

    /**
     * Returns the provider that the source contact belongs to.
     * @return the provider that the source contact belongs to.
     */
    public ProtocolProviderService getSourceProvider()
    {
        return sourceProvider;
    }

    /**
     * Returns the proto group associated with this event or null if the event
     * does not concern a particular source group.
     *
     * @return the proto group associated with this event or null if the event
     * does not concern a particular source group.
     */
    public ContactGroup getSourceProtoGroup()
    {
        return this.sourceProtoGroup;
    }

    /**
     * Returns the source MetaContactGroup.
     * @return the source MetaContactGroup.
     */
    public MetaContactGroup getSourceMetaContactGroup()
    {
        return (MetaContactGroup)getSource();
    }

    /**
     * Returns a String representation of this MetaContactGroupEvent
     *
     * @return  A String representation of this
     * MetaContactGroupEvent.
     */
    @Override
    public String toString()
    {
        StringBuffer buff
            = new StringBuffer("MetaContactGroupEvent-[ GroupName=");
        buff.append(getSourceMetaContactGroup().getGroupName());
        buff.append(", eventID=").append(getEventID());

        return buff.toString();
    }

    /**
     * Returns an event id specifying whether the type of this event (e.g.
     * METACONTACT_GROUP_ADDED, METACONTACT_GROUP_REMOVED and etc.)
     * @return one of the METACONTACT_GROUP_XXX int fields of this class.
     */
    public int getEventID()
    {
        return eventID;
    }
}
