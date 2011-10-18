/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.gui.event;

import java.util.*;

/**
 * The listener interface for receiving focus events on a <tt>Chat</tt>.
 * 
 * @author Yana Stamcheva
 */
public interface ChatFocusListener extends EventListener {

    /**
     * Indicates that a <tt>Chat</tt> has gained the focus.
     * 
     * @param event the ChatFocusEvent containing the corresponding chat.
     */
    public void chatFocusGained(ChatFocusEvent event);
    
    /**
     * Indicates that a <tt>Chat</tt> has lost the focus.
     * 
     * @param event the ChatFocusEvent containing the corresponding chat.
     */
    public void chatFocusLost(ChatFocusEvent event);
}
