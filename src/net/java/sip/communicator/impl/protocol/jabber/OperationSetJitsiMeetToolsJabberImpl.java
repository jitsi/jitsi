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
 * Jabber protocol provider implementation of {@link OperationSetJitsiMeetTools}
 *
 * @author Pawel Domas
 */
public class OperationSetJitsiMeetToolsJabberImpl
    implements OperationSetJitsiMeetTools
{
    private final ProtocolProviderServiceJabberImpl parentProvider;

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
        parentProvider.getDiscoveryManager().addFeature(featureName);
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
                                      PacketExtension extension)
    {
        ((ChatRoomJabberImpl)chatRoom).sendPresenceExtension(extension);
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
        // Not used
    }

    @Override
    public void removeRequestListener(JitsiMeetRequestListener requestHandler)
    {
        // Not used
    }
}
