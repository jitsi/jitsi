/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.jabberaccregwizz;

/**
 * <tt>ValidatingPanel</tt> validate values in panel and give access of
 * the validation to the registration form.
 * 
 * @author Damian Minkov
 */
public interface ValidatingPanel
{
    /**
     * Whether current inserted values into the panel are valid and enough
     * to continue with account creation/modification. 
     * @return whether the input values are ok to continue with account
     * creation/modification.
     */
    public boolean isValidated();
}
