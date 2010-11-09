/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist.notifsource;

import java.util.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.service.protocol.OperationSetMessageWaiting.MessageType;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>NotificationContactSource</tt> represents a contact source that would
 * listen for message waiting notifications and would display them in the
 * history view of the contact list.
 *
 * @author Yana Stamcheva
 */
public class NotificationContactSource
    implements MessageWaitingListener
{
    /**
     * A mapping attaching to each <tt>NotificationGroup</tt> the
     * corresponding <tt>MessageType</tt>, for which notifications
     * are received.
     */
    private final Hashtable<MessageType, NotificationGroup> groups
        = new Hashtable<MessageType, NotificationGroup>();

    /**
     * Adds the received waiting message to the corresponding group and contact.
     * Also adds it the <tt>UINotificationManager</tt> that would take care of
     * notifying the user.
     *
     * @param evt the notification event.
     */
    public void messageWaitingNotify(MessageWaitingEvent evt)
    {
        MessageType type = evt.getMessageType();

        NotificationGroup group = groups.get(type);

        if (group == null)
        {
            group = new NotificationGroup(type);
            groups.put(type, group);
        }

        group.messageWaitingNotify(evt);
    }

    /**
     * Returns an <tt>Iterator</tt> over a list of all notification groups
     * contained in this source.
     *
     * @return an <tt>Iterator</tt> over a list of all notification groups
     * contained in this source
     */
    public Iterator<? extends UIGroup> getNotificationGroups()
    {
        return groups.values().iterator();
    }

    /**
     * Returns an <tt>Iterator</tt> over a list of all notification contacts
     * contained in the given group.
     *
     * @param group the group, which notification contacts we're looking for
     * @return an <tt>Iterator</tt> over a list of all notification contacts
     * contained in the given group
     */
    public Iterator<? extends UIContact> getNotifications(UIGroup group)
    {
        if (!(group instanceof NotificationGroup))
            return null;

        NotificationGroup notifGroup = (NotificationGroup) group;

        return notifGroup.getNotifications();
    }
}
