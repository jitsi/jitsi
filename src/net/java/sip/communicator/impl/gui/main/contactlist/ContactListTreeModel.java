/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist;

import javax.swing.tree.*;

import net.java.sip.communicator.service.contactlist.*;

/**
 * The data model of the contact list.
 *
 * @author Yana Stamcheva
 */
public class ContactListTreeModel
    extends DefaultTreeModel
{
    private GroupNode rootGroupNode;

    /**
     * Creates the <tt>ContactListTreeModel</tt> by specifying the
     * <tt>rootMetaGroup</tt>, which would correspond to the root of our data
     * model.
     * @param rootMetaGroup the root <tt>MetaContactGroup</tt>, which would
     * correspond to our root node
     */
    public ContactListTreeModel(MetaContactGroup rootMetaGroup)
    {
        super(null);

        rootGroupNode = new GroupNode(this, rootMetaGroup);

        this.setRoot(rootGroupNode);
    }

    /**
     * Creates the <tt>ContactListTreeModel</tt> by specifying the
     * root node.
     * @param root the root node
     */
    public ContactListTreeModel(TreeNode root)
    {
        super(root);
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
     * Returns the <tt>GroupNode</tt> corresponding to the given
     * <tt>metaGroup</tt>. This method will look in deep.
     * @param metaGroup the <tt>MetaContactGroup</tt>, which corresponding node
     * we're looking for
     * @return the <tt>GroupNode</tt> corresponding to the given
     * <tt>metaGroup</tt>
     */
    public GroupNode findGroupNodeByMetaGroup(MetaContactGroup metaGroup)
    {
        if (metaGroup.equals(rootGroupNode.getMetaContactGroup()))
            return rootGroupNode;
        else
            return rootGroupNode.findGroupNode(metaGroup);
    }

    /**
     * Returns the <tt>ContactNode</tt> corresponding to the given
     * <tt>metaContact</tt>. This method will look in deep.
     * @param metaContact the <tt>MetaContact</tt>, which corresponding node
     * we're looking for
     * @return the <tt>ContactNode</tt> corresponding to the given
     * <tt>metaContact</tt>
     */
    public ContactNode findContactNodeByMetaContact(MetaContact metaContact)
    {
        MetaContactGroup parentGroup = metaContact.getParentMetaContactGroup();

        GroupNode parentGroupNode = findGroupNodeByMetaGroup(parentGroup);

        if (parentGroupNode != null)
            return parentGroupNode.findContactNode(metaContact);

        return null;
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
     * Returns the first found child <tt>ContactNode</tt>.
     * @param parentNode the parent <tt>GroupNode</tt> to search in
     * @return the first found child <tt>ContactNode</tt>.
     */
    private ContactNode findFirstContactNode(GroupNode parentNode)
    {
        // If the parent node has no children we have nothing to do here.
        if (parentNode.getChildCount() ==0)
            return null;

        TreeNode treeNode = parentNode.getFirstChild();

        if (treeNode instanceof GroupNode)
            return findFirstContactNode((GroupNode)treeNode);
        else
            return (ContactNode)treeNode;
    }
}
