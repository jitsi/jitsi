/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.impl.gui.main.chat;

import javax.swing.*;

import net.java.sip.communicator.service.protocol.*;

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

    /**
     * Adds the given <tt>conferenceDescription</tt> to the list of chat 
     * conferences in this chat renderer.
     * @param conferenceDescription the conference to add.
     */
    public void addChatConferenceCall(
        ConferenceDescription conferenceDescription);

    /**
     * Removes the given <tt>conferenceDescription</tt> from the list of chat 
     * conferences in this chat panel chat.
     * @param conferenceDescription the conference to remove.
     */
    public void removeChatConferenceCall(
        ConferenceDescription conferenceDescription);
    
    /**
     * Sets the visibility of conferences panel to <tt>true</tt> or 
     * <tt>false</tt>
     * 
     * @param isVisible if <tt>true</tt> the panel is visible.
     */
    public void setConferencesPanelVisible(boolean isVisible);
    
    /**
     * This method is called when the local user publishes a 
     * <tt>ConferenceDescription</tt> instance.
     * 
     * @param conferenceDescription the <tt>ConferenceDescription</tt> instance 
     * associated with the conference.
     */
    public void chatConferenceDescriptionSent(
        ConferenceDescription conferenceDescription);
}
