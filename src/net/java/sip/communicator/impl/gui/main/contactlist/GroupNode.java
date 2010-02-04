/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.util.*;

import javax.swing.tree.*;

import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;

/**
 * The <tt>GroupNode</tt> is a <tt>ContactListNode</tt> corresponding to a
 * given <tt>MetaContactGroup</tt>.
 *
 * @author Yana Stamcheva
 */
public class GroupNode
    extends DefaultMutableTreeNode
    implements  ContactListNode
{
    /**
     * The parent contact list model.
     */
    private final ContactListTreeModel treeModel;

    /**
     * The corresponding <tt>MetaContactGroup</tt>.
     */
    private final MetaContactGroup metaGroup;

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
     * and the corresponding <tt>metaGroup</tt> in the
     * <tt>MetaContactListService</tt>.
     *
     * @param treeModel the parent tree model containing this group
     * @param metaGroup the corresponding <tt>MetaContactGroup</tt>
     */
    public GroupNode(ContactListTreeModel treeModel, MetaContactGroup metaGroup)
    {
        super(metaGroup, true);

        this.treeModel = treeModel;
        this.metaGroup = metaGroup;

        isCollapsed = ConfigurationManager
            .isContactListGroupCollapsed(metaGroup.getMetaUID());
    }

    /**
     * Creates a <tt>ContactNode</tt> for the given <tt>metaContact</tt> and
     * adds it to this group.
     * @param metaContact the <tt>MetaContact</tt> to add
     * @return the created <tt>ContactNode</tt>
     */
    public ContactNode addMetaContact(MetaContact metaContact)
    {
        ContactNode contactNode = new ContactNode(metaContact);
        this.add(contactNode);
        return contactNode;
    }

    /**
     * Creates a <tt>ContactNode</tt> for the given <tt>metaContact</tt>,
     * adds it to this group and performs a sort at the end.
     * @param metaContact the <tt>MetaContact</tt> to add
     * @return the created <tt>ContactNode</tt>
     */
    @SuppressWarnings("unchecked")
    public ContactNode sortedAddMetaContact(MetaContact metaContact)
    {
        ContactNode contactNode = new ContactNode(metaContact);

        this.add(contactNode);

        // TODO: Optimize!
        Collections.sort(children, nodeComparator);

        this.fireNodeInserted(getIndex(contactNode));

        return contactNode;
    }

    /**
     * Removes the node corresponding to the given <tt>MetaContact</tt> from
     * this group.
     * @param metaContact the <tt>MetaContact</tt> to remove
     */
    public void removeMetaContact(MetaContact metaContact)
    {
        ContactNode contactNode = findContactNode(metaContact);

        if (contactNode != null)
        {
            int index = getIndex(contactNode);
            // We remove the node directly from the list, thus skipping all the
            // checks verifying if the node belongs to this parent.
            children.removeElementAt(index);
            contactNode.setParent(null);

            fireNodeRemoved(contactNode, index);
        }
    }

    /**
     * Creates a <tt>GroupNode</tt> for the given <tt>metaGroup</tt> and adds it
     * to this group.
     * @param metaGroup the <tt>MetaContactGroup</tt> to add
     * @return the created <tt>GroupNode</tt>
     */
    public GroupNode addMetaContactGroup(MetaContactGroup metaGroup)
    {
        GroupNode groupNode = new GroupNode(treeModel, metaGroup);
        this.add(groupNode);
        return groupNode;
    }

    /**
     * Removes the node corresponding to the given <tt>metaGroup</tt> from this
     * group node.
     * @param metaGroup the <tt>MetaContactGroup</tt> to remove
     */
    public void removeMetaContactGroup(MetaContactGroup metaGroup)
    {
        GroupNode groupNode = findGroupNode(metaGroup);

        if (groupNode != null)
        {
            int index = getIndex(groupNode);
            // We remove the node directly from the list, thus skipping all the
            // checks verifying if the node belongs to this parent.
            children.removeElementAt(index);
            groupNode.setParent(null);

            fireNodeRemoved(groupNode, index);
        }
    }

    /**
     * Creates a <tt>GroupNode</tt> for the given <tt>metaGroup</tt>, adds it
     * to this group node and performs a sort at the end.
     * @param metaGroup the <tt>MetaContactGroup</tt> to add
     * @return the created <tt>GroupNode</tt>
     */
    @SuppressWarnings("unchecked")
    public GroupNode sortedAddMetaContactGroup(MetaContactGroup metaGroup)
    {
        GroupNode groupNode = new GroupNode(treeModel, metaGroup);

        this.add(groupNode);

        // TODO: Optimize!
        Collections.sort(children, nodeComparator);

        this.fireNodeInserted(getIndex(groupNode));

        return groupNode;
    }

    /**
     * Returns the <tt>MetaContactGroup</tt> corresponding to this
     * <tt>GroupNode</tt>.
     * @return the <tt>MetaContactGroup</tt> corresponding to this
     * <tt>GroupNode</tt>
     */
    public MetaContactGroup getMetaContactGroup()
    {
        return (MetaContactGroup) getUserObject();
    }

    /**
     * Finds the <tt>GroupNode</tt> corresponding to the given
     * <tt>metaGroup</tt> in the children of this node.
     * @param metaGroup the <tt>MetaContactGroup</tt>, which node we're looking
     * for
     * @return the corresponding <tt>GroupNode</tt> or null if no group node
     * was found
     */
    @SuppressWarnings("unchecked")
    public GroupNode findGroupNode(MetaContactGroup metaGroup)
    {
        Enumeration<TreeNode> children = children();
        while(children.hasMoreElements())
        {
            TreeNode treeNode = children.nextElement();
            if (treeNode instanceof GroupNode
                && ((GroupNode)treeNode).getMetaContactGroup().equals(metaGroup))
                return (GroupNode) treeNode;
        }
        return null;
    }

    /**
     * Finds the <tt>ContactNode</tt> corresponding to the given
     * <tt>metaContact</tt> in the children of this node.
     * @param metaContact the <tt>MetaContact</tt>, which node we're looking for
     * @return the corresponding <tt>ContactNode</tt> or null if no contact node
     * was found
     */
    @SuppressWarnings("unchecked")
    public ContactNode findContactNode(MetaContact metaContact)
    {
        Enumeration<TreeNode> children = children();
        while(children.hasMoreElements())
        {
            TreeNode treeNode = children.nextElement();
            if (treeNode instanceof ContactNode
                && ((ContactNode)treeNode).getMetaContact().equals(metaContact))
                return (ContactNode) treeNode;
        }
        return null;
    }

    /**
     * Returns the index of this node in its parent group in the
     * <tt>MetaContactListService</tt>.
     * @return the index of this node in its parent group in the
     * <tt>MetaContactListService</tt>
     */
    public int getMetaContactListIndex()
    {
        MetaContactGroup parentGroup = metaGroup.getParentMetaContactGroup();
        if (parentGroup != null)
            return parentGroup.indexOf(metaGroup);
        else return 0; //this is the root group
    }

    /**
     * Sorts the children of this node.
     */
    @SuppressWarnings("unchecked")
    public void sort()
    {
        if (children != null)
        {
            Collections.sort(children, nodeComparator);

            fireNodesChanged();
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
     * 
     * Note: this comparator imposes orderings that are inconsistent with
     * equals.
     */
    private class NodeComparator implements Comparator<ContactListNode>
    {
        public int compare(ContactListNode node1, ContactListNode node2)
        {
            int index1 = node1.getMetaContactListIndex();
            int index2 = node2.getMetaContactListIndex();

            // Child groups are shown after child contacts.
            if (node1 instanceof GroupNode && node2 instanceof ContactNode) 
                return 1;

            if (node1 instanceof ContactNode && node2 instanceof GroupNode)
                return -1;

            if (index1 > index2) return 1;
            else if (index1 < index2) return -1;
            else return 0;
        }
    }
}