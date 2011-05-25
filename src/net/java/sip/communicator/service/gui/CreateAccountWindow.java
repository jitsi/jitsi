/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.gui;

/**
 * 
 * @author Yana Stamcheva
 */
public interface CreateAccountWindow
{
    /**
     * Shows or hides this create account window.
     *
     * @param visible <tt>true</tt> to show this window, <tt>false</tt> -
     * otherwise
     */
    public void setVisible(boolean visible);

    /**
     * Sets the selected wizard.
     *
     * @param wizard the wizard to select
     * @param isCreatedForm indicates if the selected wizard should be opened
     * in create account mode
     */
    public void setSelectedWizard(  AccountRegistrationWizard wizard,
                                    boolean isCreateAccount);
}
