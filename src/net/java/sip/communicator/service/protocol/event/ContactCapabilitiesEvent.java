/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * Represents an event/<tt>EventObject</tt> fired by
 * <tt>OperationSetClientCapabilities</tt> in order to notify about changes in
 * the list of the <tt>OperationSet</tt> capabilities of a <tt>Contact</tt>.
 *
 * @author Lubomir Marinov
 */
public class ContactCapabilitiesEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The ID of the <tt>ContactCapabilitiesEvent</tt> which notifies about
     * changes in the list of the <tt>OperationSet</tt> capabilities of a
     * <tt>Contact</tt>.
     */
    public static final int SUPPORTED_OPERATION_SETS_CHANGED = 1;

    /**
     * The ID of this event which indicates the specifics of the change in the
     * list of <tt>OperationSet</tt> capabilities of the associated
     * <tt>Contact</tt> and the details this event carries.
     */
    private final int eventID;

    /**
     * Initializes a new <tt>ContactCapabilitiesEvent</tt> instance which is to
     * notify about a specific change in the list of <tt>OperationSet</tt>
     * capabilities of a specific <tt>Contact</tt>.
     *
     * @param sourceContact the <tt>Contact</tt> which is to be considered the
     * source/cause of the new event
     * @param eventID the ID of the new event which indicates the specifics of
     * the change in the list of <tt>OperationSet</tt> capabilities of the
     * specified <tt>sourceContact</tt> and the details to be carried by the new
     * event
     */
    public ContactCapabilitiesEvent(Contact sourceContact, int eventID)
    {
        super(sourceContact);

        this.eventID = eventID;
    }

    /**
     * Gets the ID of this event which indicates the specifics of the change in
     * the list of <tt>OperationSet</tt> capabilities of the associated
     * <tt>sourceContact</tt> and the details it carries.
     *
     * @return the ID of this event which indicates the specifics of the change
     * in the list of <tt>OperationSet</tt> capabilities of the associated
     * <tt>sourceContact</tt> and the details it carries
     */
    public int getEventID()
    {
        return eventID;
    }

    /**
     * Gets the <tt>Contact</tt> which is the source/cause of this event i.e.
     * which has changed its list of <tt>OperationSet</tt> capabilities.
     *
     * @return the <tt>Contact</tt> which is the source/cause of this event
     */
    public Contact getSourceContact()
    {
        return (Contact) getSource();
    }
}
