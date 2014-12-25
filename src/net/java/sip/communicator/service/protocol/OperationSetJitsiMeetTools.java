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
 * conference and is currently used in the SIP gateway.
 *
 * @author Pawel Domas
 */
public interface OperationSetJitsiMeetTools
    extends OperationSet
{
    /**
     * Adds given feature to communication protocol capabilities list of parent
     * {@link ProtocolProviderService}.
     *
     * @param featureName feature name to be added to the capabilities list.
     */
    public void addSupportedFeature(String featureName);

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

    /**
     * Adds given <tt>listener</tt> to the list of
     * {@link JitsiMeetRequestListener}s.
     * @param listener the {@link JitsiMeetRequestListener} to be notified about
     *                 future events.
     */
    public void addRequestListener(JitsiMeetRequestListener listener);

    /**
     * Removes given <tt>listener</tt> from the list of
     * {@link JitsiMeetRequestListener}s.
     * @param listener the {@link JitsiMeetRequestListener} that will be no
     *                 longer notified about Jitsi Meet events.
     */
    public void removeRequestListener(JitsiMeetRequestListener listener);

    /**
     * Interface used to handle Jitsi Meet conference requests.
     */
    interface JitsiMeetRequestListener
    {
        /**
         * Events is fired for an incoming call that contains information about
         * Jitsi Meet conference room to be joined.
         * @param call the incoming {@link Call} instance.
         * @param jitsiMeetRoom the name of multi user chat room that is hosting
         *                      Jitsi Meet conference.
         */
        void onJoinJitsiMeetRequest(Call call, String jitsiMeetRoom);
    }
}
