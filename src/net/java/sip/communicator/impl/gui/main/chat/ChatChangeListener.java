/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat;

/**
 * Listens for changes in the chat window  and its chat panels. Fires when new
 * chat is created and focused or when changing chats through the tab pane.
 *
 * @author Damian Minkov
 */
public interface ChatChangeListener
{
    /**
     * @param panel the current visible chat panel
     */
    public void chatChanged(ChatPanel panel);
}
