/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.globaldisplaydetails;

import net.java.sip.communicator.service.globaldisplaydetails.event.*;

/**
 * The <tt>GlobalDisplayNameService</tt> offers generic access to a global
 * display name and an avatar for the local user. It could be used to show or
 * set the local user display name or avatar.
 * <p>
 * A global display name implementation could determine the information by going
 * through all different accounts' server stored information or by taking into
 * account a provisioned display name if any is available or choose any other
 * approach.
 *
 * @author Yana Stamcheva
 *
 */
public interface GlobalDisplayDetailsService
{
    /**
     * Returns the global display name to be used to identify the local user.
     *
     * @return a string representing the global local user display name
     */
    public String getGlobalDisplayName();

    /**
     * Sets the global local user display name.
     *
     * @param displayName the string representing the display name to set as
     * a global display name
     */
    public void setGlobalDisplayName(String displayName);

    /**
     * Returns the global avatar for the local user.
     *
     * @return a byte array containing the global avatar for the local user
     */
    public byte[] getGlobalDisplayAvatar();

    /**
     * Sets the global display avatar for the local user.
     *
     * @param avatar the byte array representing the avatar to set
     */
    public void setGlobalDisplayAvatar(byte[] avatar);

    /**
     * Adds the given <tt>GlobalDisplayDetailsListener</tt> to listen for change
     * events concerning the global display details.
     *
     * @param l the <tt>GlobalDisplayDetailsListener</tt> to add
     */
    public void addGlobalDisplayDetailsListener(
        GlobalDisplayDetailsListener l);

    /**
     * Removes the given <tt>GlobalDisplayDetailsListener</tt> listening for
     * change events concerning the global display details.
     *
     * @param l the <tt>GlobalDisplayDetailsListener</tt> to remove
     */
    public void removeGlobalDisplayDetailsListener(
        GlobalDisplayDetailsListener l);
}