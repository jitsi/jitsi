/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.media.*;
import net.java.sip.communicator.util.*;

/**
 * A wrapper of media quality control.
 *
 * @author Damian Minkov
 * @author Sebastien Vincent
 */
public class QualityControlWrapper
    extends AbstractQualityControlWrapper<CallPeerJabberImpl>
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
    QualityControlWrapper(CallPeerJabberImpl peer)
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
                peer.sendModifyVideoResolutionContent();
            }
            catch (Throwable ex)
            {
                ProtocolProviderServiceJabberImpl.throwOperationFailedException(
                    "Failed to re-invite for video quality change.",
                    OperationFailedException.INTERNAL_ERROR, ex, logger);
            }
        }
    }
}