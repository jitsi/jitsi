/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.gui;

import java.awt.event.*;

import net.java.sip.communicator.service.gui.event.*;

/**
 * The <tt>Chat</tt> interface is meant to be implemented by the GUI component
 * class representing a chat. Through the <i>isChatFocused</i> method the other
 * bundles could check the visibility of the chat component. The
 * <tt>ChatFocusListener</tt> is used to inform other bundles when a chat has
 * changed its focus state.
 * 
 * @author Yana Stamcheva
 */
public interface Chat
{
    /**
     * Checks if this <tt>Chat</tt> is currently focused.
     * 
     * @return TRUE if the chat is focused, FALSE - otherwise
     */
    public boolean isChatFocused();
    
    /**
     * Returns the message written by user in the chat write area.
     * 
     * @return the message written by user in the chat write area
     */
    public String getMessage();

    /**
     * Bring this chat to front if <tt>b</tt> is true, hide it otherwise.
     *
     * @param isVisible tells if the chat will be made visible or not.
     */
    public void setChatVisible(boolean isVisible);
    
    /**
     * Sets the given message as a message in the chat write area.
     * 
     * @param message the text that would be set to the chat write area 
     */
    public void setMessage(String message);
    
    /**
     * Adds the given <tt>ChatFocusListener</tt> to this <tt>Chat</tt>.
     * The <tt>ChatFocusListener</tt> is used to inform other bundles when a
     * chat has changed its focus state.
     * 
     * @param l the <tt>ChatFocusListener</tt> to add
     */
    public void addChatFocusListener(ChatFocusListener l);
    
    /**
     * Removes the given <tt>ChatFocusListener</tt> from this <tt>Chat</tt>.
     * The <tt>ChatFocusListener</tt> is used to inform other bundles when a
     * chat has changed its focus state.
     * 
     * @param l the <tt>ChatFocusListener</tt> to remove
     */
    public void removeChatFocusListener(ChatFocusListener l);
    
    /**
     * Adds the given {@link KeyListener} to this <tt>Chat</tt>.
     * The <tt>KeyListener</tt> is used to inform other bundles when a user has
     * typed in the chat editor area.
     * 
     * @param l the <tt>KeyListener</tt> to add
     */
    public void addChatEditorKeyListener(KeyListener l);
    
    /**
     * Removes the given {@link KeyListener} from this <tt>Chat</tt>.
     * The <tt>KeyListener</tt> is used to inform other bundles when a user has
     * typed in the chat editor area.
     * 
     * @param l the <tt>ChatFocusListener</tt> to remove
     */
    public void removeChatEditorKeyListener(KeyListener l);
}
