/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>UIContact</tt> represents the user interface contact contained in the
 * contact list component.
 *
 * @author Yana Stamcheva
 */
public interface UIContact
{
    /**
     * Returns the descriptor of this contact.
     *
     * @return the descriptor of this contact
     */
    public Object getDescriptor();

    /**
     * Returns the display name of this contact.
     *
     * @return the display name of this contact
     */
    public String getDisplayName();

    /**
     * Returns the display details of this contact. These would be shown
     * whenever the contact is selected.
     *
     * @return the display details of this contact
     */
    public String getDisplayDetails();

    /**
     * Returns the index of this contact in its source.
     *
     * @return the source index
     */
    public int getSourceIndex();

    /**
     * Returns the avatar of this contact.
     *
     * @param isSelected indicates if the contact is selected
     * @param width the width of the avatar
     * @param height the height of the avatar
     * @return  the avatar of this contact
     */
    public ImageIcon getAvatar(boolean isSelected, int width, int height);

    /**
     * Returns the status icon of this contact or null if no status is
     * available.
     *
     * @return the status icon of this contact or null if no status is
     * available
     */
    public ImageIcon getStatusIcon();

    /**
     * Creates a tool tip for this contact. If such tooltip is
     * provided it would be shown on mouse over over this <tt>UIContact</tt>.
     *
     * @return the tool tip for this contact descriptor
     */
    public ExtendedTooltip getToolTip();

    /**
     * Returns the right button menu component.
     *
     * @return the right button menu component
     */
    public JPopupMenu getRightButtonMenu();

    /**
     * Returns the parent group.
     *
     * @return the parent group
     */
    public UIGroup getParentGroup();

    /**
     * Sets the given <tt>UIGroup</tt> to be the parent group of this
     * <tt>UIContact</tt>.
     *
     * @param parentGroup the parent <tt>UIGroup</tt> of this contact
     */
    public void setParentGroup(UIGroup parentGroup);

    /**
     * Returns an <tt>Iterator</tt> over a list of the search strings of this
     * contact.
     *
     * @return an <tt>Iterator</tt> over a list of the search strings of this
     * contact
     */
    public Iterator<String> getSearchStrings();

    /**
     * Returns the corresponding <tt>ContactNode</tt>. The <tt>ContactNode</tt>
     * is the real node that is stored in the contact list component data model.
     *
     * @return the corresponding <tt>ContactNode</tt>
     */
    public ContactNode getContactNode();

    /**
     * Sets the given <tt>contactNode</tt>. The <tt>ContactNode</tt>
     * is the real node that is stored in the contact list component data model.
     *
     * @param contactNode the <tt>ContactNode</tt> that corresponds to this
     * <tt>UIGroup</tt>
     */
    public void setContactNode(ContactNode contactNode);

    /**
     * Returns the default <tt>ContactDetail</tt> to use for any operations
     * depending to the given <tt>OperationSet</tt> class.
     *
     * @param opSetClass the <tt>OperationSet</tt> class we're interested in
     * @return the default <tt>ContactDetail</tt> to use for any operations
     * depending to the given <tt>OperationSet</tt> class
     */
    public UIContactDetail getDefaultContactDetail(
        Class<? extends OperationSet> opSetClass);

    /**
     * Returns a list of all <tt>UIContactDetail</tt>s corresponding to the
     * given <tt>OperationSet</tt> class.
     *
     * @param opSetClass the <tt>OperationSet</tt> class we're looking for
     * @return a list of all <tt>UIContactDetail</tt>s corresponding to the
     * given <tt>OperationSet</tt> class
     */
    public List<UIContactDetail> getContactDetailsForOperationSet(
        Class<? extends OperationSet> opSetClass);
}
