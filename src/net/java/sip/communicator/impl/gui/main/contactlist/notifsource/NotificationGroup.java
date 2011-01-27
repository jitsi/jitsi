/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist.notifsource;

import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.service.protocol.OperationSetMessageWaiting.MessageType;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

/**
 * The <tt>NotificationGroup</tt> represents a group of notification entries
 * shown in the contact list history view. It could represent the voice
 * messages,.emails, etc.
 *
 * @author Yana Stamcheva
 */
public class NotificationGroup
    implements UIGroup
{
    /**
     * The type of the notification message, identifying this group.
     */
    private final MessageType type;

    /**
     * The corresponding group node in the contact list component.
     */
    private GroupNode groupNode;

    /**
     * A mapping attaching to each <tt>NotificationContact</tt> the
     * corresponding <tt>ProtocolProviderService</tt>, for which notifications
     * are received.
     */
    private final Hashtable<ProtocolProviderService, NotificationContact>
        contacts
            = new Hashtable<ProtocolProviderService, NotificationContact>();

    /**
     * The group of UI notifications.
     */
    private static UINotificationGroup uiNotificationGroup;

    /**
     * Creates an instance of <tt>NotificationGroup</tt> by specifying the
     * message type.
     *
     * @param type the type of messages that this group would contain
     */
    public NotificationGroup(MessageType type)
    {
        this.type = type;
    }

    /**
     * Returns the descriptor of the group. This would be the underlying object
     * that should provide all other necessary information for the group.
     *
     * @return the descriptor of the group
     */
    public Object getDescriptor()
    {
        return type;
    }

    /**
     * The display name of the group. The display name is the name to be shown
     * in the contact list group row.
     *
     * @return the display name of the group
     */
    public String getDisplayName()
    {
        String displayName;
        if (type.equals(MessageType.VOICE))
            displayName = GuiActivator.getResources()
                .getI18NString("service.gui.VOICEMAIL_TITLE");
        else
            displayName = type.toString();

        return displayName;
    }

    /**
     * Returns the index of this group in its source. In other words this is
     * the descriptor index.
     *
     * @return the index of this group in its source
     */
    public int getSourceIndex()
    {
        return 0;
    }

    /**
     * Returns null to indicate that the parent group is the root group.
     *
     * @return null
     */
    public UIGroup getParentGroup()
    {
        return null;
    }

    /**
     * Returns <tt>false</tt> to indicate that this group is never collapsed.
     *
     * @return <tt>false</tt>
     */
    public boolean isGroupCollapsed()
    {
        return false;
    }

    /**
     * Returns the count of online child contacts.
     *
     * @return the count of online child contacts
     */
    public int countOnlineChildContacts()
    {
        return contacts.size();
    }

    /**
     * Returns the child contacts count.
     *
     * @return child contacts count
     */
    public int countChildContacts()
    {
        return contacts.size();
    }

    /**
     * Returns the identifier of this group.
     *
     * @return the identifier of this group
     */
    public String getId()
    {
        return null;
    }

    /**
     * Returns the <tt>GroupNode</tt> corresponding to this <tt>UIGroup</tt>.
     * The is the actual node used in the contact list component data model.
     *
     * @return the <tt>GroupNode</tt> corresponding to this <tt>UIGroup</tt>
     */
    public GroupNode getGroupNode()
    {
        return groupNode;
    }

    /**
     * Sets the <tt>GroupNode</tt> corresponding to this <tt>UIGroup</tt>.
     *
     * @param groupNode the <tt>GroupNode</tt> to set. The is the actual
     * node used in the contact list component data model.
     */
    public void setGroupNode(GroupNode groupNode)
    {
        this.groupNode = groupNode;
    }

    /**
     * Returns null to indicate that there's no right button menu provided for
     * this group.
     *
     * @return null
     */
    public JPopupMenu getRightButtonMenu()
    {
        return null;
    }

    /**
     * Returns an <tt>Iterator</tt> over a list of all notification contacts
     * contained in this group.
     *
     * @return an <tt>Iterator</tt> over a list of all notification contacts
     * contained in this group
     */
    public Iterator<? extends UIContact> getNotifications()
    {
        return contacts.values().iterator();
    }

    /**
     * Returns the message type indicating the identity of this group.
     *
     * @return the message type corresponding to this group
     */
    public MessageType getMessageType()
    {
        return type;
    }

    /**
     * Creates all necessary notification contacts coming from the given
     * <tt>MessageWaitingEvent</tt>.
     *
     * @param event the <tt>MessageWaitingEvent</tt> that notified us
     */
    public void messageWaitingNotify(MessageWaitingEvent event)
    {
        ProtocolProviderService protocolProvider = event.getSourceProvider();

        NotificationContact contact = contacts.get(protocolProvider);

        TreeContactList contactList = GuiActivator.getContactList();

        boolean isNew = false;
        if (contact == null)
        {
            contact = new NotificationContact(this, protocolProvider);
            contacts.put(protocolProvider, contact);

            isNew = true;
        }

        contact.setMessageAccount(event.getAccount());
        contact.setUnreadMessageCount(event.getUnreadMessages());
        contact.setReadMessageCount(event.getReadMessages());

        if (contactList.getCurrentFilter().isMatching(contact))
        {
            if (isNew)
                contactList.addContact(contact, this, true, true);
            else
                contactList.refreshContact(contact);
        }

        if (contact.getUnreadMessageCount() > 0)
        {
            if (uiNotificationGroup == null)
                uiNotificationGroup
                    = new UINotificationGroup(
                        getDisplayName(),
                        GuiActivator.getResources()
                            .getI18NString("service.gui.VOICEMAIL_TOOLTIP"));

            UINotificationManager.addNotification(
                new UINotification(
                    contact.getDisplayName()
                    + " : " + contact.getDisplayDetails() ,
                    new Date(),
                    uiNotificationGroup));
        }
    }
}
