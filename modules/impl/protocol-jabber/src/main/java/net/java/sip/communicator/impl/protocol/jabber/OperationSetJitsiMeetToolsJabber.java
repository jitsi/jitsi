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
package net.java.sip.communicator.impl.protocol.jabber;

import net.java.sip.communicator.service.protocol.*;
import org.jivesoftware.smack.packet.*;

/**
 * The operation set provides functionality specific to Jitsi Meet WebRTC
 * conference and is currently used in the SIP gateway.
 *
 * @author Pawel Domas
 * @author Cristian Florin Ghita
 */
public interface OperationSetJitsiMeetToolsJabber
    extends OperationSetJitsiMeetTools
{
    /**
     * Adds given feature to communication protocol capabilities list of parent
     * {@link ProtocolProviderService}.
     *
     * @param featureName feature name to be added to the capabilities list.
     */
    void addSupportedFeature(String featureName);

    /**
     * Removes given feature from communication protocol capabilities list of
     * parent {@link ProtocolProviderService}.
     *
     * @param featureName feature name to be removed from the capabilities list.
     */
    void removeSupportedFeature(String featureName);

    /**
     * Includes given <tt>ExtensionElement</tt> in multi user chat presence and
     * sends presence update packet to the chat room.
     * @param chatRoom the <tt>ChatRoom</tt> for which the presence will be
     *                 updated.
     * @param extension the <tt>ExtensionElement</tt> to be included in MUC
     *                  presence.
     */
    void sendPresenceExtension(ChatRoom chatRoom,
        ExtensionElement extension);

    /**
     * Removes given <tt>ExtensionElement</tt> from the multi user chat presence
     * and sends presence update packet to the chat room.
     * @param chatRoom the <tt>ChatRoom</tt> for which the presence will be
     *                 updated.
     * @param extension the <tt>ExtensionElement</tt> to be removed from the MUC
     *                  presence.
     */
    void removePresenceExtension(ChatRoom chatRoom,
        ExtensionElement extension);

    /**
     * Sets the status message of our MUC presence and sends presence status
     * update packet to the server.
     * @param chatRoom the <tt>ChatRoom</tt> for which the presence status
     *                 message will be changed.
     * @param statusMessage the text that will be used as our presence status
     *                      message in the MUC.
     */
    void setPresenceStatus(ChatRoom chatRoom, String statusMessage);
}
