/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.presence;

import java.awt.image.*;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.service.protocol.*;

public abstract class StatusSelectorBox extends SIPCommMenu
{   
    public void startConnecting(BufferedImage[] images){}
    
    public void updateStatus(){}
        
    public int getAccountIndex(){return -1;}
    
    public void setAccountIndex(int index){}    
}
