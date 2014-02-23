/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
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
     * The icon representing the ChatTransport has changed.
     */
    public static final int ICON_UPDATED = 1;

    /**
     * Called when the current {@link ChatTransport} has
     * changed.
     *
     * @param chatSession the {@link ChatSession} it's current
     * {@link ChatTransport} has changed
     */
    public void currentChatTransportChanged(ChatSession chatSession);

    /**
     * When a property of the chatTransport has changed.
     * @param eventID the event id representing the property of the transport
     * that has changed.
     */
    public void currentChatTransportUpdated(int eventID);
}
