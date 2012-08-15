/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.customcontactactions;

import net.java.sip.communicator.service.protocol.*;

/**
 * A custom contact action, used to define an action that can be represented in
 * the contact list entry in the user interface.
 *
 * @author Damian Minkov
 * @author Yana Stamcheva
 */
public interface ContactAction<T>
{
    /**
     * Invoked when an action occurs.
     *
     * @param actionSource the source of the action
     * @param int x the x coordinate of the action
     * @param int y the y coordinate of the action
     */
    public void actionPerformed(T actionSource, int x, int y)
        throws OperationFailedException;

    /**
     * The icon used by the UI to visualize this action.
     * @return the button icon.
     */
    public byte[] getIcon();

    /**
     * The icon used by the UI to visualize this action.
     * @return the button icon.
     */
    public byte[] getPressedIcon();

    /**
     * Indicates if this action is visible for the given <tt>actionSource</tt>.
     *
     * @param actionSource the action source for which we're verifying the
     * action.
     * @return <tt>true</tt> if the action should be visible for the given
     * <tt>actionSource</tt>, <tt>false</tt> - otherwise
     */
    public boolean isVisible(T actionSource);
}