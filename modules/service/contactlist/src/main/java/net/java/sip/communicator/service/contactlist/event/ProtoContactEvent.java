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

import java.beans.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * Event delivered upon addition, removal or change of a protocol specific
 * contact inside an existing meta contact.
 *
 * @author Emil Ivov
 */
public class ProtoContactEvent
    extends PropertyChangeEvent
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Indicates that the MetaContactEvent instance was triggered by the
     * removal of a protocol specific contact from an existing MetaContact.
     */
    public static final String PROTO_CONTACT_REMOVED = "ProtoContactRemoved";

    /**
     * Indicates that the MetaContactEvent instance was triggered by the
     * a protocol specific contact to a new MetaContact parent.
     */
    public static final String PROTO_CONTACT_ADDED = "ProtoContactAdded";

    /**
     * Indicates that the MetaContactEvent instance was triggered by moving
     * addition of a protocol specific contact to an existing MetaContact.
     */
    public static final String PROTO_CONTACT_MOVED = "ProtoContactMoved";

    /**
     * Indicates that this event instance was triggered by changing a protocol
     * specific contact in some way.
     */
    public static final String PROTO_CONTACT_MODIFIED = "ProtoContactModified";

    /**
     * Creates an instance of this <tt>ProtoContactEvent</tt>.
     * @param source the proto <tt>Contact</tt> that this event is about.
     * @param eventName the name of the event, one of the PROTO_CONTACT_XXX
     * fields.
     * @param oldParent the <tt>MetaContact</tt> that was parent of the source
     * contact before the event occurred or null for a new contact or when
     * irrelevant.
     * @param newParent the <tt>MetaContact</tt> that is parent of the source
     * contact after the event occurred or null for a removed contact or when
     * irrelevant.
     */
    public ProtoContactEvent(Contact source, String eventName,
                             MetaContact oldParent, MetaContact newParent)
    {
        super(source, eventName, oldParent, newParent);
    }

    /**
     * Returns the protoContact that this event is about.
     * @return he <tt>Contact</tt> that this event is about.
     */
    public Contact getProtoContact()
    {
        return (Contact)getSource();
    }

    /**
     * Returns the <tt>MetaContact</tt> that was parent of the source contact
     * before the event occurred or null for a new contact or when irrelevant.
     *
     * @return the <tt>MetaContact</tt> that was parent of the source contact
     * before the event occurred or null for a new contact or when irrelevant.
     */
    public MetaContact getOldParent()
    {
        return (MetaContact)getOldValue();
    }

    /**
     * Returns the <tt>MetaContact</tt> that is parent of the source contact
     * after the event occurred or null for a removed contact or when irrelevant.
     *
     * @return the <tt>MetaContact</tt> that is parent of the source contact
     * after the event occurred or null for a removed contact or when irrelevant.
     */
    public MetaContact getNewParent()
    {
        return (MetaContact)getNewValue();
    }

    /**
     * Returns the <tt>MetaContact</tt> that is the most relevant parent of
     * the source proto <tt>Contact</tt>. In the case of a moved or newly
     * added <tt>Contact</tt> the method would return same as getNewParent()
     * and would return the contact's old parent in the case of a
     * <tt>PROTO_CONTACT_REMOVED</tt> event.
     * @return  the <tt>MetaContact</tt> that is most apt to be called parent
     * to the source <tt>Contact</tt>.
     */
    public MetaContact getParent()
    {
        return getNewParent() != null
            ? getNewParent()
            : getOldParent();
    }
}
