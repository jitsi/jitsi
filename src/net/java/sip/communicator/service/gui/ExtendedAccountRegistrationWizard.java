/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.gui;

/**
 * The <tt>ExtendedAccountRegistrationWizard</tt> allows to specify if a sign up
 * form is supported.
 *
 * @author Yana Stamcheva
 */
public abstract class ExtendedAccountRegistrationWizard
    extends AccountRegistrationWizard
{
    /**
     * Indicates if a sign up form is supported by this wizard.
     *
     * @return <tt>true</tt> if a sign up form is supported by this wizard,
     * <tt>false</tt> - otherwise
     */
    public abstract boolean isSignupSupported();

    /**
     * Sets the create account view of this registration wizard.
     */
    public abstract void setCreateAccountView();
}
