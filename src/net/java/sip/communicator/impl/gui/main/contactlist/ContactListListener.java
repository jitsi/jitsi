/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.util.*;

/**
 * Listens for events coming from the contact list.
 * 
 * @author Yana Stamcheva
 */
public interface ContactListListener extends EventListener
{
    /**
     * 
     * @param evt
     */
    public void groupSelected(ContactListEvent evt);
    
    /**
     * 
     * @param evt
     */
    public void contactClicked(ContactListEvent evt);
    
    /**
     * 
     * @param evt
     */
    public void protocolContactClicked(ContactListEvent evt);
    
}
