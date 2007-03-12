/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat.conference;

import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.service.protocol.event.*;

/**
 * The <tt>MultiUserChatManager</tt> is the one that manages chat room
 * invitations.
 * 
 * @author Yana Stamcheva
 */
public class MultiUserChatManager
    implements  InvitationListener,
                InvitationRejectionListener
{
    private MainFrame mainFrame;
    
    /**
     * Creates an instance of <tt>MultiUserChatManager</tt>, by passing to it
     * the main application window object.
     * 
     * @param mainFrame the main application window
     */
    public MultiUserChatManager(MainFrame mainFrame)
    {
        this.mainFrame = mainFrame;
    }
    
    public void invitationReceived(InvitationReceivedEvent evt)
    {
        
    }

    public void invitationRejected(InvitationRejectedEvent evt)
    {
    
    }    
}
