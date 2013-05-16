/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.credentialsstorage;

/**
 * Master password input dialog.
 *
 * @author Damian Minkov
 */
public interface MasterPasswordInputService
{
    /**
     * Shows an input dialog to the user to obtain the master password.
     *
     * @param prevSuccess <tt>true</tt> if any previous call returned a correct
     * master password and there is no need to show an extra "verification
     * failed" message
     * @return the master password obtained from the user or <tt>null</tt> if
     * none was provided
     */
    public String showInputDialog(boolean prevSuccess);
}
