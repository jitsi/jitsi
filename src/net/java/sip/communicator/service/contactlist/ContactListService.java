/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.contactlist;

import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.event.TreeModelListener;

/**
 * The <code>MetaContactListService</code> handles the global project contact
 * list including contacts from all implemented protocols.
 *
 * An implementation of
 * the <code>MetaContactListService</code> would take care of the
 * synchronization of server stored contact lists both contacts and
 * contact groups originating from server stored lists (as is the of ICQ) and
 * all protocol implementations currently
 * @author Emil Ivov
 */
public interface ContactListService
{
    /**
     * Returns the root of the tree.
     *
     * @return the root of the tree
     * @todo Implement this javax.swing.tree.TreeModel method
     */
    public Object getRoot();

    /**
     * Returns the child of <code>parent</code> at index <code>index</code>
     * in the parent's child array.
     *
     * @param parent a node in the tree, obtained from this data source
     * @param index int
     * @return the child of <code>parent</code> at index <code>index</code>
     * @todo Implement this javax.swing.tree.TreeModel method
     */
    public Object getChild(Object parent, int index);

    /**
     * Returns the number of children of <code>parent</code>.
     *
     * @param parent a node in the tree, obtained from this data source
     * @return the number of children of the node <code>parent</code>
     * @todo Implement this javax.swing.tree.TreeModel method
     */
    public int getChildCount(Object parent);

    /**
     * Returns <code>true</code> if <code>node</code> is a leaf.
     *
     * @param node a node in the tree, obtained from this data source
     * @return true if <code>node</code> is a leaf
     * @todo Implement this javax.swing.tree.TreeModel method
     */
    public boolean isLeaf(Object node);

    /**
     * Messaged when the user has altered the value for the item identified
     * by <code>path</code> to <code>newValue</code>.
     *
     * @param path path to the node that the user has altered
     * @param newValue the new value from the TreeCellEditor
     * @todo Implement this javax.swing.tree.TreeModel method
     */
    public void valueForPathChanged(TreePath path, Object newValue);

    /**
     * Returns the index of child in parent.
     *
     * @param parent a note in the tree, obtained from this data source
     * @param child the node we are interested in
     * @return the index of the child in the parent, or -1 if either
     *   <code>child</code> or <code>parent</code> are <code>null</code>
     * @todo Implement this javax.swing.tree.TreeModel method
     */
    public int getIndexOfChild(Object parent, Object child);

    /**
     * Adds a listener for the <code>TreeModelEvent</code> posted after the
     * tree changes.
     *
     * @param l the listener to add
     * @todo Implement this javax.swing.tree.TreeModel method
     */
    public void addTreeModelListener(TreeModelListener l);

    /**
     * Removes a listener previously added with
     * <code>addTreeModelListener</code>.
     *
     * @param l the listener to remove
     * @todo Implement this javax.swing.tree.TreeModel method
     */
    public void removeTreeModelListener(TreeModelListener l);

}
