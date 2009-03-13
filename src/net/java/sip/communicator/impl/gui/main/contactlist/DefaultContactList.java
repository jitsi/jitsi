/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * DeafultContactlist used to display Jlists with contacts.
 *
 * @author Damian Minkov
 */
public class DefaultContactList
    extends JList
{
    public DefaultContactList()
    {
        this.setOpaque(false);

        this.getSelectionModel().setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);

        this.setCellRenderer(new ContactListCellRenderer());
    }

    /**
     * Checks if the given contact is currently active.
     * Dummy method used and overrided from classes extending this
     * functionality such as ContactList.
     *
     * @param metaContact the <tt>MetaContact</tt> to verify
     * @return TRUE if the given <tt>MetaContact</tt> is active, FALSE -
     * otherwise
     */
    public boolean isMetaContactActive(MetaContact metaContact)
    {
        return false;
    }

    /**
     * Checks whether the group is closed.
     * Dummy method used and overrided from classes extending this
     * functionality such as ContactList.
     *
     * @param group The group to check.
     * @return True if the group is closed, false - otherwise.
     */
    public boolean isGroupClosed(MetaContactGroup group)
    {
        return false;
    }

    /**
     * Returns the general status of the given MetaContact. Detects the status
     * using the priority status table. The priority is defined on the
     * "availablity" factor and here the most "available" status is returned.
     *
     * @param metaContact The metaContact fot which the status is asked.
     * @return PresenceStatus The most "available" status from all subcontact
     *         statuses.
     */
    public PresenceStatus getMetaContactStatus(MetaContact metaContact)
    {
        PresenceStatus status = null;
        Iterator i = metaContact.getContacts();
        while (i.hasNext()) {
            Contact protoContact = (Contact) i.next();
            PresenceStatus contactStatus = protoContact.getPresenceStatus();

            if (status == null) {
                status = contactStatus;
            } else {
                status = (contactStatus.compareTo(status) > 0) ? contactStatus
                        : status;
            }
        }
        return status;
    }

    /**
     * Creates a customized tooltip for this contact list.
     *
     * @return The customized tooltip.
     */
    public JToolTip createToolTip()
    {
        Point currentMouseLocation = MouseInfo.getPointerInfo().getLocation();

        SwingUtilities.convertPointFromScreen(currentMouseLocation, this);

        int index = this.locationToIndex(currentMouseLocation);

        Object element = getModel().getElementAt(index);

        ExtendedTooltip tip = new ExtendedTooltip();
        if (element instanceof MetaContact)
        {
            MetaContact metaContact = (MetaContact) element;

            byte[] avatarImage = metaContact.getAvatar();

            if (avatarImage != null && avatarImage.length > 0)
                tip.setImage(new ImageIcon(avatarImage));

            tip.setTitle(metaContact.getDisplayName());

            Iterator<Contact> i = metaContact.getContacts();

            while (i.hasNext())
            {
                Contact protocolContact = (Contact) i.next();

                Image protocolStatusIcon
                    = ImageLoader.getBytesInImage(
                            protocolContact.getPresenceStatus().getStatusIcon());

                String contactAddress = protocolContact.getAddress();
                //String statusMessage = protocolContact.getStatusMessage();

                tip.addLine(new ImageIcon(protocolStatusIcon),
                                        contactAddress);
            }
        }
        else if (element instanceof MetaContactGroup)
        {
            MetaContactGroup metaGroup = (MetaContactGroup) element;

            tip.setTitle(metaGroup.getGroupName());
        }
        else if (element instanceof ChatContact)
        {
            ChatContact chatContact = (ChatContact) element;

            ImageIcon avatarImage = chatContact.getAvatar();

            if (avatarImage != null)
                tip.setImage(avatarImage);

            tip.setTitle(chatContact.getName());
        }

        tip.setComponent(this);

        return tip;
    }

    /**
     * Returns the string to be used as the tooltip for <i>event</i>. We don't
     * really use this string, but we need to return different string each time
     * in order to make the TooltipManager change the tooltip over the different
     * cells in the JList.
     *
     * @return the string to be used as the tooltip for <i>event</i>.
     */
    public String getToolTipText(MouseEvent event)
    {
        Point currentMouseLocation = event.getPoint();

        int index = this.locationToIndex(currentMouseLocation);

        // If the index is equals to -1, then we have nothing to do here, we
        // just return null.
        if (index == -1)
            return null;

        Object element = getModel().getElementAt(index);

        if (element instanceof MetaContact)
        {
            MetaContact metaContact = (MetaContact) element;

            return metaContact.getDisplayName();
        }
        else if (element instanceof MetaContactGroup)
        {
            MetaContactGroup metaGroup = (MetaContactGroup) element;

            return metaGroup.getGroupName();
        }
        else if (element instanceof ChatContact)
        {
            ChatContact chatContact = (ChatContact) element;

            return chatContact.getName();
        }

        return null;
    }
}
