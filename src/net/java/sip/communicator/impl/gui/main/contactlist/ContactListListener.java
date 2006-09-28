/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.util.*;

public interface ContactListListener extends EventListener
{
    
    public void contactSelected(ContactListEvent evt);
    
    public void protocolContactSelected(ContactListEvent evt);
    
}
