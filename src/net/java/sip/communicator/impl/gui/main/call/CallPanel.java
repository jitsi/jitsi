/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import javax.swing.*;

import net.java.sip.communicator.service.protocol.*;

public class CallPanel
    extends JPanel
{
    public static final String INCOMING_CALL = "IncomingCall";
    
    public static final String OUTGOING_CALL = "OutgoingCall";
    
    private Call call;
    
    public CallPanel(Call call, String callType)
    {
        this.call = call;
        
        if(callType.equals(INCOMING_CALL)) {
            this.processIncomingCall();
        }
        else {
            this.processOutgoingCall();
        }
    }
    
    private void processIncomingCall()
    {
        
    }
    
    private void processOutgoingCall()
    {
        
    }
    
    public String getTitle()
    {        
        return null;
    }
}
