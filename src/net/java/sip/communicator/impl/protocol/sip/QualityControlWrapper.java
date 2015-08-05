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

import net.java.sip.communicator.service.protocol.media.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.neomedia.*;
import org.jitsi.service.protocol.*;

/**
 * A wrapper of media quality control.
 * @author Damian Minkov
 */
public class QualityControlWrapper
    extends AbstractQualityControlWrapper<CallPeerSipImpl>
{
    /**
     * Our class logger.
     */
    private static final Logger logger
        = Logger.getLogger(QualityControlWrapper.class);

    /**
     * Creates quality control for peer.
     * @param peer peer
     */
    QualityControlWrapper(CallPeerSipImpl peer)
    {
        super(peer);
    }

    /**
     * Changes the current video settings for the peer with the desired
     * quality settings and inform the peer to stream the video
     * with those settings.
     *
     * @param preset the desired video settings
     * @throws OperationFailedException
     */
    @Override
    public void setPreferredRemoteSendMaxPreset(QualityPreset preset)
        throws OperationFailedException
    {
        QualityControl qControls = getMediaQualityControl();

        if(qControls != null)
        {
            qControls.setRemoteSendMaxPreset(preset);

            try
            {
                // re-invites the peer with the new settings
                peer.sendReInvite();
            }
            catch (Throwable cause)
            {
                String message
                    = "Failed to re-invite for video quality change.";

                logger.error(message, cause);

                throw new OperationFailedException(
                        message,
                        OperationFailedException.INTERNAL_ERROR,
                        cause);
            }
        }
    }
}
