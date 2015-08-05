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
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;

import net.java.sip.communicator.impl.gui.lookandfeel.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.main.contactlist.contactsource.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.skin.*;

/**
 * DeafultContactlist used to display <code>JList</code>s with contacts.
 *
 * @author Damian Minkov
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public class DefaultTreeContactList
    extends JTree
    implements Skinnable
{
    private static final long serialVersionUID = 0L;

    /**
     * Class id key used in UIDefaults.
     */
    private static final String uiClassID =
        DefaultTreeContactList.class.getName() +  "TreeUI";

    /**
     * Adds the ui class to UIDefaults.
     */
    static
    {
        UIManager.getDefaults().put(uiClassID,
            ExtendedTreeUI.class.getName());
    }

    /**
     * The cached selection event.
     */
    private TreeSelectionEvent myCachedSelectionEvent;

    /**
     * The tree cell renderer.
     */
    private ContactListTreeCellRenderer renderer;

    /**
     * Creates an instance of <tt>DefaultContactList</tt>.
     */
    public DefaultTreeContactList()
    {
        this.setBackground(Color.WHITE);
        this.setDragEnabled(true);
        this.setTransferHandler(new ContactListTransferHandler(this));
        this.getSelectionModel().
            setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        renderer = new ContactListTreeCellRenderer();
        this.setCellRenderer(renderer);

        ToolTipManager.sharedInstance().registerComponent(this);

        // By default 2 successive clicks are need to begin dragging.
        // Workaround provided by simon@tardell.se on 29-DEC-2002 for bug 4521075
        // http://bugs.sun.com/bugdatabase/view_bug.do;jsessionid=a13e98ab2364524506eb91505565?bug_id=4521075
        // "Drag gesture in JAVA different from Windows". The bug is also noticed
        // on Mac Leopard.
        addMouseListener(new MouseAdapter()
        {
            @Override
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
     * Returns the currently selected object in the contact list. If there's
     * no selection, returns null.
     *
     * @return the currently selected object
     */
    public Object getSelectedValue()
    {
        TreePath selectionPath = getSelectionPath();

        if (selectionPath != null)
            return selectionPath.getLastPathComponent();

        return null;
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
        UIContact uiContact;
        synchronized (metaContact)
        {
            uiContact = MetaContactListSource.getUIContact(metaContact);
        }

        if (uiContact != null)
            return isContactActive(uiContact);
        return false;
    }

    /**
     * Creates a customized tooltip for this contact list.
     *
     * @return The customized tooltip.
     */
    @Override
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
            Component cellComponent
                = findTreeCellComponent(currentMouseLocation);

            // If we're over a button we show the button tool tip.
            if (cellComponent instanceof SIPCommButton
                && ( (((SIPCommButton) cellComponent).getToolTipText() != null
                        && ((SIPCommButton) cellComponent)
                            .getToolTipText().length() > 0)
                    || ((SIPCommButton) cellComponent).getTooltip() != null))
            {
                SIPCommButton button = (SIPCommButton)cellComponent;

                tip = button.getTooltip();
                if(tip == null)
                {
                    tip = new ExtendedTooltip(true);
                    tip.setTitle(button.getToolTipText());
                }
            }
            else
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
        }
        else if (element instanceof GroupNode)
        {
            Component cellComponent
                = findTreeCellComponent(currentMouseLocation);

            // If we're over a button we show the button tool tip.
            if (cellComponent instanceof SIPCommButton
                && ((SIPCommButton) cellComponent).getToolTipText() != null
                && ((SIPCommButton) cellComponent)
                    .getToolTipText().length() > 0)
            {
                tip = new ExtendedTooltip(true);
                tip.setTitle(((SIPCommButton)cellComponent).getToolTipText());
            }
            else
            {
                UIGroup group
                    = ((GroupNode) element).getGroupDescriptor();

                tip = new ExtendedTooltip(true);
                tip.setTitle(group.getDisplayName());
            }
        }
        else if (element instanceof ChatContact<?>)
        {
            ChatContact<?> chatContact = (ChatContact<?>) element;

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

            String buttonString = "";

            Component mouseComponent = findTreeCellComponent(event.getPoint());

            if (mouseComponent instanceof SIPCommButton)
                buttonString = Integer.toString(mouseComponent.hashCode());

            String uniqueToolTipString = "className= "
                                            + element.getClass().getName()
                                            + ", hashCode= "
                                            + element.hashCode()
                                            + ", toString= "
                                            + element.toString();
            /*
             * Since it does not seem obvious how to get a unique String ID for
             * element even after converting to ContactNode, GroupNode or
             * ChatContact, just make up a String which is very likely to be
             * different for the different nodes in this JTree.
             */
            return  (buttonString.length() > 0)
                    ? uniqueToolTipString
                        + ", onButton="
                        + buttonString
                    : uniqueToolTipString;
        }
        return null;
    }

    /**
     * Reloads renderer resources for this tree.
     */
    public void loadSkin()
    {
        renderer.loadSkin();
    }

    /**
     * Returns the name of the L&F class that renders this component.
     *
     * @return the string "TreeUI"
     * @see JComponent#getUIClassID
     * @see UIDefaults#getUI
     */
    @Override
    public String getUIClassID()
    {
        return uiClassID;
    }

    protected Component findTreeCellComponent(Point point)
    {
        TreePath path = getClosestPathForLocation(point.x, point.y);

        if (path == null)
            return null;

        Object element = path.getLastPathComponent();

        ContactListTreeCellRenderer renderer
            = (ContactListTreeCellRenderer)
                getCellRenderer().getTreeCellRendererComponent(
                        this,
                        element,
                        true,
                        true,
                        true,
                        getRowForPath(path),
                        true);

        // We need to translate coordinates here.
        Rectangle r = this.getPathBounds(path);
        int translatedX = point.x - r.x;
        int translatedY = point.y - r.y;

        return renderer.findComponentAt(translatedX, translatedY);
    }
}
