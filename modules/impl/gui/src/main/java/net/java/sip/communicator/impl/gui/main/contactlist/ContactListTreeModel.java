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

import java.lang.reflect.*;

import javax.swing.*;
import javax.swing.tree.*;

import net.java.sip.communicator.service.gui.*;

/**
 * The data model of the contact list.
 *
 * @author Yana Stamcheva
 */
public class ContactListTreeModel
    extends DefaultTreeModel
{
    /**
     * The root node.
     */
    private final GroupNode rootGroupNode;

    /**
     * The parent tree.
     */
    private final JTree parentTree;

    /**
     * Creates an instance of <tt>ContactListTreeModel</tt>.
     *
     * @param tree the parent tree
     */
    public ContactListTreeModel(JTree tree)
    {
        super(null);

        this.parentTree = tree;

        UIGroupImpl rootDescriptor = new RootUIGroup();
        rootGroupNode = new GroupNode(this, rootDescriptor);
        rootDescriptor.setGroupNode(rootGroupNode);

        this.setRoot(rootGroupNode);
    }

    /**
     * Returns the root group node.
     * @return the root group node
     */
    @Override
    public GroupNode getRoot()
    {
        return rootGroupNode;
    }

    /**
     * Returns the first found child <tt>ContactNode</tt>.
     * @return the first found child <tt>ContactNode</tt> or <tt>null</tt>
     * if there is no ContactNode.
     */
    public ContactNode findFirstContactNode()
    {
        return findFirstContactNode(rootGroupNode);
    }

    /**
     * Removes all nodes except the root node and clears all dependencies.
     */
    public void clear()
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            try
            {
                SwingUtilities.invokeAndWait(new Runnable()
                {
                    public void run()
                    {
                        clear();
                    }
                });
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
            catch (InvocationTargetException e)
            {
                e.printStackTrace();
            }
            return;
        }

        // The following code is always invoked in the swing thread.
        int childCount = rootGroupNode.getChildCount();
        int[] removedIndexs = new int[childCount];
        Object[] removedNodes = new Object[childCount];
        for (int i = 0; i < childCount; i ++)
        {
            removedIndexs[i] = i;
            removedNodes[i] = rootGroupNode.getChildAt(i);
        }

        rootGroupNode.clear();
        nodesWereRemoved(rootGroupNode, removedIndexs, removedNodes);
    }

    /**
     * Returns the parent tree.
     *
     * @return the parent tree
     */
    public JTree getParentTree()
    {
        return parentTree;
    }

    /**
     * Invoke this method after you've changed how node is to be
     * represented in the tree.
     * @param node the node that has changed
     */
    @Override
    public void nodeChanged(final TreeNode node)
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                ContactListTreeModel.super.nodeChanged(node);
            }
        });
    }

    /**
     * Returns the first found child <tt>ContactNode</tt>.
     * @param parentNode the parent <tt>GroupNode</tt> to search in
     * @return the first found child <tt>ContactNode</tt>.
     */
    private ContactNode findFirstContactNode(GroupNode parentNode)
    {
        // If the parent node has no children we have nothing to do here.
        if (parentNode.getChildCount() == 0)
            return null;

        TreeNode treeNode = parentNode.getFirstChild();

        if (treeNode instanceof GroupNode)
            return findFirstContactNode((GroupNode) treeNode);
        else
            return (ContactNode)treeNode;
    }

    /**
     * The <tt>RootUIGroup</tt> is the root group in this contact list model.
     */
    private static class RootUIGroup
        extends UIGroupImpl
    {
        /**
         * The corresponding group node.
         */
        private GroupNode groupNode;

        /**
         * Returns null to indicate that this group has no parent.
         * @return null
         */
        @Override
        public UIGroup getParentGroup()
        {
            return null;
        }

        /**
         * This group is not attached to a contact source, so we return the
         * first index.
         * @return 0
         */
        @Override
        public int getSourceIndex()
        {
            return 0;
        }

        /**
         * This group should never be collapsed.
         * @return false
         */
        @Override
        public boolean isGroupCollapsed()
        {
            return false;
        }

        /**
         * Returns null to indicate that this group has no display name.
         * @return null
         */
        @Override
        public String getDisplayName()
        {
            return null;
        }

        /**
         * As this group is not attached to a contact source it has no child
         * contacts.
         * @return 0
         */
        @Override
        public int countChildContacts()
        {
            return 0;
        }

        /**
         * As this group is not attached to a contact source it has no child
         * contacts.
         * @return 0
         */
        @Override
        public int countOnlineChildContacts()
        {
            return 0;
        }

        /**
         * Returns the descriptor of this group, just a string.
         * @return the descriptor of this group
         */
        @Override
        public Object getDescriptor()
        {
            return "RootGroup";
        }

        /**
         * Returns null to indicate that this group has no identifier.
         * @return null
         */
        @Override
        public String getId()
        {
            return null;
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
         * @param groupNode the <tt>GroupNode</tt> to set
         */
        @Override
        public void setGroupNode(GroupNode groupNode)
        {
            this.groupNode = groupNode;
        }

        /**
         * This group is not visible to the user.
         * @return null
         */
        @Override
        public JPopupMenu getRightButtonMenu()
        {
            return null;
        }
    }
}
