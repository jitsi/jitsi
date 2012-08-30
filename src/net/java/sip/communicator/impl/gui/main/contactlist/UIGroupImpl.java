package net.java.sip.communicator.impl.gui.main.contactlist;

import net.java.sip.communicator.service.gui.*;

public abstract class UIGroupImpl
    extends UIGroup
{
    /**
     * Returns the <tt>GroupNode</tt> corresponding to this <tt>UIGroup</tt>.
     * The is the actual node used in the contact list component data model.
     *
     * @return the <tt>GroupNode</tt> corresponding to this <tt>UIGroup</tt>
     */
    public abstract GroupNode getGroupNode();

    /**
     * Sets the <tt>GroupNode</tt> corresponding to this <tt>UIGroup</tt>.
     *
     * @param groupNode the <tt>GroupNode</tt> to set. The is the actual
     * node used in the contact list component data model.
     */
    public abstract void setGroupNode(GroupNode groupNode);
}
