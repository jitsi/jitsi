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
package net.java.sip.communicator.service.protocol;

import org.json.simple.*;

import java.util.*;

/**
 * The operation set provides functionality specific to Jitsi Meet WebRTC
 * conference and is currently used in the SIP gateway.
 *
 * @author Pawel Domas
 * @author Cristian Florin Ghita
 */
public interface OperationSetJitsiMeetTools
    extends OperationSet
{
    /**
     * Adds given <tt>listener</tt> to the list of
     * {@link JitsiMeetRequestListener}s.
     * @param listener the {@link JitsiMeetRequestListener} to be notified about
     *                 future events.
     */
    void addRequestListener(JitsiMeetRequestListener listener);

    /**
     * Removes given <tt>listener</tt> from the list of
     * {@link JitsiMeetRequestListener}s.
     * @param listener the {@link JitsiMeetRequestListener} that will be no
     *                 longer notified about Jitsi Meet events.
     */
    void removeRequestListener(JitsiMeetRequestListener listener);

    /**
     *  Sends a JSON to the specified <tt>callPeer</tt>.
     *
     * @param callPeer the CallPeer to which we send the JSONObject to.
     * @param jsonObject the JSONObject that we send to the CallPeer.
     * @param parameterMap a map which is used to set specific parameters
     * for the protocol used to send the jsonObject.
     *
     * @throws OperationFailedException thrown in case anything goes wrong
     * while preparing or sending the JSONObject.
     *
     */
    void sendJSON(CallPeer callPeer,
        JSONObject jsonObject,
        Map<String, Object> parameterMap)
                        throws OperationFailedException;

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
         * @param extraData extra data passes for this request in the form
         *                  of Map<name, value>.
         */
        void onJoinJitsiMeetRequest(
            Call call,
            String jitsiMeetRoom,
            Map<String, String> extraData);

        /**
         * Event is fired after startmuted extension is received.
         *
         * @param startMutedFlags startMutedFlags[0] represents
         * the muted status of audio stream.
         * startMuted[1] represents the muted status of video stream.
         */
        void onSessionStartMuted(boolean[] startMutedFlags);

        /**
         * Event is fired when a JSON is received from a CallPeer.
         *
         * @param callPeer the CallPeer that sent the JSONObject.
         * @param jsonObject the JSONObject that was received from the CallPeer.
         * @param parameterMap a map which describes protocol specific
         * parameters used to receive the jsonObject.
         */
        void onJSONReceived(CallPeer callPeer,
                            JSONObject jsonObject,
                            Map<String, Object> parameterMap);
    }
}
