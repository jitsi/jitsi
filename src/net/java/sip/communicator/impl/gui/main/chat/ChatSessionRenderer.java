/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat;

import javax.swing.*;

/**
 * The <tt>ChatSessionRenderer</tt> is the connector between the
 * <tt>ChatSession</tt> and the <tt>ChatPanel</tt>, which represents the UI
 * part of the chat.
 *
 * @author Yana Stamcheva
 */
public interface ChatSessionRenderer
{
    /**
     * Sets the name of the given chat contact.
     *
     * @param chatContact the chat contact to be modified.
     * @param name the new name.
     */
    public void setContactName(ChatContact<?> chatContact, String name);

    /**
     * Adds the given chat transport to the UI.
     *
     * @param chatTransport the chat transport to add.
     */
    public void addChatTransport(ChatTransport chatTransport);

    /**
     * Removes the given chat transport from the UI.
     *
     * @param chatTransport the chat transport to remove.
     */
    public void removeChatTransport(ChatTransport chatTransport);

    /**
     * Adds the given chat contact to the UI.
     *
     * @param chatContact the chat contact to add.
     */
    public void addChatContact(ChatContact<?> chatContact);

    /**
     * Removes the given chat contact from the UI.
     *
     * @param chatContact the chat contact to remove.
     */
    public void removeChatContact(ChatContact<?> chatContact);

    /**
     * Removes all chat contacts from the contact list of the chat.
     */
    public void removeAllChatContacts();

    /**
     * Updates the status of the given chat transport.
     *
     * @param chatTransport the chat transport to update.
     */
    public void updateChatTransportStatus(ChatTransport chatTransport);

    /**
     * Sets the given <tt>chatTransport</tt> to be the selected chat transport.
     *
     * @param chatTransport the <tt>ChatTransport</tt> to select
     * @param isMessageOrFileTransferReceived Boolean telling us if this change
     * of the chat transport correspond to an effective switch to this new
     * transform (a mesaage received from this transport, or a file transfer
     * request received, or if the resource timeouted), or just a status update
     * telling us a new chatTransport is now available (i.e. another device has
     * startup).
     */
    public void setSelectedChatTransport(
            ChatTransport chatTransport,
            boolean isMessageOrFileTransferReceived);

    /**
     * Updates the status of the given chat contact.
     *
     * @param chatContact the chat contact to update.
     * @param statusMessage the status message to show to the user.
     */
    public void updateChatContactStatus(ChatContact<?> chatContact,
                                        String statusMessage);

    /**
     * Sets the chat subject.
     *
     * @param subject the new subject to set.
     */
    public void setChatSubject(String subject);

    /**
     * Sets the chat icon.
     *
     * @param icon the chat icon to set
     */
    public void setChatIcon(Icon icon);
}
