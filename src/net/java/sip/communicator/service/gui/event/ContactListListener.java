/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.gui.event;

import java.util.*;

/**
 * Listens for events coming from mouse events over the contact list. For
 * example a contact been clicked or a group been selected.
 *
 * @author Yana Stamcheva
 */
public interface ContactListListener extends EventListener
{
    /**
     * Indicates that a group has been selected.
     *
     * @param evt the <tt>ContactListEvent</tt> that has been triggered from
     * the user selection
     */
    public void groupClicked(ContactListEvent evt);

    /**
     * Indicates that a group has been selected.
     *
     * @param evt the <tt>ContactListEvent</tt> that has been triggered from
     * the user selection
     */
    public void groupSelected(ContactListEvent evt);

    /**
     * Indicates that a contact has been clicked.
     *
     * @param evt the <tt>ContactListEvent</tt> that has been triggered from
     * the user click
     */
    public void contactClicked(ContactListEvent evt);

    /**
     * Indicates that a contact has been selected.
     *
     * @param evt the <tt>ContactListEvent</tt> that has been triggered from
     * the user selection
     */
    public void contactSelected(ContactListEvent evt);
}
