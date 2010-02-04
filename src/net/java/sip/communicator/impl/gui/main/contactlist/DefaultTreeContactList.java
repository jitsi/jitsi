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
import javax.swing.event.*;
import javax.swing.tree.*;

import net.java.sip.communicator.impl.gui.lookandfeel.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * DeafultContactlist used to display <code>JList</code>s with contacts.
 *
 * @author Damian Minkov
 * @author Yana Stamcheva
 */
public class DefaultTreeContactList
    extends JTree
{
    private static final long serialVersionUID = 0L;

    /**
     * The cached selection event.
     */
    private TreeSelectionEvent myCachedSelectionEvent;

    /**
     * Creates an instance of <tt>DefaultContactList</tt>.
     */
    public DefaultTreeContactList()
    {
        this.setUI(new SIPCommTreeUI());
        this.setBackground(Color.WHITE);
        this.setDragEnabled(true);
        this.setTransferHandler(new ContactListTransferHandler(this));
        this.setCellRenderer(new ContactListTreeCellRenderer());
        ToolTipManager.sharedInstance().registerComponent(this);

        // By default 2 successive clicks are need to begin dragging.
        // Workaround provided by simon@tardell.se on 29-DEC-2002 for bug 4521075
        // http://bugs.sun.com/bugdatabase/view_bug.do;jsessionid=a13e98ab2364524506eb91505565?bug_id=4521075
        // "Drag gesture in JAVA different from Windows". The bug is also noticed
        // on Mac Leopard.
        addMouseListener(new MouseAdapter()
        {
            public void mouseReleased(MouseEvent e)
            {
                if(myCachedSelectionEvent == null)
                    return;

                DefaultTreeContactList.super
                    .fireValueChanged(myCachedSelectionEvent);

                myCachedSelectionEvent = null;
            }
        });
    }

    /**
     * Checks if the given contact is currently active.
     * Dummy method used and overridden from classes extending this
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
     * Returns the general status of the given MetaContact. Detects the status
     * using the priority status table. The priority is defined on the
     * "availability" factor and here the most "available" status is returned.
     *
     * @param metaContact The metaContact for which the status is asked.
     * @return PresenceStatus The most "available" status from all subcontact
     *         statuses.
     */
    public PresenceStatus getMetaContactStatus(MetaContact metaContact)
    {
        PresenceStatus status = null;
        Iterator<Contact> i = metaContact.getContacts();
        while (i.hasNext()) {
            Contact protoContact = i.next();
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

        TreePath path = this.getClosestPathForLocation(
            currentMouseLocation.x, currentMouseLocation.y);

        Object element = path.getLastPathComponent();

        ExtendedTooltip tip = new ExtendedTooltip(true);
        if (element instanceof ContactNode)
        {
            MetaContact metaContact = ((ContactNode) element).getMetaContact();

            byte[] avatarImage = metaContact.getAvatar();

            if (avatarImage != null && avatarImage.length > 0)
                tip.setImage(new ImageIcon(avatarImage));

            tip.setTitle(metaContact.getDisplayName());

            Iterator<Contact> i = metaContact.getContacts();

            String statusMessage = null;
            Contact protocolContact;
            while (i.hasNext())
            {
                protocolContact = i.next();

                ImageIcon protocolStatusIcon
                    = new ImageIcon(
                        protocolContact.getPresenceStatus().getStatusIcon());

                String contactAddress = protocolContact.getAddress();
                //String statusMessage = protocolContact.getStatusMessage();

                tip.addLine(protocolStatusIcon, contactAddress);

                // Set the first found status message.
                if (statusMessage == null
                    && protocolContact.getStatusMessage() != null
                    && protocolContact.getStatusMessage().length() > 0)
                    statusMessage = protocolContact.getStatusMessage();
            }

            if (statusMessage != null)
                tip.setBottomText(statusMessage);
        }
        else if (element instanceof GroupNode)
        {
            MetaContactGroup metaGroup
                = ((GroupNode) element).getMetaContactGroup();

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
}
