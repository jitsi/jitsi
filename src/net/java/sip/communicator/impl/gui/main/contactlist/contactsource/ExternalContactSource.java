/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist.contactsource;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.service.contactsource.*;

/**
 * The <tt>ExternalContactSource</tt> is the UI abstraction of the
 * <tt>ContactSourceService</tt>.
 *
 * @author Yana Stamcheva
 */
public class ExternalContactSource
{
    /**
     * The <tt>SourceUIGroup</tt> containing all contacts from this source.
     */
    private final SourceUIGroup sourceUIGroup;

    /**
     * The contact source.
     */
    private final ContactSourceService contactSource;

    /**
     * Creates an <tt>ExternalContactSource</tt> based on the given
     * <tt>ContactSourceService</tt>.
     *
     * @param contactSource the <tt>ContactSourceService</tt>, on which this
     * <tt>ExternalContactSource</tt> is based
     */
    public ExternalContactSource(ContactSourceService contactSource)
    {
        this.contactSource = contactSource;

        sourceUIGroup = new SourceUIGroup(contactSource.getDisplayName());
    }

    /**
     * Returns the corresponding <tt>ContactSourceService</tt>.
     *
     * @return the corresponding <tt>ContactSourceService</tt>
     */
    public ContactSourceService getContactSourceService()
    {
        return contactSource;
    }

    /**
     * Returns the UI group for this contact source. There's only one group
     * descriptor per external source.
     *
     * @return the group descriptor
     */
    public UIGroup getUIGroup()
    {
        return sourceUIGroup;
    }

    /**
     * Returns the <tt>UIContact</tt> corresponding to the given
     * <tt>sourceContact</tt>.
     *
     * @param sourceContact the <tt>SourceContact</tt>, for which we search a
     * corresponding <tt>UIContact</tt>
     * @return the <tt>UIContact</tt> corresponding to the given
     * <tt>sourceContact</tt>
     */
    public UIContact createUIContact(SourceContact sourceContact)
    {
        return new SourceUIContact(sourceContact, sourceUIGroup);
    }

    /**
     * The <tt>SourceUIGroup</tt> is the implementation of the UIGroup for the
     * <tt>ExternalContactSource</tt>. It takes the name of the source and
     * sets it as a group name.
     */
    private class SourceUIGroup
        implements UIGroup
    {
        /**
         * The display name of the group.
         */
        private final String displayName;

        /**
         * The corresponding group node.
         */
        private GroupNode groupNode;

        /**
         * Creates an instance of <tt>SourceUIGroup</tt>.
         * @param name the name of the group
         */
        public SourceUIGroup(String name)
        {
            this.displayName = name;
        }

        /**
         * Returns null to indicate that this group doesn't have a parent group
         * and can be added directly to the root group.
         * @return null
         */
        public UIGroup getParentGroup()
        {
            return null;
        }

        /**
         * Returns -1 to indicate that this group doesn't have a source index.
         * @return -1
         */
        public int getSourceIndex()
        {
            if (contactSource.getIdentifier()
                .equals(ContactSourceService.CALL_HISTORY))
                return Integer.MAX_VALUE;

            return Integer.MAX_VALUE - 1;
        }

        /**
         * Returns <tt>false</tt> to indicate that this group is always opened.
         * @return false
         */
        public boolean isGroupCollapsed()
        {
            return false;
        }

        /**
         * Returns the display name of this group.
         * @return the display name of this group
         */
        public String getDisplayName()
        {
            return displayName;
        }

        /**
         * Returns -1 to indicate that the child count is unknown.
         * @return -1
         */
        public int countChildContacts()
        {
            return -1;
        }

        /**
         * Returns -1 to indicate that the child count is unknown.
         * @return -1
         */
        public int countOnlineChildContacts()
        {
            return -1;
        }

        /**
         * Returns the display name of the group.
         * @return the display name of the group
         */
        public Object getDescriptor()
        {
            return displayName;
        }

        /**
         * Returns null to indicate that this group doesn't have an identifier.
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
         * Sets the given <tt>groupNode</tt>.
         * @param groupNode the <tt>GroupNode</tt> to set
         */
        public void setGroupNode(GroupNode groupNode)
        {
            this.groupNode = groupNode;
        }

        /**
         * Returns the right button menu for this group.
         * @return null
         */
        public JPopupMenu getRightButtonMenu()
        {
            return null;
        }
    }
}
