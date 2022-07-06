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
import org.json.simple.*;

import java.util.*;
import java.util.concurrent.*;

/**
 * Jabber protocol provider implementation of {@link OperationSetJitsiMeetTools}
 *
 * @author Pawel Domas
 * @author Cristian Florin Ghita
 */
public class OperationSetJitsiMeetToolsJabberImpl
    implements OperationSetJitsiMeetToolsJabber
{
    private final ProtocolProviderServiceJabberImpl parentProvider;

    private final static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(OperationSetJitsiMeetToolsJabberImpl.class);

    /**
     * The list of {@link JitsiMeetRequestListener}.
     */
    private final List<JitsiMeetRequestListener> requestHandlers
        = new CopyOnWriteArrayList<JitsiMeetRequestListener>();

    /**
     * Creates new instance of <tt>OperationSetJitsiMeetToolsJabberImpl</tt>.
     *
     * @param parentProvider parent Jabber protocol provider service instance.
     */
    public OperationSetJitsiMeetToolsJabberImpl(
            ProtocolProviderServiceJabberImpl parentProvider)
    {
        this.parentProvider = parentProvider;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addSupportedFeature(String featureName)
    {
        parentProvider.addSupportedFeature(featureName);
    }

    /**
     * {@inheritDoc}
     */
    public void removeSupportedFeature(String featureName)
    {
        parentProvider.getDiscoveryManager().removeFeature(featureName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendPresenceExtension(ChatRoom chatRoom,
                                      ExtensionElement extension)
    {
        ((ChatRoomJabberImpl)chatRoom).sendPresenceExtension(extension);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removePresenceExtension(ChatRoom chatRoom,
                                        ExtensionElement extension)
    {
        ((ChatRoomJabberImpl)chatRoom).removePresenceExtension(extension);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setPresenceStatus(ChatRoom chatRoom, String statusMessage)
    {
        ((ChatRoomJabberImpl)chatRoom).publishPresenceStatus(statusMessage);
    }

    @Override
    public void addRequestListener(JitsiMeetRequestListener requestHandler)
    {
        this.requestHandlers.add(requestHandler);
    }

    @Override
    public void removeRequestListener(JitsiMeetRequestListener requestHandler)
    {
        this.requestHandlers.remove(requestHandler);
    }

    /**
     * Event is fired after startmuted extension is received.
     *
     * @param startMuted startMutedFlags[0] represents
     * the muted status of audio stream.
     * startMuted[1] represents the muted status of video stream.
     */
    public void notifySessionStartMuted(boolean[] startMuted)
    {
        boolean handled = false;
        for (JitsiMeetRequestListener l : requestHandlers)
        {
            l.onSessionStartMuted(startMuted);
            handled = true;
        }

        if (!handled)
        {
            logger.warn(
                "Unhandled join onStartMuted Jitsi Meet request!");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendJSON(CallPeer callPeer,
                        JSONObject jsonObject,
                        Map<String, Object> params)
                        throws OperationFailedException
    {
        throw new OperationFailedException("Operation not supported for this protocol yet!",
                                            OperationFailedException.NOT_SUPPORTED_OPERATION);
    }
}
