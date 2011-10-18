/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist;

import javax.swing.*;

/**
 * The <tt>UIGroup</tt> represents the user interface contact list group.
 *
 * @author Yana Stamcheva
 */
public interface UIGroup
{
    /**
     * Returns the descriptor of the group. This would be the underlying object
     * that should provide all other necessary information for the group.
     *
     * @return the descriptor of the group
     */
    public Object getDescriptor();

    /**
     * The display name of the group. The display name is the name to be shown
     * in the contact list group row.
     *
     * @return the display name of the group
     */
    public String getDisplayName();

    /**
     * Returns the index of this group in its source. In other words this is
     * the descriptor index.
     *
     * @return the index of this group in its source
     */
    public int getSourceIndex();

    /**
     * Returns the parent group.
     *
     * @return the parent group
     */
    public UIGroup getParentGroup();

    /**
     * Indicates if the group is collapsed or expanded.
     *
     * @return <tt>true</tt> to indicate that the group is collapsed,
     * <tt>false</tt> to indicate that it's expanded
     */
    public boolean isGroupCollapsed();

    /**
     * Returns the count of online child contacts.
     *
     * @return the count of online child contacts
     */
    public int countOnlineChildContacts();

    /**
     * Returns the child contacts count.
     *
     * @return child contacts count
     */
    public int countChildContacts();

    /**
     * Returns the identifier of this group.
     *
     * @return the identifier of this group
     */
    public String getId();

    /**
     * Returns the <tt>GroupNode</tt> corresponding to this <tt>UIGroup</tt>.
     * The is the actual node used in the contact list component data model.
     *
     * @return the <tt>GroupNode</tt> corresponding to this <tt>UIGroup</tt>
     */
    public GroupNode getGroupNode();

    /**
     * Sets the <tt>GroupNode</tt> corresponding to this <tt>UIGroup</tt>.
     *
     * @param groupNode the <tt>GroupNode</tt> to set. The is the actual
     * node used in the contact list component data model.
     */
    public void setGroupNode(GroupNode groupNode);

    /**
     * Returns the right button menu for this group.
     *
     * @return the right button menu component for this group
     */
    public JPopupMenu getRightButtonMenu();
}
