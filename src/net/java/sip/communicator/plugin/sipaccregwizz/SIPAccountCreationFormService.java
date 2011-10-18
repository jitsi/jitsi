/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.sipaccregwizz;

import java.awt.*;

/**
 * The <tt>CreateAccountService</tt> is meant to be implemented by specific
 * account registration implementations, which contain an account create form.
 *
 * @author Yana Stamcheva
 */
public interface SIPAccountCreationFormService
{
    /**
     * Creates an account for a specific server.
     * @return the new account
     */
    public NewAccount createAccount();

    /**
     * Returns the form, which would be used by the user to create a new
     * account.
     * @return the component of the form
     */
    public Component getForm();

    /**
     * Clears all the data previously entered in the form.
     */
    public void clear();
}
