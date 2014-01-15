/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.customcontactactions;

import net.java.sip.communicator.service.protocol.*;

/**
 * A custom contact action menu item, used to define an action that can be 
 * represented in the contact list entry in the user interface.
 *
 * @author Hristo Terezov
 */
public interface ContactActionMenuItem<T>
{
    /**
     * Invoked when an action occurs.
     *
     * @param actionSource the source of the action
     */
    public void actionPerformed(T actionSource)
        throws OperationFailedException;

    /**
     * The icon used by the UI to visualize this action.
     * @return the button icon.
     */
    public byte[] getIcon();

    /**
     * Returns the text of the component to create for this contact
     * action.
     * 
     * @param actionSource the action source for associated with the
     * action.
     * @return the tool tip text of the component to create for this contact
     * action
     */
    public String getText(T actionSource);

    /**
     * Indicates if this action is visible for the given <tt>actionSource</tt>.
     *
     * @param actionSource the action source for which we're verifying the
     * action.
     * @return <tt>true</tt> if the action should be visible for the given
     * <tt>actionSource</tt>, <tt>false</tt> - otherwise
     */
    public boolean isVisible(T actionSource);
    
    /**
     * 
     * @return
     */
    public char getMnemonics();
    
    /**
     * Returns <tt>true</tt> if the item should be enabled and <tt>false</tt>
     *  - not.
     *  
     * @param actionSource the action source for which we're verifying the
     * action.
     * @return <tt>true</tt> if the item should be enabled and <tt>false</tt>
     *  - not.
     */
    public boolean isEnabled(T actionSource);
    
    /**
     * Returns <tt>true</tt> if the item should be a check box and 
     * <tt>false</tt> if not
     * 
     * @return <tt>true</tt> if the item should be a check box and 
     * <tt>false</tt> if not
     */
    public boolean isCheckBox();

    /**
     * Returns the state of the item if the item is check box.
     * 
     * @param actionSource the action source for which we're verifying the
     * action.
     * @return the state of the item.
     */
    public boolean isSelected(T actionSource);
}