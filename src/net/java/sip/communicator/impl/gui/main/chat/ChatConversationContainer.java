/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat;

import java.awt.*;

public interface ChatConversationContainer
{
    public Window getConversationContainerWindow();
    
    public void setStatusMessage(String statusMessage);
}
