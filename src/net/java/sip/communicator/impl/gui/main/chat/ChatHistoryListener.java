/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat;

/**
 * The <tt>ChatHistoryListener</tt> is notified each time the chat history of
 * a <tt>ChatPanel</tt> changes.
 * 
 * @author Yana Stamcheva
 */
public interface ChatHistoryListener
{
    /**
     * Notified when the history of the given <tt>ChatPanel</tt> changes.
     *
     * @param chatPanel the <tt>ChatPanel</tt>, which history has changed
     */
    public void chatHistoryChanged(ChatPanel chatPanel);
}
