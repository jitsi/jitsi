/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.contactlist.event;

import java.util.*;

import net.java.sip.communicator.service.contactlist.*;

/**
 * Parent class for meta contact events indicating addition and removal of
 * meta contacts in a meta contact list.
 * @author Yana Stamcheva
 * @author Emil Ivov
 */
public class MetaContactEvent
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
     * Indicates that the MetaContactEvent instance was triggered by
     * adding a MetaContact.
     */
    public static final int META_CONTACT_ADDED = 1;

    /**
     * Indicates that the MetaContactEvent instance was triggered by the
     * removal of an existing MetaContact.
     */
    public static final int META_CONTACT_REMOVED = 2;

    /**
     * Indicates that the MetaContactEvent instance was triggered by moving
     * an existing MetaContact.
     */
    public static final int META_CONTACT_MOVED = 3;

    /**
     * The parent group of the contact.
     */
    private MetaContactGroup  parentGroup = null;

    /**
     * Creates a new MetaContact event according to the specified parameters.
     * @param source the MetaContact instance that is added to the MetaContactList
     * @param parentGroup the MetaContactGroup underwhich the corresponding
     * MetaContact is located
     * @param eventID one of the METACONTACT_XXX static fields indicating the
     * nature of the event.
     */
    public MetaContactEvent( MetaContact source,
                       MetaContactGroup parentGroup,
                       int eventID)
    {
        super(source);
        this.parentGroup = parentGroup;
        this.eventID = eventID;
    }

    /**
     * Returns the source MetaContact.
     * @return the source MetaContact.
     */
    public MetaContact getSourceMetaContact()
    {
        return (MetaContact)getSource();
    }

    /**
     * Returns the MetaContactGroup that the MetaContact belongs to.
     * @return the MetaContactGroup that the MetaContact belongs to.
     */
    public MetaContactGroup getParentGroup()
    {
        return parentGroup;
    }

    /**
     * Returns a String representation of this MetaContactEvent
     *
     * @return  A String representation of this
     * MetaContactListEvent.
     */
    public String toString()
    {
        StringBuffer buff
            = new StringBuffer("MetaContactEvent-[ ContactID=");
        buff.append(getSourceMetaContact().getDisplayName());
        buff.append(", eventID=").append(getEventID());
        if(getParentGroup() != null)
            buff.append(", ParentGroup=").append(getParentGroup().getGroupName());
        return buff.toString();
    }

    /**
     * Returns an event id specifying whether the type of this event (e.g.
     * METACONTACT_ADDED, METACONTACT_REMOVED and etc.)
     * @return one of the METACONTACT_XXX int fields of this class.
     */
    public int getEventID()
    {
        return eventID;
    }
}
