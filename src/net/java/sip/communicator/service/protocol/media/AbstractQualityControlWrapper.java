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
package net.java.sip.communicator.service.protocol.media;

import org.jitsi.service.neomedia.*;
import org.jitsi.service.protocol.*;

/**
 * A wrapper of media quality control.
 *
 * @param <T> <tt>MediaAwareCallPeer</tt>
 * @author Damian Minkov
 * @author Sebastien Vincent
 */
public abstract class AbstractQualityControlWrapper<
    T extends MediaAwareCallPeer<?, ?, ?>>
    implements QualityControl
{
    /**
     * The peer we are controlling.
     */
    protected final T peer;

    /**
     * The media quality control.
     */
    private QualityControl qualityControl;

    /**
     * The currently used video quality preset.
     */
    protected QualityPreset remoteSendMaxPreset = null;

    /**
     * The frame rate.
     */
    private float maxFrameRate = -1;

    /**
     * Creates quality control for peer.
     * @param peer
     */
    protected AbstractQualityControlWrapper(T peer)
    {
        this.peer = peer;
    }

    /**
     * Checks and obtains quality control from media stream.
     * @return
     */
    protected QualityControl getMediaQualityControl()
    {
        if(qualityControl != null)
            return qualityControl;

        MediaStream stream = peer.getMediaHandler().getStream(MediaType.VIDEO);

        if(stream instanceof VideoMediaStream)
            qualityControl = ((VideoMediaStream)stream).getQualityControl();

        return qualityControl;
    }

    /**
     * The currently used quality preset announced as receive by remote party.
     * @return the current quality preset.
     */
    public QualityPreset getRemoteReceivePreset()
    {
        QualityControl qc = getMediaQualityControl();

        return (qc == null) ? null : qc.getRemoteReceivePreset();
    }

    /**
     * The minimum preset that the remote party is sending and we are receiving.
     * Not Used.
     * @return the minimum remote preset.
     */
    public QualityPreset getRemoteSendMinPreset()
    {
        QualityControl qc = getMediaQualityControl();

        return (qc == null) ? null : qc.getRemoteSendMinPreset();
    }

    /**
     * The maximum preset that the remote party is sending and we are receiving.
     * @return the maximum preset announced from remote party as send.
     */
    public QualityPreset getRemoteSendMaxPreset()
    {
        QualityControl qControls = getMediaQualityControl();

        if(qControls == null)
            return remoteSendMaxPreset;

        QualityPreset qp = qControls.getRemoteSendMaxPreset();

        // there is info about max frame rate
        if(qp != null && maxFrameRate > 0)
            qp = new QualityPreset(qp.getResolution(), (int)maxFrameRate);

        return qp;
    }

    /**
     * Changes local value of frame rate, the one we have received from
     * remote party.
     * @param f new frame rate.
     */
    public void setMaxFrameRate(float f)
    {
        this.maxFrameRate = f;
    }

    /**
     * Changes remote send preset. This doesn't have impact of current stream.
     * But will have on next media changes.
     * With this we can try to change the resolution that the remote part
     * is sending.
     * @param preset the new preset value.
     */
    public void setRemoteSendMaxPreset(QualityPreset preset)
    {
        QualityControl qControls = getMediaQualityControl();

        if(qControls != null)
            qControls.setRemoteSendMaxPreset(preset);
        else
            remoteSendMaxPreset = preset;
    }

    /**
     * Changes the current video settings for the peer with the desired
     * quality settings and inform the peer to stream the video
     * with those settings.
     *
     * @param preset the desired video settings
     * @throws OperationFailedException
     */
    public abstract void setPreferredRemoteSendMaxPreset(QualityPreset preset)
        throws OperationFailedException;
}
