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

import java.util.*;

import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.tree.*;

import net.java.sip.communicator.plugin.desktoputil.plaf.*;
import net.java.sip.communicator.service.gui.*;
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
     * The <tt>Logger</tt> used by the <tt>GroupNode</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(GroupNode.class);

    /**
     * The parent contact list model.
     */
    private final ContactListTreeModel treeModel;

    /**
     * The corresponding <tt>UIGroup</tt>.
     */
    private final UIGroup group;

    /**
     * The <tt>ContactListNode</tt> <tt>Comparator</tt> used to sort the list of
     * children.
     * <p>
     * Since the <tt>NodeComparator</tt> class is static, it makes sense to not
     * have it instantiated per <tt>GroupNode</tt> instance but rather share one
     * and the same between all of them.
     * </p>
     */
    private static final NodeComparator nodeComparator = new NodeComparator();

    /**
     * Indicates if this group node is collapsed or expanded.
     */
    private boolean isCollapsed = false;

    /**
     * Creates a <tt>GroupNode</tt> by specifying the parent <tt>treeModel</tt>
     * and the corresponding <tt>uiGroup</tt>.
     *
     * @param treeModel the parent tree model containing this group
     * @param uiGroup the corresponding <tt>UIGroupImpl</tt>
     */
    public GroupNode(   ContactListTreeModel treeModel,
                        UIGroupImpl uiGroup)
    {
        super(uiGroup, true);

        this.treeModel = treeModel;
        this.group = uiGroup;

        isCollapsed = group.isGroupCollapsed();
    }

    /**
     * Creates a <tt>ContactNode</tt> for the given <tt>uiContact</tt>
     * and adds it to this group.
     * @param uiContact the <tt>UIContactImpl</tt> to add
     * @return the created <tt>ContactNode</tt>
     */
    public ContactNode addContact(UIContactImpl uiContact)
    {
        if (logger.isDebugEnabled())
            logger.debug("Group node add contact: "
                    + uiContact.getDisplayName());

        int selectedIndex = getLeadSelectionRow();

        ContactNode contactNode = new ContactNode(uiContact);
        uiContact.setContactNode(contactNode);

        add(contactNode);

        // Since contactNode is added to the back of the list, don't go looking
        // for it, just calculate which index is for the last node in the list.
        fireNodeInserted(children.size() - 1);

        refreshSelection(selectedIndex, getLeadSelectionRow());

        return contactNode;
    }

    /**
     * Creates a <tt>ContactNode</tt> for the given <tt>uiContact</tt>,
     * adds it to this group and performs a sort at the end.
     * @param uiContact the <tt>UIContactImpl</tt> to add
     * @return the created <tt>ContactNode</tt>
     */
    @SuppressWarnings("unchecked")
    public ContactNode sortedAddContact(UIContactImpl uiContact)
    {
        if (logger.isDebugEnabled())
            logger.debug("Group node sorted add contact: "
                    + uiContact.getDisplayName());

        ContactNode contactNode = new ContactNode(uiContact);
        uiContact.setContactNode(contactNode);

        if (children == null)
        {
            // Initially, children will be null.
            add(contactNode);
            fireNodeInserted(0);
        }
        else
        {
            // Instead of sorting after every addition, find the spot where we
            // should insert the node such that it is inserted in order.
            final int insertionPoint = Collections.binarySearch(children,
                    contactNode, nodeComparator);
            if (insertionPoint < 0)
            {
                // index < 0 indicates that the node is not currently in the
                // list and suggests an insertion point.
                final int index = (insertionPoint + 1) * -1;
                insert(contactNode, index);
                fireNodeInserted(index);
            }
        }

        return contactNode;
    }

    /**
     * Removes the node corresponding to the given <tt>uiContact</tt> from this
     * group.
     * @param uiContact the <tt>UIContactImpl</tt> to remove
     */
    public void removeContact(UIContactImpl uiContact)
    {
        if (logger.isDebugEnabled())
            logger.debug("Group node remove contact: "
                + uiContact.getDisplayName());

        final ContactNode contactNode;
        int index;
        synchronized (uiContact)
        {
            contactNode = uiContact.getContactNode();

            if (contactNode == null)
                return;
        
            index = getIndex(contactNode);
        }

        // not found
        if(index == -1)
            return;
        
        int selectedIndex = getLeadSelectionRow();

        // We remove the node directly from the list, thus skipping all
        // the checks verifying if the node belongs to this parent.
        children.removeElementAt(index);

        contactNode.setParent(null);
        synchronized (uiContact)
        {
            uiContact.setContactNode(null);
            uiContact = null;
        }

        fireNodeRemoved(contactNode, index);

        refreshSelection(selectedIndex, getLeadSelectionRow());
    }

    /**
     * Creates a <tt>GroupNode</tt> for the given <tt>uiGroup</tt> and
     * adds it to this group.
     * @param uiGroup the <tt>UIGroupImpl</tt> to add
     * @return the created <tt>GroupNode</tt>
     */
    public GroupNode addContactGroup(UIGroupImpl uiGroup)
    {
        int selectedIndex = getLeadSelectionRow();

        GroupNode groupNode;
        synchronized (uiGroup)
        {
            groupNode = new GroupNode(treeModel, uiGroup);
            uiGroup.setGroupNode(groupNode);
        }

        add(groupNode);

        // Since contactNode is added to the back of the list, don't go looking
        // for it, just calculate which index is for the last node in the list.
        fireNodeInserted(children.size() - 1);

        refreshSelection(selectedIndex, getLeadSelectionRow());

        return groupNode;
    }

    /**
     * Removes the node corresponding to the given <tt>uiGroup</tt> from this
     * group node.
     * @param uiGroup the <tt>UIGroupImpl</tt> to remove
     */
    public void removeContactGroup(UIGroupImpl uiGroup)
    {
        GroupNode groupNode;
        
        synchronized (uiGroup)
        {
            groupNode = uiGroup.getGroupNode();

            if (groupNode == null)
                return;
        }

        int index = getIndex(groupNode);

        // not found
        if(index == -1)
            return;

        int selectedIndex = getLeadSelectionRow();

        // We remove the node directly from the list, thus skipping all the
        // checks verifying if the node belongs to this parent.
        children.removeElementAt(index);

        groupNode.setParent(null);
        synchronized (uiGroup)
        {
            uiGroup.setGroupNode(null);
        }
        
        fireNodeRemoved(groupNode, index);

        refreshSelection(selectedIndex, getLeadSelectionRow());
    }

    /**
     * Creates a <tt>GroupNode</tt> for the given <tt>uiGroup</tt>,
     * adds it to this group node and performs a sort at the end.
     * @param uiGroup the <tt>UIGroupImpl</tt> to add
     * @return the created <tt>GroupNode</tt>
     */
    @SuppressWarnings("unchecked")
    public GroupNode sortedAddContactGroup(UIGroupImpl uiGroup)
    {
        GroupNode groupNode;
        synchronized (uiGroup)
        {
            groupNode = new GroupNode(treeModel, uiGroup);

            uiGroup.setGroupNode(groupNode);
        }

        if (children == null)
        {
            // Initially, children will be null.
            add(groupNode);
            fireNodeInserted(0);
        }
        else
        {
            // Instead of sorting after every addition, find the spot where we
            // should insert the node such that it is inserted in order.
            int insertionPoint = Collections.binarySearch(children,
                    groupNode, nodeComparator);
            if (insertionPoint < 0)
            {
                // index < 0 indicates that the node is not currently in the
                // list and suggests an insertion point.
                insertionPoint = (insertionPoint + 1) * -1;
            }
            else
            {
                // A node with this index was already found. As the index
                // is not guaranteed to be unique, add this group after the
                // one just found.
                ++insertionPoint;
            }

            insert(groupNode, insertionPoint);
            fireNodeInserted(insertionPoint);
        }

        return groupNode;
    }

    /**
     * Returns a collection of all direct children of this <tt>GroupNode</tt>.
     *
     * @return a collection of all direct children of this <tt>GroupNode</tt>
     */
    @SuppressWarnings("unchecked")
    public Collection<ContactNode> getContacts()
    {
        if (children != null)
            return Collections.unmodifiableCollection(children);

        return null;
    }

    /**
     * Returns the <tt>UIGroup</tt> corresponding to this <tt>GroupNode</tt>.
     * @return the <tt>UIGroup</tt> corresponding to this <tt>GroupNode</tt>
     */
    public UIGroupImpl getGroupDescriptor()
    {
        return (UIGroupImpl) getUserObject();
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
    public void sort(final ContactListTreeModel treeModel)
    {
        if (children != null)
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    TreePath selectionPath = getLeadSelectionPath();
                    int oldSelectionIndex = getLeadSelectionRow();

                    Collections.sort(children, nodeComparator);

                    fireNodesChanged();

                    treeModel.getParentTree().setSelectionPath(selectionPath);

                    refreshSelection(oldSelectionIndex, getLeadSelectionRow());
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
        int childCount = getChildCount();

        for (int i = 0; i < childCount; i ++)
        {
            TreeNode treeNode = getChildAt(i);

            if (treeNode instanceof ContactNode)
            {
                UIContactImpl  contact 
                    = ((ContactNode) treeNode).getContactDescriptor();
                synchronized (contact)
                {
                    contact.setContactNode(null);
                }
                    
            }
            else if (treeNode instanceof GroupNode)
            {
                UIGroupImpl group
                    = ((GroupNode) treeNode).getGroupDescriptor();
                synchronized (group)
                {
                    group.setGroupNode(null);
                }
                    
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
        treeModel.nodesWereInserted(this, new int[]{index});
    }

    /**
     * Notifies all interested listeners that <tt>node</tt> has been removed
     * from the given <tt>index</tt>.
     * @param node the node that has been removed
     * @param index the index of the removed node
     */
    private void fireNodeRemoved(ContactListNode node, int index)
    {
        treeModel.nodesWereRemoved(this, new int[]{index}, new Object[]{node});
    }

    /**
     * Notifies all interested listeners that all nodes have changed.
     */
    private void fireNodesChanged()
    {
        int childCount = getChildCount();
        int[] changedIndexes = new int[childCount];

        for (int i = 0; i < childCount; i++)
            changedIndexes[i] = i;

        treeModel.nodesChanged(this, changedIndexes);
    }

    /**
     * Note: this comparator imposes orderings that are inconsistent with
     * equals.
     */
    static class NodeComparator
        implements Comparator<ContactListNode>
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

            // If both indexes are unknown, consider them equal. We need this
            // case to ensure the property of symmetry in the node comparator.
            if (index1 < 0 && index2 < 0)
                return 0;
            // If the first index is unknown then we position it at the end.
            if (index1 < 0)
                return 1;
            // If the second index is unknown then we position it at the end.
            if (index2 < 0)
                return -1;

            if (index1 > index2) return 1;
            else if (index1 < index2) return -1;
            else return 0;
        }
    }

    /**
     * Returns the current lead selection row.
     *
     * @return the current lead selection row
     */
    private int getLeadSelectionRow()
    {
        JTree tree = treeModel.getParentTree();
        int[] rows = tree.getSelectionRows();
        int selectedRow = -1;

        if ((rows != null) && (rows.length != 0))
            selectedRow = rows[0];

        return selectedRow;
    }

    /**
     * Returns the current lead selection path.
     *
     * @return the current lead selection path
     */
    private TreePath getLeadSelectionPath()
    {
        return treeModel.getParentTree().getSelectionPath();
    }

    /**
     * Refreshes the selection paths.
     *
     * @param lastSelectedIndex the last selected index
     * @param newSelectedIndex the newly selected index
     */
    private void refreshSelection(int lastSelectedIndex, int newSelectedIndex)
    {
        JTree tree = treeModel.getParentTree();
        TreeUI treeUI = tree.getUI();

        if (treeUI instanceof SIPCommTreeUI)
        {
            SIPCommTreeUI sipCommTreeUI = (SIPCommTreeUI) treeUI;
            TreePath oldSelectionPath = tree.getPathForRow(lastSelectedIndex);
            TreePath newSelectionPath = tree.getPathForRow(newSelectedIndex);

            sipCommTreeUI.selectionChanged(oldSelectionPath, newSelectionPath);
        }
    }
}
