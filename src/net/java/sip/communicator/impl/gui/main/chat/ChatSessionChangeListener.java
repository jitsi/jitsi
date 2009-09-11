/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat;

/**
 * Listens for changes in {@link ChatSession}.
 * @author George Politis
 */
public interface ChatSessionChangeListener
{
    /**
     * Called when the current {@link ChatTransport} has
     * changed.
     * 
     * @param chatSession the {@link ChatSession} it's current 
     * {@link ChatTransport} has changed 
     */
    public void currentChatTransportChanged(ChatSession chatSession);
}
