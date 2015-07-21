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
package net.java.sip.communicator.impl.gui.main.contactlist.contactsource;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import java.util.*;

/**
 * The <tt>MetaUIGroup</tt> is the implementation of the UIGroup for the
 * <tt>MetaContactListService</tt>. This implementation is based on the
 * <tt>MetaContactGroup</tt>.
 *
 * @author Yana Stamcheva
 */
public class MetaUIGroup
    extends UIGroupImpl
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
    @Override
    public Object getDescriptor()
    {
        return metaGroup;
    }

    /**
     * Returns the index of the underlying <tt>MetaContactGroup</tt> in its
     * <tt>MetaContactListService</tt> parent group.
     * @return the source index of the underlying <tt>MetaContactGroup</tt>
     */
    @Override
    public int getSourceIndex()
    {
        MetaContactGroup parentGroup = metaGroup.getParentMetaContactGroup();
        return GuiActivator.getContactList()
                .getMetaContactListSource().getIndex()
            * MAX_GROUPS + 
            ((parentGroup == null)? 0 : (parentGroup.indexOf(metaGroup) + 1)) 
            * MAX_CONTACTS;
    }

    /**
     * Returns the parent <tt>UIGroup</tt>.
     * @return the parent <tt>UIGroup</tt>
     */
    @Override
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
    @Override
    public boolean isGroupCollapsed()
    {
        return ConfigurationUtils
            .isContactListGroupCollapsed(metaGroup.getMetaUID());
    }

    /**
     * Returns the display name of the underlying <tt>MetaContactGroup</tt>.
     * @return the display name of the underlying <tt>MetaContactGroup</tt>
     */
    @Override
    public String getDisplayName()
    {
        return metaGroup.getGroupName();
    }

    /**
     * Returns the count of child contacts of the underlying
     * <tt>MetaContactGroup</tt>.
     * @return the count of child contacts
     */
    @Override
    public int countChildContacts()
    {
        return metaGroup.countChildContacts();
    }

    /**
     * Returns the count of online child contacts of the underlying
     * <tt>MetaContactGroup</tt>.
     * @return the count of online child contacts
     */
    @Override
    public int countOnlineChildContacts()
    {
        return metaGroup.countOnlineChildContacts();
    }

    /**
     * Returns the identifier of the underlying <tt>MetaContactGroup</tt>.
     * @return the identifier of the underlying <tt>MetaContactGroup</tt>
     */
    @Override
    public String getId()
    {
        return metaGroup.getMetaUID();
    }

    /**
     * Returns the corresponding <tt>GroupNode</tt>.
     * @return the corresponding <tt>GroupNode</tt>
     */
    @Override
    public GroupNode getGroupNode()
    {
        return groupNode;
    }

    /**
     * Sets the corresponding <tt>GroupNode</tt>.
     * @param groupNode the corresponding <tt>GroupNoe</tt> in the contact list
     * component data model
     */
    @Override
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
    @Override
    public JPopupMenu getRightButtonMenu()
    {
        // check if group has readonly proto group then skip menu
        boolean hasReadonlyGroup = false;
        Iterator<ContactGroup> groupsIterator =
            metaGroup.getContactGroups();
        while(groupsIterator.hasNext())
        {
            ContactGroup group = groupsIterator.next();
            OperationSetPersistentPresencePermissions opsetPermissions =
                group.getProtocolProvider().getOperationSet(
                    OperationSetPersistentPresencePermissions.class);

            if(opsetPermissions != null
                && opsetPermissions.isReadOnly(group))
            {
                hasReadonlyGroup = true;
                break;
            }
        }

        if(hasReadonlyGroup)
            return null;

        return new GroupRightButtonMenu(
            GuiActivator.getUIService().getMainFrame(), metaGroup);
    }
}
