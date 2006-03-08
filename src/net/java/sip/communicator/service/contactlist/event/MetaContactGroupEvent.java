/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.contactlist.event;

import java.util.EventObject;

import net.java.sip.communicator.service.contactlist.MetaContactGroup;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;

/**
 *
 * @author Yana Stamcheva
 */
public class MetaContactGroupEvent
    extends EventObject
{
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
     * renaming of a protocol specific ContactGroup in the source
     * MetaContactGroup. Note that this does not in any way mean that the
     * name of the MetaContactGroup itslef has changed. <tt>MetaContactGroup</tt>s
     * contain multiple protocol groups and their name cannot change each time
     * one of them is renamed.
     */
    public static final int CONTACT_GROUP_RENAMED_IN_META_GROUP = 3;


    /**
     * Indicates that the MetaContactGroupEvent instance was triggered by the
     * renaming of an existing MetaContactGroup.
     */
    public static final int META_CONTACT_GROUP_RENAMED = 4;

    private ProtocolProviderService sourceProvider = null;

    /**
     * Creates a new MetaContactGroup event according to the specified parameters.
     * @param source the MetaContactGroup instance that is added to the MetaContactList
     * @param provider the ProtocolProviderService instance where this event
     * occurred
     * @param eventID one of the METACONTACT_XXX static fields indicating the
     * nature of the event.
     */
    public MetaContactGroupEvent( MetaContactGroup source,
                       ProtocolProviderService provider,
                       int eventID)
    {
        super(source);
        this.sourceProvider = provider;
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
    public int getEventID(){
        return eventID;
    }
}
