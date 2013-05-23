/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.gui.event;

import java.util.*;

import net.java.sip.communicator.service.gui.*;

/**
 * The <tt>ContactListEvent</tt> is triggered when a contact or a group is
 * clicked in the contact list.
 * @author Yana Stamcheva
 */
public class ContactListEvent
    extends EventObject
{
    private int eventID = -1;

    /**
     * Indicates that the ContactListEvent instance was triggered by
     * selecting a contact in the contact list.
     */
    public static final int CONTACT_CLICKED = 1;

    /**
     * Indicates that the ContactListEvent instance was triggered by selecting
     * a group in the contact list.
     */
    public static final int GROUP_CLICKED = 2;

    /**
     * Indicates that the ContactListEvent instance was triggered by
     * selecting a contact in the contact list.
     */
    public static final int CONTACT_SELECTED = 3;

    /**
     * Indicates that the ContactListEvent instance was triggered by selecting
     * a group in the contact list.
     */
    public static final int GROUP_SELECTED = 4;

    /**
     * Indicated the number of click accompanying the event
     */
    private int clickCount;

    /**
     * Creates a new ContactListEvent according to the specified parameters.
     * @param source the MetaContact which was selected
     * @param eventID one of the XXX_SELECTED static fields indicating the
     * nature of the event.
     * @param clickCount the number of clicks that was produced when clicking
     * over the contact list
     */
    public ContactListEvent(Object source, int eventID, int clickCount)
    {
        super(source);

        this.eventID = eventID;
        this.clickCount = clickCount;
    }

    /**
     * Returns an event id specifying whether the type of this event
     * (CONTACT_SELECTED or PROTOCOL_CONTACT_SELECTED)
     * @return one of the XXX_SELECTED int fields of this class.
     */
    public int getEventID()
    {
        return eventID;
    }

    /**
     * Returns the <tt>UIContactDescriptor</tt> for which this event occured.
     * @return the </tt>UIContactDescriptor</tt> for which this event occured
     */
    public UIContact getSourceContact()
    {
        if(getSource() instanceof UIContact)
            return (UIContact) getSource();

        return null;
    }

    /**
     * Returns the <tt>UIGroupDescriptor</tt> for which this event occured.
     * @return the <tt>UIGroupDescriptor</tt> for which this event occured
     */
    public UIGroup getSourceGroup()
    {
        if(getSource() instanceof UIGroup)
            return (UIGroup) getSource();

        return null;
    }

    /**
     * Returns the number of click of this event.
     * @return the number of click of this event.
     */
    public int getClickCount()
    {
        return clickCount;
    }
}
