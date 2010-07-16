/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;

import net.java.sip.communicator.impl.gui.lookandfeel.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.main.contactlist.contactsource.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;

/**
 * DeafultContactlist used to display <code>JList</code>s with contacts.
 *
 * @author Damian Minkov
 * @author Yana Stamcheva
 * @author Lubomir Marinov
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
     * @param contact the <tt>MetaContact</tt> to verify
     * @return TRUE if the given <tt>MetaContact</tt> is active, FALSE -
     * otherwise
     */
    public boolean isContactActive(UIContact contact)
    {
        return false;
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
    public boolean isContactActive(MetaContact metaContact)
    {
        UIContact uiContact = MetaContactListSource.getUIContact(metaContact);
        if (uiContact != null)
            return isContactActive(uiContact);
        return false;
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

        ExtendedTooltip tip = null;
        if (element instanceof ContactNode)
        {
            UIContact contact
                = ((ContactNode) element).getContactDescriptor();

            tip = contact.getToolTip();
            if (tip == null)
            {
                tip = new ExtendedTooltip(true);
                tip.setTitle(contact.getDisplayName());
            }
        }
        else if (element instanceof GroupNode)
        {
            UIGroup group
                = ((GroupNode) element).getGroupDescriptor();

            tip = new ExtendedTooltip(true);
            tip.setTitle(group.getDisplayName());
        }
        else if (element instanceof ChatContact)
        {
            ChatContact chatContact = (ChatContact) element;

            ImageIcon avatarImage = chatContact.getAvatar();

            tip = new ExtendedTooltip(true);
            if (avatarImage != null)
                tip.setImage(avatarImage);

            tip.setTitle(chatContact.getName());
        }

        tip.setComponent(this);

        return tip;
    }

    /**
     * Gets the <tt>String</tt> to be used as the tool tip text for the mouse
     * location given by a specific <tt>MouseEvent</tt>.
     * <tt>DefaultTreeContactList</tt> only overrides in order to return a
     * different <tt>String</tt> each time in order to make
     * <tt>TooltipManager</tt> change the tool tip over the different nodes of
     * this <tt>JTree</tt>.
     *
     * @param event the <tt>MouseEvent</tt> which gives the mouse location to
     * get the tool tip text for
     * @return the <tt>String</tt> to be used as the tool tip text for the mouse
     * location given by the specified <tt>MouseEvent</tt>
     */
    @Override
    public String getToolTipText(MouseEvent event)
    {
        TreePath path = getClosestPathForLocation(event.getX(), event.getY());

        if (path != null)
        {
            Object element = path.getLastPathComponent();

            /*
             * Since it does not seem obvious how to get a unique String ID for
             * element even after converting to ContactNode, GroupNode or
             * ChatContact, just make up a String which is very likely to be
             * different for the different nodes in this JTree.
             */
            return
                "className= "
                    + element.getClass().getName()
                    + ", hashCode= "
                    + element.hashCode()
                    + ", toString= "
                    + element.toString();
        }
        return null;
    }
}
