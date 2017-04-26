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
package net.java.sip.communicator.impl.protocol.sip;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.jivesoftware.smack.packet.*;

import java.util.*;
import java.util.concurrent.*;

/**
 * The SIP implementation of {@link OperationSetJitsiMeetTools}.
 *
 * @author Pawel Domas
 */
public class OperationSetJitsiMeetToolsSipImpl
    implements OperationSetJitsiMeetTools
{
    /**
     * Name of extra INVITE header which specifies name of MUC room that is
     * hosting the Jitsi Meet conference.
     */
    public String JITSI_MEET_ROOM_HEADER = "Jitsi-Conference-Room";

    /**
     * Property name of extra INVITE header which specifies name of MUC room
     * that is hosting the Jitsi Meet conference.
     */
    private static final String JITSI_MEET_ROOM_HEADER_PROPERTY
        = "JITSI_MEET_ROOM_HEADER_NAME";

    /**
     * The logger used by this class.
     */
    private final static Logger logger
        = Logger.getLogger(OperationSetJitsiMeetToolsSipImpl.class);

    /**
     * The list of {@link JitsiMeetRequestListener}.
     */
    private final List<JitsiMeetRequestListener> requestHandlers
        = new CopyOnWriteArrayList<JitsiMeetRequestListener>();

    /**
     * Constructs new OperationSetJitsiMeetToolsSipImpl.
     * @param parentProvider the parent provider.
     */
    public OperationSetJitsiMeetToolsSipImpl(
        ProtocolProviderServiceSipImpl parentProvider)
    {
        AccountID account = parentProvider.getAccountID();
        // Specify custom header names
        JITSI_MEET_ROOM_HEADER = account.getAccountPropertyString(
            JITSI_MEET_ROOM_HEADER_PROPERTY, JITSI_MEET_ROOM_HEADER);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addRequestListener(JitsiMeetRequestListener requestHandler)
    {
        this.requestHandlers.add(requestHandler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeRequestListener(JitsiMeetRequestListener requestHandler)
    {
        this.requestHandlers.remove(requestHandler);
    }

    /**
     * Notifies all registered {@link JitsiMeetRequestListener} about incoming
     * call that contains name of the MUC room which is hosting Jitsi Meet
     * conference.
     * @param call the incoming {@link Call} instance.
     * @param callHeaders map of all the sip headers (<name,value>)
     */
    public void notifyJoinJitsiMeetRoom(
        Call call, Map<String, String> callHeaders)
    {
        // Parses Jitsi Meet room name header
        String jitsiMeetRoom = callHeaders.get(JITSI_MEET_ROOM_HEADER);

        // nothing to handle
        if (jitsiMeetRoom == null)
            return;

        boolean handled = false;
        for (JitsiMeetRequestListener l : requestHandlers)
        {
            l.onJoinJitsiMeetRequest(call, jitsiMeetRoom, callHeaders);
            handled = true;
        }
        if (!handled)
        {
            logger.warn(
                "Unhandled join Jitsi Meet request R:" + jitsiMeetRoom
                    + " C: " + call);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addSupportedFeature(String featureName)
    {
        throw new RuntimeException("Not implemented for SIP");
    }

    @Override
    public void removeSupportedFeature(String featureName)
    {
        throw new RuntimeException("Not implemented for SIP");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendPresenceExtension(ChatRoom chatRoom,
                                      PacketExtension extension)
    {
        throw new RuntimeException("Not implemented for SIP");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removePresenceExtension(ChatRoom chatRoom,
                                        PacketExtension extension)
    {
        throw new RuntimeException("Not implemented for SIP");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setPresenceStatus(ChatRoom chatRoom, String statusMessage)
    {
        throw new RuntimeException("Not implemented for SIP");
    }
}
