/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist.contactsource;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;

/**
 * The <tt>MetaUIGroup</tt> is the implementation of the UIGroup for the
 * <tt>MetaContactListService</tt>. This implementation is based on the
 * <tt>MetaContactGroup</tt>.
 *
 * @author Yana Stamcheva
 */
public class MetaUIGroup
    implements UIGroup
{
    /**
     * The <tt>MetaContactGroup</tt>, on which this UI group is based.
     */
    private final MetaContactGroup metaGroup;

    /**
     * The corresponding <tt>GroupNode</tt> in the contact list component data
     * model.
     */
    private GroupNode groupNode;

    /**
     * Creates an instance of <tt>MetaUIGroup</tt> by specifying the underlying
     * <tt>MetaContactGroup</tt>.
     * @param metaGroup the <tt>MetaContactGroup</tt>, on which this UI group
     * is based
     */
    public MetaUIGroup(MetaContactGroup metaGroup)
    {
        this.metaGroup = metaGroup;
    }

    /**
     * Returns the underlying <tt>MetaContactGroup</tt>.
     * @return the underlying <tt>MetaContactGroup</tt>
     */
    public Object getDescriptor()
    {
        return metaGroup;
    }

    /**
     * Returns the index of the underlying <tt>MetaContactGroup</tt> in its
     * <tt>MetaContactListService</tt> parent group.
     * @return the source index of the underlying <tt>MetaContactGroup</tt>
     */
    public int getSourceIndex()
    {
        return metaGroup.getParentMetaContactGroup().indexOf(metaGroup);
    }

    /**
     * Returns the parent <tt>UIGroup</tt>.
     * @return the parent <tt>UIGroup</tt>
     */
    public UIGroup getParentGroup()
    {
        MetaContactGroup parentGroup = metaGroup.getParentMetaContactGroup();

        if (parentGroup != null
            && !parentGroup.equals(
                GuiActivator.getContactListService().getRoot()))
            return new MetaUIGroup(parentGroup);

        return null;
    }

    /**
     * Indicates if this group was collapsed.
     * @return <tt>true</tt> to indicate that this group has been collapsed,
     * <tt>false</tt> - otherwise
     */
    public boolean isGroupCollapsed()
    {
        return ConfigurationManager
            .isContactListGroupCollapsed(metaGroup.getMetaUID());
    }

    /**
     * Returns the display name of the underlying <tt>MetaContactGroup</tt>.
     * @return the display name of the underlying <tt>MetaContactGroup</tt>
     */
    public String getDisplayName()
    {
        return metaGroup.getGroupName();
    }

    /**
     * Returns the count of child contacts of the underlying
     * <tt>MetaContactGroup</tt>.
     * @return the count of child contacts
     */
    public int countChildContacts()
    {
        return metaGroup.countChildContacts();
    }

    /**
     * Returns the count of online child contacts of the underlying
     * <tt>MetaContactGroup</tt>.
     * @return the count of online child contacts
     */
    public int countOnlineChildContacts()
    {
        return metaGroup.countOnlineChildContacts();
    }

    /**
     * Returns the identifier of the underlying <tt>MetaContactGroup</tt>.
     * @return the identifier of the underlying <tt>MetaContactGroup</tt>
     */
    public String getId()
    {
        return metaGroup.getMetaUID();
    }

    /**
     * Returns the corresponding <tt>GroupNode</tt>.
     * @return the corresponding <tt>GroupNode</tt>
     */
    public GroupNode getGroupNode()
    {
        return groupNode;
    }

    /**
     * Sets the corresponding <tt>GroupNode</tt>.
     * @param groupNode the corresponding <tt>GroupNoe</tt> in the contact list
     * component data model
     */
    public void setGroupNode(GroupNode groupNode)
    {
        this.groupNode = groupNode;
        if (groupNode == null)
            MetaContactListSource.removeUIGroup(metaGroup);
    }

    /**
     * Returns the <tt>JPopupMenu</tt> opened on a right button click over this
     * group in the contact list.
     * @return the <tt>JPopupMenu</tt> opened on a right button click over this
     * group in the contact list
     */
    public JPopupMenu getRightButtonMenu()
    {
        return new GroupRightButtonMenu(
            GuiActivator.getUIService().getMainFrame(), metaGroup);
    }
}
