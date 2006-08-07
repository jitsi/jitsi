/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.message;

import java.awt.Window;

public interface ChatConversationContainer {

    public void setStatusMessage(String message);
    
    public Window getWindow();
}
