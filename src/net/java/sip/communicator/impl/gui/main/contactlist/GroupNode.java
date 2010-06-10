/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.util.*;

import javax.swing.*;
import javax.swing.tree.*;

import net.java.sip.communicator.util.*;

/**
 * The <tt>GroupNode</tt> is a <tt>ContactListNode</tt> corresponding to a
 * given <tt>UIGroup</tt>.
 *
 * @author Yana Stamcheva
 */
public class GroupNode
    extends DefaultMutableTreeNode
    implements  ContactListNode
{
    /**
     * The Group node logger.
     */
    private final Logger logger = Logger.getLogger(GroupNode.class);

    /**
     * The parent contact list model.
     */
    private final ContactListTreeModel treeModel;

    /**
     * The corresponding <tt>UIGroup</tt>.
     */
    private final UIGroup group;

    /**
     * A node comparator used to sort the list of children.
     */
    private final NodeComparator nodeComparator = new NodeComparator();

    /**
     * Indicates if this group node is collapsed or expanded.
     */
    private boolean isCollapsed = false;

    /**
     * Creates a <tt>GroupNode</tt> by specifying the parent <tt>treeModel</tt>
     * and the corresponding <tt>uiGroup</tt>.
     *
     * @param treeModel the parent tree model containing this group
     * @param uiGroup the corresponding <tt>UIGroup</tt>
     */
    public GroupNode(   ContactListTreeModel treeModel,
                        UIGroup uiGroup)
    {
        super(uiGroup, true);

        this.treeModel = treeModel;
        this.group = uiGroup;

        isCollapsed = group.isGroupCollapsed();
    }

    /**
     * Creates a <tt>ContactNode</tt> for the given <tt>uiContact</tt>
     * and adds it to this group.
     * @param uiContact the <tt>UIContact</tt> to add
     * @return the created <tt>ContactNode</tt>
     */
    public ContactNode addContact(UIContact uiContact)
    {
        if (logger.isDebugEnabled())
            logger.debug("Group node add contact: "
                    + uiContact.getDisplayName());

        ContactNode contactNode = new ContactNode(uiContact);
        uiContact.setContactNode(contactNode);

        add(contactNode);

        int contactIndex = getIndex(contactNode);

        if (contactIndex > -1)
            fireNodeInserted(contactIndex);

        return contactNode;
    }

    /**
     * Creates a <tt>ContactNode</tt> for the given <tt>uiContact</tt>,
     * adds it to this group and performs a sort at the end.
     * @param uiContact the <tt>UIContact</tt> to add
     * @return the created <tt>ContactNode</tt>
     */
    @SuppressWarnings("unchecked")
    public ContactNode sortedAddContact(UIContact uiContact)
    {
        if (logger.isDebugEnabled())
            logger.debug("Group node sorted add contact: "
                    + uiContact.getDisplayName());

        ContactNode contactNode = new ContactNode(uiContact);
        uiContact.setContactNode(contactNode);

        add(contactNode);

        // TODO: Optimize!
        Collections.sort(children, nodeComparator);

        int contactIndex = getIndex(contactNode);

        if (contactIndex > -1)
            fireNodeInserted(contactIndex);

        return contactNode;
    }

    /**
     * Removes the node corresponding to the given <tt>uiContact</tt> from this
     * group.
     * @param uiContact the <tt>UIContact</tt> to remove
     */
    public void removeContact(UIContact uiContact)
    {
        final ContactNode contactNode = uiContact.getContactNode();

        if (contactNode != null)
        {
            int index = getIndex(contactNode);
            // We remove the node directly from the list, thus skipping all
            // the checks verifying if the node belongs to this parent.
            children.removeElementAt(index);

            contactNode.setParent(null);
            uiContact.setContactNode(null);
            uiContact = null;

            fireNodeRemoved(contactNode, index);
        }
    }

    /**
     * Creates a <tt>GroupNode</tt> for the given <tt>uiGroup</tt> and
     * adds it to this group.
     * @param uiGroup the <tt>UIGroup</tt> to add
     * @return the created <tt>GroupNode</tt>
     */
    public GroupNode addContactGroup(UIGroup uiGroup)
    {
        GroupNode groupNode = new GroupNode(treeModel, uiGroup);
        uiGroup.setGroupNode(groupNode);

        add(groupNode);

        int groupIndex = getIndex(groupNode);

        if (groupIndex > -1)
            fireNodeInserted(groupIndex);

        return groupNode;
    }

    /**
     * Removes the node corresponding to the given <tt>uiGroup</tt> from this
     * group node.
     * @param uiGroup the <tt>UIGroup</tt> to remove
     */
    public void removeContactGroup(UIGroup uiGroup)
    {
        GroupNode groupNode = uiGroup.getGroupNode();

        if (groupNode != null)
        {
            int index = getIndex(groupNode);
            // We remove the node directly from the list, thus skipping all the
            // checks verifying if the node belongs to this parent.
            children.removeElementAt(index);

            groupNode.setParent(null);
            uiGroup.setGroupNode(null);

            fireNodeRemoved(groupNode, index);
        }
    }

    /**
     * Creates a <tt>GroupNode</tt> for the given <tt>uiGroup</tt>,
     * adds it to this group node and performs a sort at the end.
     * @param uiGroup the <tt>UIGroup</tt> to add
     * @return the created <tt>GroupNode</tt>
     */
    @SuppressWarnings("unchecked")
    public GroupNode sortedAddContactGroup(UIGroup uiGroup)
    {
        GroupNode groupNode = new GroupNode(treeModel, uiGroup);

        uiGroup.setGroupNode(groupNode);

        add(groupNode);

        // TODO: Optimize!
        Collections.sort(children, nodeComparator);

        int contactIndex = getIndex(groupNode);

        if (contactIndex > -1)
            fireNodeInserted(contactIndex);

        return groupNode;
    }

    /**
     * Returns the <tt>UIGroup</tt> corresponding to this <tt>GroupNode</tt>.
     * @return the <tt>UIGroup</tt> corresponding to this <tt>GroupNode</tt>
     */
    public UIGroup getGroupDescriptor()
    {
        return (UIGroup) getUserObject();
    }

    /**
     * Returns the index of this node in its parent group.
     * @return the index of this node in its parent group
     */
    public int getSourceIndex()
    {
        return group.getSourceIndex();
    }

    /**
     * Sorts the children of this node.
     * @param treeModel the <tt>ContactListTreeModel</tt>, which should be
     * refreshed
     */
    @SuppressWarnings("unchecked")
    public void sort(ContactListTreeModel treeModel)
    {
        if (children != null)
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    Collections.sort(children, nodeComparator);

                    fireNodesChanged();
                }
            });
        }
    }

    /**
     * Returns <tt>true</tt> if the group is collapsed or <tt>false</tt>
     * otherwise.
     * @return <tt>true</tt> if the group is collapsed or <tt>false</tt>
     * otherwise.
     */
    public boolean isCollapsed()
    {
        return isCollapsed;
    }

    /**
     * Clears all dependencies for all children in the given <tt>groupNode</tt>
     * (i.e. GroupNode - UIGroup - MetaContactGroup or ContactNode - UIContact
     * - SourceContact).
     */
    public void clear()
    {
        for (int i = 0; i < getChildCount(); i ++)
        {
            TreeNode treeNode = getChildAt(i);

            if (treeNode instanceof ContactNode)
            {
                ((ContactNode) treeNode).getContactDescriptor()
                    .setContactNode(null);
            }
            else if (treeNode instanceof GroupNode)
            {
                ((GroupNode) treeNode).getGroupDescriptor()
                    .setGroupNode(null);

                ((GroupNode) treeNode).clear();
            }
        }
        if (children != null)
            children.removeAllElements();
    }

    /**
     * Notifies all interested listeners that a node has been inserted at the
     * given <tt>index</tt>.
     * @param index the index of the newly inserted node
     */
    private void fireNodeInserted(int index)
    {
        int[] newIndexs = new int[1];
        newIndexs[0] = index;
        treeModel.nodesWereInserted(this, newIndexs);
    }

    /**
     * Notifies all interested listeners that <tt>node</tt> has been removed
     * from the given <tt>index</tt>.
     * @param node the node that has been removed
     * @param index the index of the removed node
     */
    private void fireNodeRemoved(ContactListNode node, int index)
    {
        int[] removedIndexs = new int[1];
        removedIndexs[0] = index;
        treeModel.nodesWereRemoved(this, removedIndexs, new Object[]{node});
    }

    /**
     * Notifies all interested listeners that all nodes has changed.
     */
    private void fireNodesChanged()
    {
        int childCount = getChildCount();
        int[] changedIndexs = new int[childCount];
        for (int i = 0; i < childCount; i++)
            changedIndexs[i] = i;

        treeModel.nodesChanged(this, changedIndexs);
    }

    /**
     * Note: this comparator imposes orderings that are inconsistent with
     * equals.
     */
    private class NodeComparator implements Comparator<ContactListNode>
    {
        /**
         * Compares its two arguments for order.  Returns a negative integer,
         * zero, or a positive integer as the first argument is less than, equal
         * to, or greater than the second.
         * @param node1 the first <tt>ContactListNode</tt> to compare
         * @param node2 the second <tt>ContactListNode</tt> to compare
         * @return -1 if the first node should be positioned before the second
         * one, 1 if the first argument should be positioned after the second
         * one, 0 if there's no matter
         */
        public int compare(ContactListNode node1, ContactListNode node2)
        {
            int index1 = node1.getSourceIndex();
            int index2 = node2.getSourceIndex();

            // Child groups are shown after child contacts.
            if (node1 instanceof GroupNode && node2 instanceof ContactNode) 
                return 1;

            if (node1 instanceof ContactNode && node2 instanceof GroupNode)
                return -1;

            // If the first index is unknown then we position it to the end.
            if (index1 < 0)
                return 1;

            // If the second index is unknown then we position it to the end.
            if (index2 < 0)
                return -1;

            if (index1 > index2) return 1;
            else if (index1 < index2) return -1;
            else return 0;
        }
    }
}