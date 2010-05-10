/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist;

import javax.swing.*;
import javax.swing.tree.*;

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
     * Creates an instance of <tt>ContactListTreeModel</tt>.
     */
    public ContactListTreeModel()
    {
        super(null);

        RootUIGroup rootDescriptor = new RootUIGroup();
        rootGroupNode = new GroupNode(this, rootDescriptor);
        rootDescriptor.setGroupNode(rootGroupNode);

        this.setRoot(rootGroupNode);
    }

    /**
     * Returns the root group node.
     * @return the root group node
     */
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
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
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
        });
    }

    /**
     * Invoke this method after you've changed how node is to be
     * represented in the tree.
     * @param node the node that has changed
     */
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
    private class RootUIGroup
        implements UIGroup
    {
        /**
         * The corresponding group node.
         */
        private GroupNode groupNode;

        /**
         * Returns null to indicate that this group has no parent.
         * @return null
         */
        public UIGroup getParentGroup()
        {
            return null;
        }

        /**
         * This group is not attached to a contact source, so we return the
         * first index.
         * @return 0
         */
        public int getSourceIndex()
        {
            return 0;
        }

        /**
         * This group should never be collapsed.
         * @return false
         */
        public boolean isGroupCollapsed()
        {
            return false;
        }

        /**
         * Returns null to indicate that this group has no display name.
         * @return null
         */
        public String getDisplayName()
        {
            return null;
        }

        /**
         * As this group is not attached to a contact source it has no child
         * contacts.
         * @return 0
         */
        public int countChildContacts()
        {
            return 0;
        }

        /**
         * As this group is not attached to a contact source it has no child
         * contacts.
         * @return 0
         */
        public int countOnlineChildContacts()
        {
            return 0;
        }

        /**
         * Returns the descriptor of this group, just a string.
         * @return the descriptor of this group
         */
        public Object getDescriptor()
        {
            return "RootGroup";
        }

        /**
         * Returns null to indicate that this group has no identifier.
         * @return null
         */
        public String getId()
        {
            return null;
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
         * @param groupNode the <tt>GroupNode</tt> to set
         */
        public void setGroupNode(GroupNode groupNode)
        {
            this.groupNode = groupNode;
        }

        /**
         * This group is not visible to the user.
         * @return null
         */
        public JPopupMenu getRightButtonMenu()
        {
            return null;
        }
    }
}
