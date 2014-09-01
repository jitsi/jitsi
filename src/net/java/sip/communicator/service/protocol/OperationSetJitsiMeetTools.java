/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import org.jivesoftware.smack.packet.*;

/**
 * The operation set provides functionality specific to Jitsi Meet WebRTC
 * conference.
 *
 * @author Pawel Domas
 */
public interface OperationSetJitsiMeetTools
    extends OperationSet
{
    /**
     * Includes given <tt>PacketExtension</tt> in multi user chat presence and
     * sends presence update packet to the chat room.
     * @param chatRoom the <tt>ChatRoom</tt> for which the presence will be
     *                 updated.
     * @param extension the <tt>PacketExtension</tt> to be included in MUC
     *                  presence.
     */
    public void sendPresenceExtension(ChatRoom chatRoom,
                                      PacketExtension extension);

    /**
     * Sets the status message of our MUC presence and sends presence status
     * update packet to the server.
     * @param chatRoom the <tt>ChatRoom</tt> for which the presence status
     *                 message will be changed.
     * @param statusMessage the text that will be used as our presence status
     *                      message in the MUC.
     */
    public void setPresenceStatus(ChatRoom chatRoom, String statusMessage);
}
