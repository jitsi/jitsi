/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.contacteventhandler;

import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>ContactEventHandler</tt> is meant to be used from other bundles in
 * order to change the default behavior of events generated when clicking
 * a contact. The GUI implementation should take in consideration all registered
 * <tt>ContactEventHandler</tt>s when managing contact list events.
 *
 * @author Yana Stamcheva
 */
public interface ContactEventHandler
{
    /**
     * Indicates that a contact in the contact list was clicked.
     *
     * @param contact the selected <tt>Contact</tt>
     * @param clickCount the count of clicks
     */
    public void contactClicked(Contact contact, int clickCount);
}
